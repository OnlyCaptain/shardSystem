package lightNode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
//import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFuture;
import netty.NettyClientBootstrap;
import org.apache.log4j.*;
import pbftSimulator.message.Message;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.TimeMsg;
import shardSystem.config;
import shardSystem.transaction.Transaction;

import javax.jms.TextMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author zhanjzh
 *	light node in shard system.
 *  only send transactions.
 */
public class Client {

	private static final Level LOGLEVEL = Level.INFO;
	private static final int TPS_SECOND = 500;

	public String IP;
	public int port;
	
	public String priIP;
	public int priPort;
	
	public Logger logger;
	public String curWorkspace;
	public String receiveTag = "CliReceive";
	public String sendTag = "CliSend";
	public String name;
	public static int which_shard = 0;



	public Client(String IP, int port, String priIP, int priPort) {
		this.IP = IP;
		this.port = port;
		this.priIP = priIP;
		this.priPort = priPort;
		this.name = "client";
		this.curWorkspace = "./workspace/client/";
		buildWorkspace();
	}
	
	public void buildWorkspace() {
		File dir = new File(this.curWorkspace);
		if (dir.exists()) {
			// System.out.println("Dir OK");
		}
		else if (dir.mkdirs()) {
	        System.out.println("创建目录" + curWorkspace + "成功！");
        } else {
            System.out.println("创建目录" + curWorkspace + "失败！");
        }
		logger = Logger.getLogger(this.name);
		logger.removeAllAppenders(); 
		try {
			Layout layout = new PatternLayout("%-d{yyyy-MM-dd HH:mm:ss} [ %l:%r ms ] %n[%p] %m%n");
			FileAppender appender = new FileAppender(layout, this.curWorkspace.concat(this.name).concat(".log"));
			appender.setAppend(false);
			logger.setLevel(LOGLEVEL);
			logger.setAdditivity(false); 
			appender.activateOptions(); 
			logger.addAppender(appender);
			logger.info("Create log file ".concat(this.name));
			
		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}
	}

	public ArrayList<String> queryShardIDs(Transaction tx) {
		// 分片规则有三种： origin，monoxide，metis，proposed
		ArrayList<String> result = new ArrayList<>();
		String[] keys = config.topos.keySet().toArray(new String[config.SHARDNUM]);
		int s1, s2;
		switch (config.sharding_rule) {
			case "origin":
				String addr1 = tx.getSender();
				String slice1 = addr1.substring(addr1.length()-config.SLICENUM, addr1.length());
				result.add(config.addrShard.get(slice1));
				String addr2 = tx.getRecipient();
				String slice2 = addr2.substring(addr2.length()-config.SLICENUM, addr2.length());
				result.add(config.addrShard.get(slice2));
				break;
			case "monoxide":
				s1 = tx.getMonoxide_d1();
				s2 = tx.getMonoxide_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			case "lbf":
				s1 = tx.getLBF_d1();
				s2 = tx.getLBF_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			case "proposed":
				s1 = tx.getProposed_d1();
				s2 = tx.getProposed_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			default:
				System.out.println("Error!!, sharding rule is wrong");
				logger.error("sharding rule is wrong");
		}
		return result;
	}

	public Map<String, ArrayList<Transaction>> classifyTx(ArrayList<Transaction> txs) {
		// 本地调度器
        Map<String, ArrayList<Transaction>> classify = new HashMap<>();
		for (int ind = 0; ind < txs.size(); ind ++) {
			Transaction tx = txs.get(ind);
			ArrayList<String> shardIDs = queryShardIDs(tx);
			if (shardIDs.size() == 0) {
				System.out.println("Error!!, 交易没查到分片id");
				this.logger.error("Error!!, 交易没查到分片id" + tx.encoder());
			}
			String sendShard = shardIDs.get(0);
			String reciShard = shardIDs.get(1);

			ArrayList<String> shard_to_send = new ArrayList<>();
			if (sendShard.equals(reciShard)) {
				shard_to_send.add(sendShard);
			} else {
				shard_to_send.add(sendShard);
				shard_to_send.add(reciShard);
			}
			tx.setRelayFlag(2);
			for (int i = 0; i < shard_to_send.size(); i++) {
				String shard_send = shard_to_send.get(i);
				if (!classify.containsKey(shard_send)) {
					classify.put(shard_send, new ArrayList<>());
				}
				classify.get(shard_send).add(tx);
			}
		}
		return classify;
	}

	public void sendRawTx(ArrayList<Transaction> txs) {
		String[] shardLists = config.topos.keySet().toArray(new String[config.SHARDNUM]);
		Map<String, ArrayList<Transaction>> classify = classifyTx(txs);
		Iterator<String> keyIt = classify.keySet().iterator();
		while (keyIt.hasNext()) {
			String shard_send = keyIt.next();
			if (classify.get(shard_send).size() == 0) {
				System.out.println("Error size");
			}
			sendRawTx(classify.get(shard_send),shard_send);
		}
	}

	private void sendRawTx(ArrayList<Transaction> txs, String shard_send) {
		if (config.env.equals("dev")) {
			this.priIP = "127.0.0.1";
			this.priPort = config.PBFTSealer_ports.get(shard_send);
		} else {
			this.priIP = config.topos.get(shard_send).get(0).getIP();
			this.priPort = config.PBFTSEALER_PORT;
		}
		System.out.println(String.format("正在发送给分片 %s %s:%d ", shard_send, this.priIP, this.priPort));
		int message_size = 500;
		for (int start = 0; start < txs.size(); start += message_size) {
			ArrayList<Transaction> sub_tx = new ArrayList<>(txs.subList(start, Math.min(start+message_size, txs.size())));
			RawTxMessage rt = new RawTxMessage(sub_tx);
			try { //  负载均衡
				System.out.println(String.format("send to : %s:%d, txsize: %d", this.priIP, this.priPort, rt.getTxs().size()));
				sendMsg(this.priIP, this.priPort, rt, sendTag, this.logger);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static long getTimeStamp() {
		return System.currentTimeMillis();
	}

	public void sendTimer(String sIP, int sport, TimeMsg msg, Logger logger) {
		String jsbuff = msg.encoder();
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("发送失败");
		}
	}
	
	/**
	 * 发送消息
	 * @param sIP
	 * @param sport
	 * @param msg
	 * @param tag
	 * @param logger
	 */
	public void sendMsg(String sIP, int sport, Message msg, String tag, Logger logger) {
		String jsbuff = msg.encoder() + "\t";
		System.out.println(String.format("I send to %s %d", sIP, sport));
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
			ChannelFuture future = bootstrap.socketChannel.writeAndFlush(jsbuff);
			future.await();
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (Exception e) {
			this.logger.error(e);
			e.printStackTrace();
		}
	}

	public static ArrayList<Transaction> getTxsFromFile(String filepath) {
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		long uniq_id = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			String title[] = reader.readLine().split(",");    // 文件头，用来做 json 的索引
			for (int i = 0; i < title.length; i ++) title[i] = title[i].trim();

			String line = null;
			while((line=reader.readLine())!=null){
				JsonObject jstmp = new JsonObject();
				JsonObject jsin = new JsonObject();
				String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				for (int i = 0; i < item.length; i ++) {
					item[i] = item[i].trim();
					jstmp.addProperty(title[i], item[i].trim());
				}
				jsin.addProperty("txId", String.valueOf(uniq_id));
				uniq_id ++;
				jsin.addProperty("sender", jstmp.get("sender").getAsString());
				jsin.addProperty("recipient", jstmp.get("recipient").getAsString());
				jsin.addProperty("value", Double.parseDouble(jstmp.get("value").getAsString()));
//				jsin.addProperty("Broadcast", Long.parseLong(jstmp.get("Broadcast").getAsString()).intValue());
				jsin.addProperty("Broadcast", Double.valueOf(jstmp.get("Broadcast").getAsString()).intValue());
				jsin.addProperty("Monoxide_d1", Double.valueOf(jstmp.get("Monoxide_d1").getAsString()).intValue());
				jsin.addProperty("Monoxide_d2", Double.valueOf(jstmp.get("Monoxide_d2").getAsString()).intValue());
				jsin.addProperty("LBF_d1", Double.valueOf(jstmp.get("LBF_d1").getAsString()).intValue());
				jsin.addProperty("LBF_d2", Double.valueOf(jstmp.get("LBF_d2").getAsString()).intValue());
				jsin.addProperty("Proposed_d1", Double.valueOf(jstmp.get("Proposed_d1").getAsString()).intValue());
				jsin.addProperty("Proposed_d2", Double.valueOf(jstmp.get("Proposed_d2").getAsString()).intValue());
				jsin.addProperty("timestamp", System.currentTimeMillis());
				jsin.addProperty("gasPrice", 11);
				jsin.addProperty("accountNonce", 11);
				Transaction innerTx = new Gson().fromJson(jsin,Transaction.class);
				result.add(innerTx);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static void main(String[] args) throws InterruptedException {
//		config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config-dev-oneshard.json");
		config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config.json");
		System.out.println(config.Print());
		String curIP = Utils.getPublicIp();
		System.out.println("Local HostAddress "+curIP);   // ip
		System.out.println(config.topos.toString());
		String priIP = config.topos.get("0").get(0).getIP();
		int priPort;
		if (config.env.equals("prod"))
			priPort = config.PBFTSEALER_PORT;
		else
			priPort = config.PBFTSealer_ports.get("0");
		String txFilePath = args[0];
//		String txFilePath = "data/systemdata.csv";
        long start_reading = System.currentTimeMillis();
		ArrayList<Transaction> txs = Client.getTxsFromFile(txFilePath);
		long end_reading = System.currentTimeMillis();
		System.out.println(String.format("读取文件用了 %d ms", end_reading - start_reading));
		Client client = new Client(curIP, 61080, priIP, priPort);

		System.out.println(String.format("总共有 %d 条交易", txs.size()));
		if (txs.size() == 0) {
			return ;
		}
		int blocksize = config.MESSAGE_SIZE;
		int start = 0, end = txs.size();
		ArrayList<ArrayList<Transaction>> txs_tps = new ArrayList<>();
		// 构造要发送的交易队列
		while (start < end) {
			ArrayList<Transaction> sub_list = new ArrayList<>(txs.subList(start, Math.min(end, start+TPS_SECOND)));
			txs_tps.add(sub_list);
			start += TPS_SECOND;
		}

		long netWorkDelay = 0;
		long begin_time = System.currentTimeMillis();
		long waittime = 1000;   // 两次发送交易的间隔时间，默认是1000ms
		long prev_timestamp = getTimeStamp(), cur_timestamp = prev_timestamp;
		for (int i = 0; i < txs_tps.size(); i ++) {
			client.sendRawTx(txs_tps.get(i));
			while (cur_timestamp - prev_timestamp < waittime + netWorkDelay) {
				cur_timestamp = getTimeStamp();
				continue;
			}
			System.out.println(String.format("sleep: %d", cur_timestamp-prev_timestamp));
			prev_timestamp = cur_timestamp;
		}
		long end_time = System.currentTimeMillis();
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒");
		String beginT = sdf2.format(new Date(Long.parseLong(String.valueOf(begin_time))));
		String endT = sdf2.format(new Date(Long.parseLong(String.valueOf(end_time))));
		System.out.println(String.format("开始于： %s  结束于： %s ", beginT, endT));
	}
	
};
