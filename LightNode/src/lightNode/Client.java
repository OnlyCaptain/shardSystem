package lightNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import message.TimeMsg;
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
	
	public static final int REPLICA_PORT = 60635;
	public static final int PBFTSEALER_PORT = 62458;
	public static final int TIMEOUT = 500;					//节点超时设定(毫秒)
	public static final String COLLECTOR_IP = "127.0.0.1";
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
		// logger = Logger.getLogger(this.name);  
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
//		sendMsg(this.priIP, this.priPort, rt, sendTag, this.logger);
		long time = getTimeStamp();
		TimeMsg tmsg = new TimeMsg(txs, time, TimeMsg.CommitTag);
		System.out.println("正在发送记录时间的包");
		sendTimer(Client.COLLECTOR_IP, Client.COLLECTOR_PORT, tmsg, sendTag, this.logger);
		System.out.println(tmsg.encoder());
	}

	public static long getTimeStamp() {
		return System.currentTimeMillis();
	}

	public void sendTimer(String sIP, int sport, TimeMsg msg, String tag, Logger logger) {
		String jsbuff = msg.encoder();
		// System.out.println("after encoding" + jsbuff);
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
//			msg.print(tag, logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			// bootstrap.socketChannel.writeAndFlush(clo);
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
//			msg.print(tag, logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			// 关闭连接
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Transaction> getTxsFromFile(String filepath) {
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			reader.readLine();//第一行信息，为标题信息，不用，如果需要，注释掉
			String line = null; 
			while((line=reader.readLine())!=null){ 
				String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				// String last = item[item.length-1];//这就是你要的数据了
				for (int i = 0; i < item.length; i ++) {
					item[i] = item[i].trim();
				}
				result.add(new Transaction(item[0], item[1], Double.parseDouble(item[2]), null, Long.parseLong(item[3]), Double.parseDouble(item[4]), 0));
				//int value = Integer.parseInt(last);//如果是数值，可以转化为数值
				// System.out.println(last); 
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
		// String priIP = "112.74.168.78";
		String priIP = args[1];
		int priPort = PBFTSEALER_PORT;
		String txFilePath = args[0];
		ArrayList<Transaction> txs = Client.getTxsFromFile(txFilePath);
		Client client = new Client(curIP, 8080, priIP, priPort);
		int howManyTx = Integer.parseInt(args[2]);
		int times = (int)Math.ceil(howManyTx/50);
		int start = 0;
		 for(int i = 0; i < times; i++) {
		 	ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(start, Math.min(txs.size(), start+50)));
		 	client.sendRawTx(tx1);
		 	start += 50;
//		 	TimeUnit.SECONDS.sleep(5);
		 }
	}
	
};
