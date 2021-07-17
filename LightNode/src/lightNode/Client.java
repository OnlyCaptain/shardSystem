package lightNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import static java.lang.Thread.sleep;

import message.TimeMsg;
import net.sf.json.JSONObject;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import message.Message;
import message.RawTxMessage;
import netty.NettyClientBootstrap;
import transaction.Transaction;

/**
 * @author zhanjzh
 *	light node in shard system.
 *  only send transactions.
 */
public class Client {

//	public static final int PBFTSEALER_PORT = 58052;  // prod
	public static final int PBFTSEALER_PORT = 62458;  // dev
	public static final int TIMEOUT = 500;					//节点超时设定(毫秒)
	public static final String COLLECTOR_IP = "127.0.0.1";
	public static final String PRI_IP = "127.0.0.1";
	public static final int COLLECTOR_PORT = 57050;
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
		// TODO Auto-generated constructor stub
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
		sendTimer(Client.COLLECTOR_IP, Client.COLLECTOR_PORT, tmsg, this.logger);
		System.out.println(tmsg.encoder());
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
				JSONObject jstmp = new JSONObject();
				JSONObject jsin = new JSONObject();
				String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				for (int i = 0; i < item.length; i ++) {
					item[i] = item[i].trim();
					jstmp.element(title[i], item[i].trim());
				}
				jsin.put("sender", jstmp.getString("sender"));
				jsin.put("recipient", jstmp.getString("recipient"));
				jsin.put("value", Double.parseDouble(jstmp.getString("value")));
				jsin.put("Monoxide_d1", Double.valueOf(jstmp.getString("Monoxide_d1")).intValue());
				jsin.put("Monoxide_d2", Double.valueOf(jstmp.getString("Monoxide_d2")).intValue());
				jsin.put("Metis_d1", Double.valueOf(jstmp.getString("Metis_d1")).intValue());
				jsin.put("Metis_d2", Double.valueOf(jstmp.getString("Metis_d2")).intValue());
				jsin.put("Proposed_d1", Double.valueOf(jstmp.getString("Proposed_d1")).intValue());
				jsin.put("Proposed_d2", Double.valueOf(jstmp.getString("Proposed_d2")).intValue());
				jsin.put("timestamp", System.currentTimeMillis());
				jsin.put("gasPrice", 0);
				jsin.put("accountNonce", 0);
				result.add(new Transaction(jsin.toString()));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void main(String[] args) throws InterruptedException {

		String curIP = Utils.getPublicIp();
		System.out.println("Local HostAddress "+curIP);   // ip
		String priIP = PRI_IP;
		int priPort = PBFTSEALER_PORT;
		String txFilePath = args[0];
		ArrayList<Transaction> txs = Client.getTxsFromFile(txFilePath);
		Client client = new Client(curIP, 61080, priIP, priPort);

		System.out.println(String.format("总共有 %d 条交易", txs.size()));
		int start = 0;
		if (txs.size() == 0) {
			return 0;
		}
		long waittime = 1000;   // 两次发送交易的间隔时间，默认是1000ms
		long prev_timestamp = getTimeStamp();
		while (start < txs.size()) {
			boolean tx_fin = false;
			for (int i=0; i <= txs.size(); i++) {
				// tx读完了
				if (i == txs.size()) {
					ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(start, txs.size()));
					client.sendRawTx(tx1);
					tx_fin = true;
					break;
				// 新的broadcast值
				} else if (txs[i].Broadcast != txs[start].Broadcast) {
					ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(start, i));
					client.sendRawTx(tx1);
					waittime = (txs[i].Broadcast - txs[start].Broadcast) * 1000;
					start = i;
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
