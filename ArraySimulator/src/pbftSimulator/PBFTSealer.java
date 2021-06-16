package pbftSimulator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import pbftSimulator.MQ.MqListener;
import pbftSimulator.NettyClient.NettyClientBootstrap;
import pbftSimulator.NettyServer.PBFTSealerServerHandler;
import pbftSimulator.message.CliTimeOutMsg;
import pbftSimulator.message.Message;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.RequestMsg;
import shardSystem.transaction.Transaction;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class PBFTSealer {
	
	public static final int PROCESSING = 0;		//没有收到f+1个reply
	public static final int STABLE = 1;			//已经收到了f+1个reply
	
	public String name;
	public int id;								//客户端编号
	public int v;								//视图编号
	public String IP;
	public int port;
	public String shardID;
	public Logger logger;
	public String curWorkspace;
	public Map<Long, Integer> reqStats;			//request 请求状态
	public Map<Long, Integer> reqTimes;         // request 请求次数
	public Map<Long, Message> reqMsgs;			//request消息（删除已经达到stable状态的request消息）
	public Map<Long, Map<Integer, Message>> repMsgs;	//reply消息（删除已经达到stable状态的reply消息）
	public long accTime;						//累积确认时间
	public int netDlys[];						//与各节点的基础网络时延
	public String receiveTag = "CliReceive";
	public String sendTag = "CliSend";
	public ArrayList<PairAddress> replicaAddrs;
	public long time;

	public String txPoolName;

	public Map<String, String> addrShard;
	public Map<String, ArrayList<PairAddress>> topos;  // 这个东西，应该有如下的结构：
	/** 
	 * topos: {
	 * 	 "0": [ {ip:.., port:.., id:...}, {}, ... ]
	 * 	 "1": [ {ip:.., port:.., id:...}, {}, ... ]
	 * }
	*/

	public Semaphore lastBlockEnd;

	public PBFTSealer(String shardID, int id, String IP, int port, int[] netDlys, Map<String, ArrayList<PairAddress>> topos, Map<String,String> addrShard) {
		this.id = id;
		this.netDlys = netDlys;
		this.IP = IP;
		this.port = port;
		this.shardID = shardID;
		reqStats = new HashMap<>();
		reqTimes = new HashMap<>();
		reqMsgs = new HashMap<>();
		repMsgs = new HashMap<>();
		this.addrShard = addrShard;
		// replicaAddrs = new ArrayList<PairAddress>();
		this.time = 0;
		this.lastBlockEnd = new Semaphore(1, true);

		this.topos = topos;
		this.replicaAddrs = this.topos.get(shardID);

		this.txPoolName = "txPool_".concat(this.shardID);
		// 定义当前Replica的工作目录
		this.name = "PBFTSealer_".concat(String.valueOf(id)).concat("_ofShard").concat(shardID);
		StringBuffer buf = new StringBuffer("./workspace/");
		curWorkspace = buf.append(this.name).append("/").toString();
		buildWorkspace();

		// 开启服务端
		try {
			bind();
		} catch (InterruptedException e) { e.printStackTrace(); }

		// 开启监听TxPool的线程
		Thread t = new Thread(new MyRunnable(this));
		t.start();
	}
	
	/**
	 * 创建当前Replica的工作目录并定义日志文件
	 */
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
			logger.setLevel(Simulator.LOGLEVEL);
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

	/**
     * Server开启的核心代码。
     * 其中 NettyServerHandler是 Server “接收消息”的代码。
     * @throws InterruptedException
     */
    private void bind() throws InterruptedException {
        EventLoopGroup boss=new NioEventLoopGroup();
        EventLoopGroup worker=new NioEventLoopGroup();
        ServerBootstrap bootstrap=new ServerBootstrap();
        bootstrap.group(boss,worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline p = socketChannel.pipeline();
                p.addLast(new ObjectEncoder());
                p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                p.addLast(new PBFTSealerServerHandler(PBFTSealer.this));
            }
        });
        ChannelFuture f= bootstrap.bind(this.port).sync();
		if(f.isSuccess()){
            System.out.println("Client server start---------------");
        }
    }

	public synchronized void msgProcess(Message msg) {
		msg.print(receiveTag, this.logger);
		switch(msg.type) {
			case Message.REPLY:
				receiveReply(msg);
				break;
			case Message.CLITIMEOUT:
				receiveCliTimeOut(msg);
				break;
			default:
				System.out.println("【Error】消息类型错误！");
		}
		
	}

	public void sendRawTx(ArrayList<Transaction> txs) {
		RawTxMessage rt = new RawTxMessage(txs);
		sendMsg(this.IP, this.port, rt, sendTag, this.logger);
	}
	
	public void sendRequest(ArrayList<Transaction> txs) {
		//避免时间重复
		try {
			this.lastBlockEnd.acquire();
			this.logger.info("等待上一个块完成");
		} catch (InterruptedException e) {
			this.logger.info("等待上一个块完成");
		}
		this.logger.info("正在发送消息");
		while(reqStats.containsKey(time)) {
			time++;
		}
		int priId = v % Simulator.RN;
		JSONArray txStr = new JSONArray();
		for (int i = 0; i < txs.size(); i ++) {
			txStr.add(txs.get(i));
		}
		
		Message requestMsg = new RequestMsg("Message", txStr, time, id, id, priId, time + netDlys[priId]);

		// Simulator.sendMsg(requestMsg, sendTag, this.logger);
		// this.logger.info("正在向")
		sendMsg(replicaAddrs.get(priId).getIP(), replicaAddrs.get(priId).getPort(), requestMsg, sendTag, this.logger);
		reqStats.put(time, PROCESSING);
		reqMsgs.put(time, requestMsg);
		repMsgs.put(time, new HashMap<>());
		//发送一条Timeout消息，以便将来检查是否发生超时
		setTimer(time, time);
	}
	
	public void receiveReply(Message msg) {
		ReplyMsg repMsg = (ReplyMsg)msg;
		long t = repMsg.t;
		//如果这条消息对应的request消息不存在或者已经是stable状态，那就忽略这条消息
		if(!reqStats.containsKey(t) || reqStats.get(t) == STABLE) {
			return;
		}
		//否则就将这条reply消息包含到缓存中
		saveReplyMsg(repMsg);
		//判断是否满足f+1条件，如果满足就设定主节点编号，累加确认时间并清理缓存
		if(isStable(repMsg)) {
			v = repMsg.v;
			accTime += repMsg.rcvtime - t;
			reqStats.put(t, STABLE);
			reqMsgs.remove(t);
			repMsgs.remove(t);
			this.lastBlockEnd.release();
			this.logger.info("【Stable】客户端"+id+"在"+t
					+"时间请求的消息已经得到了f+1 = " + Utils.getMaxTorelentNumber(Simulator.RN) + " 条reply，进入稳态，共耗时"+(repMsg.rcvtime - t)+"毫秒,此时占用带宽为"+Simulator.inFlyMsgLen+"B");
		}
	}
	
	public void receiveCliTimeOut(Message msg) {
		CliTimeOutMsg cliTimeOutMsg = (CliTimeOutMsg)msg;
		long t = cliTimeOutMsg.t;
		//如果这条消息对应的request消息不存在或者已经是stable状态，那就忽略这条消息
		if(!reqStats.containsKey(t) || reqStats.get(t) == STABLE) {
			return;
		}
		//否则给所有的节点广播request消息
		for(int i = 0; i < Simulator.RN; i++) {
			Message requestMsg = new RequestMsg("Message", null, t, id, id, i, cliTimeOutMsg.rcvtime + netDlys[i]);
			sendMsg(replicaAddrs.get(i).getIP(), replicaAddrs.get(i).getPort(), requestMsg, sendTag, this.logger);
			// Simulator.sendMsg(requestMsg, sendTag, this.logger);
		}
		//发送一条Timeout消息，以便将来检查是否发生超时
		setTimer(t, cliTimeOutMsg.rcvtime);
	}
	
	/**
     * 去重缓存reply消息
     * @param msg reply消息
     */
	public void saveReplyMsg(ReplyMsg msg) {
		Map<Integer, Message> rMap = repMsgs.get(msg.t);
		for(Integer i : rMap.keySet()) {
			if(i == msg.i && ((ReplyMsg)rMap.get(i)).v >= msg.v) {
				return;
			}
		}
		repMsgs.get(msg.t).put(msg.i, msg);
	}
	
	/**
	 * 判断请求消息是否已经达到稳定状态（即收到了f+1条reply消息）
	 * @param msg 请求消息
	 * @return	是否达到稳态的判断结果
	 */
	public boolean isStable(ReplyMsg msg) {
		Map<Integer, Message> rMap = repMsgs.get(msg.t);
		int cnt = 0;
		for(Integer i : rMap.keySet()) {
			if(((ReplyMsg)rMap.get(i)).v == msg.v && ((ReplyMsg)rMap.get(i)).r.equals(msg.r)) {
				cnt++;
			}
		}
		if(cnt > Utils.getMaxTorelentNumber(Simulator.RN)) return true;
		return false;
	}
	
	/**
     * 根据数组下标获取客户端Id
     * @param index 表示客户端在数组中的下标
     * @return 返回客户端id
     */
	public static int getCliId(int index) {
		return index * (-1) - 1;
	}
	
	/**
     * 根据客户端Id获取数组下标
     * @param id 表示客户端id
     * @return 返回数组下标
     */
	public static int getCliArrayIndex(int id) {
		return (id + 1) * (-1);
	}
	
	public int stableMsgNum() {
		int cnt = 0;
		if(reqStats == null) return cnt;
		for(long t : reqStats.keySet()) 
			if(reqStats.get(t) == STABLE) 
				cnt++;
		return cnt;
	}
	
	public void setTimer(long t, long time) {
		Message timeoutMsg = new CliTimeOutMsg(t, id, id, time + Simulator.CLITIMEOUT);
		// Simulator.sendMsg(timeoutMsg, "ClientTimeOut", this.logger);
		this.logger.warn("Timeout. + "+timeoutMsg.toString());
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
			msg.print(tag, logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			// 关闭连接
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发送跨分片交易的后半段
	 */
	public void sendToOtherShard(ArrayList<Transaction> txs, String targetShard) {
		RawTxMessage rt = new RawTxMessage(txs);
		this.sendMsg(this.IP, Simulator.PBFTSEALER_PORT, rt, sendTag, this.logger);
	}

	/**
	 * 根据地址查询该地址所在的分片。
	 * TODO
	 * @param addr 表示查询的地址（账户地址）
	 * @return	返回对应的分片ID
	 */
	public String queryShardID(String addr) {
		// 查询的规则有两种方式：
		String result;
		// 1. 根据尾数 mod
		String slice = addr.substring(addr.length()-Simulator.SLICENUM, addr.length());
		result = addrShard.get(slice);
		// 2. 根据地址数据库查询
		// TODO
		return result;  // 一开始只有一个分片
	}



}

class MyRunnable implements Runnable {
	public PBFTSealer pbftSealer;

	public MyRunnable(PBFTSealer pbftSealer) {
		this.pbftSealer = pbftSealer;
	}

	@Override
	public void run() {
		MqListener mqListener = new MqListener(this.pbftSealer.txPoolName);
		System.out.println("开始监听TxPool of "+this.pbftSealer.name);
		int count = 0;
        while (true) {
			long beginTime = System.currentTimeMillis();
			ArrayList<Transaction> txsBuffer = new ArrayList<>();
			// 读取够 Simulator.BLOCKTXNUM 就发送一次Request;
			try {
				int i = 0;
				long endTime = System.currentTimeMillis();
				while (i < Simulator.BLOCKTXNUM && endTime - beginTime < Simulator.BLOCK_GENERATION_TIME) {
					// 设置接收者接收消息的时间，这里设定为100s.即100s没收到新消息就会自动关闭
					TextMessage message = null;
					message = (TextMessage) mqListener.consumer.receive(Simulator.BLOCK_GENERATION_TIME - (endTime - beginTime));
					Transaction tx = null;

					if (null != message) {
						// this.pbftSealer.logger.debug("收到消息" + message.getText());
						tx = new Transaction(message.getText());
						if (null != tx) {
							txsBuffer.add(tx);
						} 
					}
					else {
						this.pbftSealer.logger.debug("超过系统设置的出块时间"+Simulator.BLOCK_GENERATION_TIME+"ms, 还没有收集到足够的交易，停止收集，出块");
						break;
					}
					i ++;
					endTime = System.currentTimeMillis();
				}
				count ++;
				this.pbftSealer.sendRequest(txsBuffer);
				System.out.println(String.format("这里是分片 %s 的第 %d 个块", this.pbftSealer.shardID, count)+"In here, txsBuffer.size = "+txsBuffer.size());
				// Thread.sleep(7000);
			} catch (JMSException e) {
				System.out.println(e.getMessage());
			} 
        }
	}
}