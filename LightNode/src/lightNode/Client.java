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

	public String IP;
	public int port;
	
	public String priIP;
	public int priPort;
	
	public Logger logger;
	public String curWorkspace;
	public String receiveTag = "CliReceive";
	public String sendTag = "CliSend";
	public String name;
	
	
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
		RawTxMessage rt = new RawTxMessage(txs);
		sendMsg(this.priIP, this.priPort, rt, sendTag, this.logger);
		long time = getTimeStamp();
		TimeMsg tmsg = new TimeMsg(txs, time, TimeMsg.SendTag);
		System.out.println("正在发送记录时间的包");
		try {
			sendTimer(config.COLLECTOR_IP, config.COLLECTOR_PORT, tmsg, this.logger);
		} catch (Exception e) {}
//		System.out.println(tmsg.encoder());
		TimeMsg test = new Gson().fromJson(tmsg.encoder(), TimeMsg.class);
//		System.out.println(test.encoder());
		System.out.println(rt.encoder());
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
		config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config-dev-oneshard.json");
//		config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config.json");
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
		int start = 0;
		if (txs.size() == 0) {
			return ;
		}
		long waittime = 1000;   // 两次发送交易的间隔时间，默认是1000ms
		long prev_timestamp = getTimeStamp();
		int blocksize = 100;
		while (start < txs.size()) {
			boolean tx_fin = false;
			for (int i=start; i <= txs.size(); i++) {
				// tx读完了
				if (i == txs.size()) {
					for (int j = start; j < i; j += blocksize) {
						ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(j, Math.min(txs.size(),j+blocksize)));
						client.sendRawTx(tx1);
						System.out.println(tx1.size());
					}
					tx_fin = true;
//					System.out.println("Broadcast is : " + txs.get(start).Broadcast + " cur time is: " + getTimeStamp());
					start = i;
					break;
					// 新的broadcast值
				} else if (txs.get(i).Broadcast != txs.get(start).Broadcast) {
					for (int j = start; j < i; j += blocksize) {
						ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(j, j+blocksize));
						client.sendRawTx(tx1);
						System.out.println(tx1.size());
					}
					waittime = (txs.get(i).Broadcast - txs.get(start).Broadcast) * 1000;
//					System.out.println("Broadcast is : " + txs.get(start).Broadcast + " cur time is: " + getTimeStamp());
					start = i;
					break;
				}
			}
			// 等待下一次发送交易
			while ( !tx_fin ) {
				long cur_timestamp = getTimeStamp();
				if (( cur_timestamp - prev_timestamp) >= waittime) {
					prev_timestamp = cur_timestamp;
					break;
				} else {
					// 考虑是否需要sleep: sleep可以减少cpu开销，不sleep可以提高精度
					//sleep(10);
				}
			}
		 }
	}
	
};
