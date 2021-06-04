package pbftSimulator;

import java.util.HashMap;
import java.util.Map;

import pbftSimulator.NettyClient.NettyClientBootstrap;
import pbftSimulator.NettyMessage.Constants;
import pbftSimulator.message.CliTimeOutMsg;
import pbftSimulator.message.Message;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.RequestMsg;

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.File;
import java.io.IOException;

public class Client {
	
	public static final int PROCESSING = 0;		//没有收到f+1个reply
	public static final int STABLE = 1;			//已经收到了f+1个reply
	
	public String name;
	public int id;								//客户端编号
	public int v;								//视图编号
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

	public Client(int id, int[] netDlys) {
		this.id = id;
		this.netDlys = netDlys;
		reqStats = new HashMap<>();
		reqTimes = new HashMap<>();
		reqMsgs = new HashMap<>();
		repMsgs = new HashMap<>();

		// 定义当前Replica的工作目录
		this.name = "Client_".concat(String.valueOf(id));
		StringBuffer buf = new StringBuffer("./workspace/client_");
		curWorkspace = buf.append(String.valueOf(id)).append("/").toString();
		buildWorkspace();
	}
	
	// 创建当前Replica的工作目录并定义日志文件
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
		FileHandler fh;
		try {
			// This block configure the logger with handler and formatter  
			fh = new FileHandler(this.curWorkspace.concat(this.name).concat(".log"));  
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  
			logger.setUseParentHandlers(false);
			// the following statement is used to log any messages  
			logger.info("Create log file ".concat(this.name));
			
		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}
	}

	public void msgProcess(Message msg) {
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
	
	public void sendRequest(long time) {
		//避免时间重复
		while(reqStats.containsKey(time)) {
			time++;
		}
		int priId = v % Simulator.RN;
		Message requestMsg = new RequestMsg("Message", time, id, id, priId, time + netDlys[priId]);
		Simulator.sendMsg(requestMsg, sendTag, this.logger);
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
			// System.out.println("【Stable】客户端"+id+"在"+t
			// 		+"时间请求的消息已经得到了f+1条reply，进入稳态，共耗时"+(repMsg.rcvtime - t)+"毫秒,此时占用带宽为"+Simulator.inFlyMsgLen+"B");
			this.logger.info("【Stable】客户端"+id+"在"+t
					+"时间请求的消息已经得到了f+1条reply，进入稳态，共耗时"+(repMsg.rcvtime - t)+"毫秒,此时占用带宽为"+Simulator.inFlyMsgLen+"B");
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
			Message requestMsg = new RequestMsg("Message", t, id, id, i, cliTimeOutMsg.rcvtime + netDlys[i]);
			Simulator.sendMsg(requestMsg, sendTag, this.logger);
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
			if(((ReplyMsg)rMap.get(i)).v == msg.v && ((ReplyMsg)rMap.get(i)).r == msg.r) {
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
		Simulator.sendMsg(timeoutMsg, "ClientTimeOut", this.logger);
	}


	/**
	 * 使用socket发送消息
	 * @param msg 要发送的消息
	 * @param tag 消息类型
	 * @param logger 日志
	 * @param myClientId clientId，用于识别clientId，应具有唯一性
	 * @param serverPort 服务端端口
	 * @throws InterruptedException
	 */
	public void sendMsg(Message msg, String tag, Logger logger, int myClientId, int serverPort ) throws InterruptedException {
		//与服务端建立连接
		String myClientId_str = String.format("%03d",myClientId);
		Constants.setClientId(myClientId_str);
		NettyClientBootstrap bootstrap=new NettyClientBootstrap(serverPort,"localhost");

		//打印日志
		msg.print(tag, logger);

		//发送Msg
		bootstrap.socketChannel.writeAndFlush(msg);

		//通知server，即将关闭连接.(server需要从map中删除该client）
		String clo = "";
		bootstrap.socketChannel.writeAndFlush(clo);

		//关闭连接.
		bootstrap.eventLoopGroup.shutdownGracefully();

	}



}