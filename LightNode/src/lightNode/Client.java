package lightNode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import netty.NettyClientBootstrap;
import org.apache.log4j.*;
import pbftSimulator.message.Message;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.TimeMsg;
import shardSystem.config;
import shardSystem.transaction.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
	
	
	public void sendRawTx(ArrayList<Transaction> txs) {
		int message_size = config.MESSAGE_SIZE, start = 0;
		String[] shardLists = config.topos.keySet().toArray(new String[config.SHARDNUM]);

		for (start = 0; start < txs.size(); start += message_size) {
			ArrayList<Transaction> sub_tx = new ArrayList<>(txs.subList(start, Math.min(start+message_size, txs.size())));
			RawTxMessage rt = new RawTxMessage(sub_tx);
			try { //  负载均衡
				String sendShard = shardLists[which_shard++];
				if (config.env.equals("dev")) {
					this.priIP = "127.0.0.1";
					this.priPort = config.PBFTSealer_ports.get(sendShard);
				} else {
					this.priIP = config.topos.get(sendShard).get(0).getIP();
					this.priPort = config.PBFTSEALER_PORT;
				}
				System.out.println(String.format("send to : %s:%d", this.priIP, this.priPort));
				sendMsg(this.priIP, this.priPort, rt, sendTag, this.logger);
			} catch (Exception e) {
				e.printStackTrace();
			}
			TimeMsg tmsg = new TimeMsg(sub_tx, getTimeStamp(), TimeMsg.SendTag);
			try {
				System.out.println("正在发送记录时间的包");
				sendTimer(config.COLLECTOR_IP, config.COLLECTOR_PORT, tmsg, this.logger);
			} catch (Exception e) {
				e.printStackTrace();
			}
			which_shard = which_shard % shardLists.length;
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
		String jsbuff = msg.encoder();
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<Transaction> getTxsFromFile(String filepath) {
		ArrayList<Transaction> result = new ArrayList<Transaction>();
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
				jsin.addProperty("sender", jstmp.get("sender").getAsString());
				jsin.addProperty("recipient", jstmp.get("recipient").getAsString());
				jsin.addProperty("value", Double.parseDouble(jstmp.get("value").getAsString()));
//				jsin.addProperty("Broadcast", Long.parseLong(jstmp.get("Broadcast").getAsString()).intValue());
				jsin.addProperty("Broadcast", Double.valueOf(jstmp.get("Broadcast").getAsString()).intValue());
				jsin.addProperty("Monoxide_d1", Double.valueOf(jstmp.get("Monoxide_d1").getAsString()).intValue());
				jsin.addProperty("Monoxide_d2", Double.valueOf(jstmp.get("Monoxide_d2").getAsString()).intValue());
				jsin.addProperty("Metis_d1", Double.valueOf(jstmp.get("Metis_d1").getAsString()).intValue());
				jsin.addProperty("Metis_d2", Double.valueOf(jstmp.get("Metis_d2").getAsString()).intValue());
				jsin.addProperty("Proposed_d1", Double.valueOf(jstmp.get("Proposed_d1").getAsString()).intValue());
				jsin.addProperty("Proposed_d2", Double.valueOf(jstmp.get("Proposed_d2").getAsString()).intValue());
				jsin.addProperty("timestamp", System.currentTimeMillis());
				jsin.addProperty("gasPrice", 0);
				jsin.addProperty("accountNonce", 0);
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
		ArrayList<Transaction> txs = Client.getTxsFromFile(txFilePath);
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

		long waittime = 1000;   // 两次发送交易的间隔时间，默认是1000ms
		long prev_timestamp = getTimeStamp(), cur_timestamp = prev_timestamp;
		for (int i = 0; i < txs_tps.size(); i ++) {
			client.sendRawTx(txs_tps.get(i));
			while (cur_timestamp - prev_timestamp < waittime) {
				cur_timestamp = getTimeStamp();
				continue;
			}
			System.out.println(String.format("sleep: %d", cur_timestamp-prev_timestamp));
			prev_timestamp = cur_timestamp;
		}
	}
	
};
