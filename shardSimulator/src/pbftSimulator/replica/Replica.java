package pbftSimulator.replica;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.File;
import java.io.IOException;

import pbftSimulator.PBFTSealer;
import pbftSimulator.PairAddress;
import pbftSimulator.Utils;
import pbftSimulator.NettyClient.NettyClientBootstrap;
import pbftSimulator.NettyServer.ReplicaServerHandler;
import pbftSimulator.message.*;
import shardSystem.config;

public class Replica implements Runnable {
	
	public static final int K = 10;						//发送checkpoint消息的周期
	public static final int L = 30;						//L = 高水位 - 低水位		(一般取L>=K*2)
	public static final int PROCESSING = 0;		//没有收到f+1个reply
	public static final int STABLE = 1;			//已经收到了f+1个reply
	public String receiveTag = "Receive";
	public String sendTag = "Send";
	public String shardID;    // 节点所属分片 ID

	public String name;
	public int id; 										//当前节点的id

	public String IP;
	public int port;                                    // 作为服务器端口监听

	public int v;										//视图编号
	public int n;										//消息处理序列号
	public int lastRepNum;								//最新回复的消息处理序列号
	public int h;										//低水位 = 稳定状态checkpoint的n
	public boolean isTimeOut;							//当前正在处理的请求是否超时（如果超时了不会再发送任何消息）
	public Logger logger;

	public ArrayList<PairAddress> neighbors;
	public ArrayList<PairAddress> sealerIPs;
	
	//消息缓存<type, <msg>>:type消息类型;
	public Map<Integer, Set<Message>> msgCache;
	public String curWorkspace = "./";
	//最新reply的状态集合<c, <c, t, r>>:c客户端编号;t请求消息时间戳;r返回结果
	public Map<Integer, LastReply> lastReplyMap;		
	
	//checkpoints集合<n, <c, <c, t, r>>>:n消息处理序列号
	public Map<Integer, Map<Integer, LastReply>> checkPoints;
	
	public Map<Message, Integer> reqStats;			//request请求状态

	public PBFTSealer pbftSealer;
//	public Map<String, ArrayList<PairAddress>> topos;  // 这个东西，应该有如下的结构：
	/** 
	 * topos: {
	 * 	 "0": [ {ip:.., port:.., id:...}, {}, ... ]
	 * 	 "1": [ {ip:.., port:.., id:...}, {}, ... ]
	 * }
	*/
	
	public static Comparator<PrePrepareMsg> nCmp = new Comparator<PrePrepareMsg>(){
		@Override
		public int compare(PrePrepareMsg c1, PrePrepareMsg c2) {
			return (int) (c1.n - c2.n);
		}
	};
	
	public Replica(String name, String shardID, int id, String IP, int port) {
		this.name = "shard_".concat(shardID).concat("_").concat(name).concat(String.valueOf(id));
		this.shardID = shardID;
		this.id = id;
		this.IP = IP;
		this.port = port;

		msgCache = new HashMap<>();
		lastReplyMap = new HashMap<>();
		checkPoints = new HashMap<>();
		reqStats = new HashMap<>();
		checkPoints.put(0, lastReplyMap);

		neighbors = new ArrayList<PairAddress>();
		sealerIPs = new ArrayList<PairAddress>();

		ArrayList<PairAddress> IPAddrs = config.topos.get(shardID);

		for (int i = 0; i < IPAddrs.size(); i ++) {
			if (IPAddrs.get(i).getPort() == port && IPAddrs.get(i).getIP() == IP)  
				continue;
			neighbors.add(IPAddrs.get(i));
		}

		switch (config.env) {
			case "dev":
				sealerIPs.add(new PairAddress(PBFTSealer.getCliId(0), config.topos.get(this.shardID).get(0).getIP(), config.PBFTSealer_ports.get(this.shardID)));
				break;
			case "prod":
				sealerIPs.add(new PairAddress(PBFTSealer.getCliId(0), config.topos.get(this.shardID).get(0).getIP(), config.PBFTSEALER_PORT));
				break;
			case "default":
				this.logger.error("打包器地址出问题，配置环境不对");
		}
		
		// 定义当前Replica的工作目录
		curWorkspace = "./workspace/".concat(this.name).concat("/");
		buildWorkspace();

		if (isPrimary()) {
			System.out.println(String.format("分片 %s 的打包器建立在端口 %d 上", shardID, sealerIPs.get(0).getPort()));
			this.pbftSealer = new PBFTSealer(this.shardID, PBFTSealer.getCliId(0), sealerIPs.get(0).getIP(), sealerIPs.get(0).getPort());
		}
	}

	@Override
	public void run() {
		if (isPrimary()) {
			this.pbftSealer.start();
		}
		try {
			bind();
		} catch (InterruptedException e) { e.printStackTrace(); }

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
                p.addLast(new ReplicaServerHandler(Replica.this));
            }
        });
        ChannelFuture f= bootstrap.bind(port).sync();
		f.channel().closeFuture().sync();
		if(f.isSuccess()){
            System.out.println("server start---------------");
        }
    }

	/**
	 * 创建当前Replica的工作目录并定义日志文件
	 */
	public void buildWorkspace() {
		File dir = new File(this.curWorkspace);
		if (dir.exists()) {
			 System.out.println("目录已经存在，Dir OK");
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
			logger.setLevel(config.LOGLEVEL);
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
	
	public synchronized void msgProcess(Message msg) {
		msg.print(receiveTag, this.logger);
		switch(msg.type) {
			case Message.REQUEST:
				receiveRequest(msg);
				break;
			case Message.PREPREPARE:
				receivePreprepare(msg);
				break;
			case Message.PREPARE:
				receivePrepare(msg);
				break; 
			case Message.COMMIT:
				receiveCommit(msg);
				break;
			case Message.VIEWCHANGE:
				receiveViewChange(msg);
				break;
			case Message.NEWVIEW:
				receiveNewView(msg);
				break;
			case Message.TIMEOUT:
				receiveTimeOut(msg);
				break;
			case Message.CHECKPOINT:
				receiveCheckPoint(msg);
				break;
			default:
				this.logger.error("【Error】消息类型错误！");
				return;
		}
		//收集所有符合条件的prePrepare消息,并进行后续处理
		Set<Message> prePrepareMsgSet = msgCache.get(Message.PREPREPARE); 
		Queue<PrePrepareMsg> executeQ = new PriorityQueue<>(nCmp);
		if(prePrepareMsgSet == null) return; 
		for(Message m : prePrepareMsgSet) {
			PrePrepareMsg mm = (PrePrepareMsg)m;
			if(mm.v >= v && mm.n >= lastRepNum + 1) {
				sendCommit(m, msg.rcvtime);
				executeQ.add(mm);
			}
		}
		while(!executeQ.isEmpty()) {
			execute(executeQ.poll(), msg.rcvtime);
		}
		//垃圾处理
		garbageCollect();
	}
	
	public void sendCommit(Message msg, long time) {
		PrePrepareMsg mm = (PrePrepareMsg)msg;
		String d = Utils.getMD5Digest(mm.mString());
		CommitMsg cm = new CommitMsg(mm.v, mm.n, d, id, id, id, time);
		// this.logger.info("is InMsgCache: "+isInMsgCache(cm));
		if (msgCache.get(cm.type) == null)
			this.logger.debug("msgSet commit.size = 0");
		else this.logger.debug("msgSet commit.size = "+msgCache.get(cm.type).size());
		if(isInMsgCache(cm) || !prepared(mm)) {
			return;
		}
		// System.out.println(d);
		// config.sendMsgToOthers(cm, id, sendTag, this.logger);
		addMessageToCache(cm);
		sendMsgToOthers(cm, sendTag, this.logger);
	}
	
	public void execute(Message msg, long time) {
		PrePrepareMsg mm = (PrePrepareMsg)msg;
		RequestMsg rem = null;
		ReplyMsg rm = null;
		if(mm.m != null) {
			rem = (RequestMsg)(mm.m);
			rm = new ReplyMsg(mm.v, rem.t, rem.c, id, "result", id, rem.c, time);
		}
		
		if((rem == null || !isInMsgCache(rm)) && mm.n == lastRepNum + 1 && commited(mm)) {
			lastRepNum++;
			setTimer(lastRepNum+1, time);
			if(rem != null) {
				sendMsg(sealerIPs.get(PBFTSealer.getCliId(rem.c)).getIP(), sealerIPs.get(PBFTSealer.getCliId(rem.c)).getPort(), rm, sendTag, this.logger);
				LastReply llp = lastReplyMap.get(rem.c);
				if(llp == null) {
					llp = new LastReply(rem.c, rem.t, "result");
					lastReplyMap.put(rem.c, llp);
				}
				llp.t = rem.t;
				reqStats.put(rem, STABLE);
			}
			// 周期性发送checkpoint消息
			if(mm.n % K == 0) {
				Message checkptMsg = new CheckPointMsg(v, mm.n, lastReplyMap, id, id, id, time);
				 /*
				 addMessageToCache(checkptMsg);
				 config.sendMsgToOthers(checkptMsg, id, sendTag, this.logger);
				 sendMsgToOthers(checkptMsg, sendTag, this.logger);
				 */
			}
		}
	}
	
	public boolean prepared(PrePrepareMsg m) {
		Set<Message> prepareMsgSet = msgCache.get(Message.PREPARE);
		if (prepareMsgSet == null) return false;
		int cnt = 0;
		String d = Utils.getMD5Digest(m.mString());
		for(Message msg : prepareMsgSet) {
			PrepareMsg pm = (PrepareMsg)msg;
			if(pm.v == m.v && pm.n == m.n && pm.d.equals(d)) {
				cnt++;
			}
		}
		this.logger.debug("In prepare: cnt is "+cnt+" and d is "+m.mString());
		if(cnt >= 2 * Utils.getMaxTorelentNumber(config.RN)) {
			return true;
		}
		return false;
	}
	
	public boolean commited(PrePrepareMsg m) {
		Set<Message> commitMsgSet = msgCache.get(Message.COMMIT);
		if (commitMsgSet == null) return false;
		int cnt = 0;
		String d = Utils.getMD5Digest(m.mString());
		for(Message msg : commitMsgSet) {
			CommitMsg pm = (CommitMsg)msg;
			if(pm.v == m.v && pm.n == m.n && pm.d.equals(d)) {
				cnt++;
			}
		}
		this.logger.debug("In commited: cnt is "+cnt+" and d is "+m.mString());
		if(cnt > 2 * Utils.getMaxTorelentNumber(config.RN)) {
			return true;
		}
		return false;
	}
	
	public boolean viewChanged(ViewChangeMsg m) {
		Set<Message> viewChangeMsgSet = msgCache.get(Message.VIEWCHANGE);
		if (viewChangeMsgSet == null) return false;
		int cnt = 0;	
		for(Message msg : viewChangeMsgSet) {
			ViewChangeMsg vm = (ViewChangeMsg)msg;
			if(vm.v == m.v && vm.sn == m.sn) {
				cnt++;
			}
		}
		if(cnt > 2 * Utils.getMaxTorelentNumber(config.RN)) {
			return true;
		}
		return false;
	}
	
	public void garbageCollect() {
		Set<Message> checkptMsgSet = msgCache.get(Message.CHECKPOINT);
		if(checkptMsgSet == null) return;
		//找出满足f+1条件的最大的sn
		Map<Integer, Integer> snMap = new HashMap<>();
		int maxN = 0;
		for(Message msg : checkptMsgSet) {
			CheckPointMsg ckt = (CheckPointMsg)msg;
			if(!snMap.containsKey(ckt.n)) {
				snMap.put(ckt.n, 0);
			}
			int cnt = snMap.get(ckt.n)+1;
			snMap.put(ckt.n, cnt);
			if(cnt > Utils.getMaxTorelentNumber(config.RN)) {
				checkPoints.put(ckt.n, ckt.s);
				maxN = Math.max(maxN, ckt.n);
			}
		}
		//删除msgCache和checkPoints中小于n的所有数据，以及更新h值为sn
		deleteCache(maxN);
		deleteCheckPts(maxN);
		h = maxN;
	}
	
	public void receiveRequest(Message msg) {
		if(msg == null) return;
		RequestMsg reqlyMsg = (RequestMsg)msg;
		this.logger.debug(reqlyMsg.encoder());
		int c = reqlyMsg.c;
		long t = reqlyMsg.t;
		//如果这条请求已经reply过了，那么就再回复一次reply
		if(reqStats.containsKey(msg) && reqStats.get(msg) == STABLE) {
			long recTime = msg.rcvtime;
			Message replyMsg = new ReplyMsg(v, t, c, id, "result", id, c, recTime);
			return;
		}
		if(!reqStats.containsKey(msg)) {
			//把消息放进缓存
			addMessageToCache(msg);
			reqStats.put(msg, PROCESSING);
		}
		//如果是主节点
		if(isPrimary()) {
			//如果已经发送过PrePrepare消息，那就再广播一次
			Set<Message> prePrepareSet = msgCache.get(Message.PREPREPARE);
			if(prePrepareSet != null) {
				for(Message m : prePrepareSet) {
					PrePrepareMsg ppMsg = (PrePrepareMsg)m;
					if(ppMsg.v == v && ppMsg.i == id && ppMsg.m.equals(msg)) {
						m.rcvtime = msg.rcvtime;
						// config.sendMsgToOthers(m, id, sendTag, this.logger);
						sendMsgToOthers(m, sendTag, this.logger);
						return;
					}
				}
			}
			//否则如果不会超过水位就生成新的prePrepare消息并广播,同时启动timeout
			if(inWater(n + 1)) {
				n++;
				// this.logger.debug("before constructing: "+reqlyMsg.encoder());
				Message prePrepareMsg = new PrePrepareMsg(v, n, reqlyMsg, id, id, id, reqlyMsg.rcvtime);
				// this.logger.debug("after constructing: "+ prePrepareMsg.encoder());
				addMessageToCache(prePrepareMsg);
				// config.sendMsgToOthers(prePrepareMsg, id, sendTag, this.logger);
				this.logger.debug("构造的 preprepare： " + prePrepareMsg.encoder());
				sendMsgToOthers(prePrepareMsg, sendTag, logger);
			}
		}
	}
	
	public void receivePreprepare(Message msg) {
		if(isTimeOut) return;
		PrePrepareMsg prePrepareMsg = (PrePrepareMsg)msg;
		int msgv = prePrepareMsg.v;
		int msgn = prePrepareMsg.n;
		int i = prePrepareMsg.i;
		//检查消息的视图是否与节点视图相符，消息的发送者是否是主节点，
		//消息的视图是否合法，序号是否在水位内
		if(msgv < v || !inWater(msgn) || i != msgv % config.RN || !hasNewView(v)) {
			return;
		}
		//把prePrepare消息和其包含的request消息放进缓存
		receiveRequest(prePrepareMsg.m);
		addMessageToCache(msg);
		n = Math.max(n, prePrepareMsg.n);
		//生成Prepare消息并广播
		String d = Utils.getMD5Digest(prePrepareMsg.mString());
		Message prepareMsg = new PrepareMsg(msgv, msgn, d, id, id, id, msg.rcvtime);
		if(isInMsgCache(prepareMsg)) return;
		// config.sendMsgToOthers(prepareMsg, id, sendTag, this.logger);
		addMessageToCache(prepareMsg);
		sendMsgToOthers(prepareMsg, sendTag, this.logger);
	}
	
	public void receivePrepare(Message msg) {
		if(isTimeOut) return;
		PrepareMsg prepareMsg = (PrepareMsg)msg;
		int msgv = prepareMsg.v;
		int msgn = prepareMsg.n;
		//检查缓存中是否有这条消息，消息的视图是否合法，序号是否在水位内
		if(isInMsgCache(msg) || msgv < v || !inWater(msgn) || !hasNewView(v)) {
			return;
		}
		//把prepare消息放进缓存
		addMessageToCache(msg);
	}
	
	public void receiveCommit(Message msg) {
		if(isTimeOut) return;
		CommitMsg commitMsg = (CommitMsg)msg;
		int msgv = commitMsg.v;
		int msgn = commitMsg.n;
		//检查消息的视图是否合法，序号是否在水位内
		if(isInMsgCache(msg) || msgv < v || !inWater(msgn) || !hasNewView(v)) {
			return;
		}

		//把commit消息放进缓存
		addMessageToCache(msg);
	}
	
	public void receiveTimeOut(Message msg) {
		TimeOutMsg tMsg = (TimeOutMsg)msg;
		//如果消息已经进入稳态，就忽略这条消息
		if(tMsg.n <= lastRepNum || tMsg.v < v ) return;
		//如果不再会有新的request请求，则停止timeOut
		if(reqStats.size() >= config.REQNUM) return;
		isTimeOut = true;
		//发送viewChange消息
		Map<Integer, LastReply> ss = checkPoints.get(h);
		Set<Message> C = computeC();
		Map<Integer, Set<Message>> P = computeP();
		Message vm = new ViewChangeMsg(v + 1, h, ss, C, P, id, id, id, msg.rcvtime);
		addMessageToCache(vm);
		// config.sendMsgToOthers(vm, id, sendTag, this.logger);
		sendMsgToOthers(vm, sendTag, this.logger);
	}
	
	public void receiveCheckPoint(Message msg) {
		CheckPointMsg checkptMsg = (CheckPointMsg)msg;
		int msgv = checkptMsg.v;
		//检查缓存中是否有这条消息，消息的视图是否合法
		if(msgv < v ) {
			return;
		}
		//把checkpoint消息放进缓存
		addMessageToCache(msg);
	}
	
	
	public void receiveViewChange(Message msg) {
		ViewChangeMsg vcMsg = (ViewChangeMsg)msg;
		int msgv = vcMsg.v;
		int msgn = vcMsg.sn;
		//检查缓存中是否有这条消息，消息的视图是否合法
		if(msgv <= v || msgn < h) {
			return;
		}
		//把checkpoint消息放进缓存
		addMessageToCache(msg);
		//是否收到了2f+1条viewChange消息
		if(viewChanged(vcMsg)) {
			v = vcMsg.v;
			h = vcMsg.sn;
			lastRepNum = h;
			lastReplyMap = vcMsg.ss;
			n = lastRepNum;
			Map<Integer, Set<Message>> prePrepareMap = vcMsg.P;
			if(prePrepareMap != null) {
				for(Integer nn : prePrepareMap.keySet()) {
					n = Math.max(n, nn);
				}
			}
			isTimeOut = false;
			setTimer(lastRepNum + 1, msg.rcvtime);
			if(isPrimary()) {
				//发送NewView消息
				Map<String, Set<Message>> VONMap = computeVON();
				Message nvMsg = new NewViewMsg(v, VONMap.get("V"), VONMap.get("O"), VONMap.get("N"), id, id, id, msg.rcvtime);
				addMessageToCache(nvMsg);
				// config.sendMsgToOthers(nvMsg, id, sendTag, this.logger);
				sendMsgToOthers(nvMsg, sendTag, this.logger);
				//发送所有不在O内的request消息的prePrepare消息
				Set<Message> reqSet = msgCache.get(Message.REQUEST);
				if(reqSet == null) reqSet = new HashSet<>();
				Set<Message> OSet = VONMap.get("O");
				reqSet.removeAll(OSet);
				for(Message m : reqSet) {
					RequestMsg reqMsg = (RequestMsg)m;
					reqMsg.rcvtime = msg.rcvtime;
					receiveRequest(reqMsg);
				}
			}
		}
	}
	
	public void receiveNewView(Message msg) {
		NewViewMsg nvMsg = (NewViewMsg)msg;
		int msgv = nvMsg.v;
		//检查缓存中是否有这条消息，消息的视图是否合法
		if(msgv < v) {
			return;
		}
		v = msgv;
		addMessageToCache(msg);
		
		//逐一处理new view中的prePrepare消息
		Set<Message> O = nvMsg.O;
		for(Message m : O) {
			PrePrepareMsg ppMsg = (PrePrepareMsg)m;
			PrePrepareMsg newPPm = new PrePrepareMsg(v, ppMsg.n, ppMsg.m, ppMsg.i, msg.sndId, msg.rcvId, msg.rcvtime);
			receivePreprepare(newPPm);
		}
		Set<Message> N = nvMsg.N; 
		for(Message m : N) {
			PrePrepareMsg ppMsg = (PrePrepareMsg)m;
			PrePrepareMsg newPPm = new PrePrepareMsg(ppMsg.v, ppMsg.n, ppMsg.m, ppMsg.i, msg.sndId, msg.rcvId, msg.rcvtime);
			receivePreprepare(newPPm);
		}
	}
	
	public int getPriId() {
		return v % config.RN;
	}
	
	public boolean isPrimary() {
		return getPriId() == id;
	}
	
	/**
	 * 将消息存到缓存中
	 * @param m
	 */
	protected boolean isInMsgCache(Message m) {
		Set<Message> msgSet = msgCache.get(m.type);
		if(msgSet == null) {
			return false;
		}
		return msgSet.contains(m);
	}
	
	/**
	 * 将消息存到缓存中
	 * @param m
	 */
	protected void addMessageToCache(Message m) {
		Set<Message> msgSet = msgCache.get(m.type);
		if(msgSet == null) {
			msgSet = new HashSet<>();
			msgCache.put(m.type, msgSet);
		}
		msgSet.add(m);
		// msgCache.get(m.type).add(m);
	}
	
	/**
	 * 删除序号n之前的所有缓存消息
	 * @param n
	 */
	private void deleteCache(int n) {
		Map<Integer, LastReply> lastReplyMap = checkPoints.get(n);
		if(lastReplyMap == null)  return;
		for(Integer type : msgCache.keySet()) {
			Set<Message> msgSet = msgCache.get(type);
			if(msgSet != null) {
				Iterator<Message> it = msgSet.iterator();
				while(it.hasNext()) {
					Message m = it.next();
					if(m instanceof RequestMsg) {
						RequestMsg mm = (RequestMsg)m;
						if(lastReplyMap.get(mm.c) != null && mm.t <= lastReplyMap.get(mm.c).t) {
							it.remove();
						}
					}else if(m instanceof PrePrepareMsg) {
						PrePrepareMsg mm = (PrePrepareMsg)m;
						if(mm.n <= n) {
							it.remove();
						}
					}else if(m instanceof PrepareMsg) {
						PrepareMsg mm = (PrepareMsg)m;
						if(mm.n <= n) {
							it.remove();
						}
					}else if(m instanceof CommitMsg) {
						CommitMsg mm = (CommitMsg)m;
						if(mm.n <= n) {
							it.remove();
						}
					}else if(m instanceof CheckPointMsg) {
						CheckPointMsg mm = (CheckPointMsg)m;
						if(mm.n < n) {
							it.remove();
						}
					}else if(m instanceof ViewChangeMsg) {
						ViewChangeMsg mm = (ViewChangeMsg)m;
						if(mm.sn < n) {
							it.remove();
						}
					}
				}
			}
		}
	}
	
	private void deleteCheckPts(int n) {
		Iterator<Map.Entry<Integer, Map<Integer, LastReply>>> it = checkPoints.entrySet().iterator();
		while(it.hasNext()){  
			Map.Entry<Integer, Map<Integer, LastReply>> entry=it.next(); 
			int sn = entry.getKey(); 
			if(sn < n) {
				it.remove();
			}
		}
	}
	
	/**
	 * 判断一个视图编号是否有NewView的消息基础
	 * @return
	 */
	public boolean hasNewView(int v) {
		if(v == 0)
			return true;
		Set<Message> msgSet = msgCache.get(Message.NEWVIEW);
		if(msgSet != null) {
			for(Message m : msgSet) {
				NewViewMsg nMsg = (NewViewMsg)m;
				if(nMsg.v == v) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean inWater(int n) {
		return true;
		/* 这里是来限制一个Primary 不能当超过 L 个块的时长，但是目前的切换Primary还是 TODO 状态，先不限制*/
		/* return n == 0 || (n > h && n < h + L); */
	}
	
	private Set<Message> computeC(){
		if(h == 0) return null;
		Set<Message> result = new HashSet<>();
		Set<Message> checkptSet = msgCache.get(Message.CHECKPOINT);
		for(Message msg : checkptSet) {
			CheckPointMsg ckpt = (CheckPointMsg)msg;
			if(ckpt.n == h) {
				result.add(msg);
			}
		}
		return result;
	}
	
	private Map<Integer, Set<Message>> computeP(){
		Map<Integer, Set<Message>> result = new HashMap<>();
		Set<Message> prePrepareSet = msgCache.get(Message.PREPREPARE);
		if(prePrepareSet == null) return null;
		for(Message msg : prePrepareSet) {
			PrePrepareMsg ppm = (PrePrepareMsg)msg;
			if(ppm.n > h && prepared(ppm)) {
				Set<Message> set = result.get(ppm.n);
				if(set == null) {
					set = new HashSet<>();
					result.put(ppm.n, set);
				}
				set.add(msg);
			}
		}
		return result;
	}
	
	private Map<String, Set<Message>> computeVON(){
		int maxN = h;
		Set<Message> V = new HashSet<>();
		Set<Message> O = new HashSet<>();
		Set<Message> N = new HashSet<>();
		Set<Message> vcSet = msgCache.get(Message.VIEWCHANGE);
		for(Message msg : vcSet) {
			ViewChangeMsg ckpt = (ViewChangeMsg)msg;
			if(ckpt.v == v) {
				V.add(msg);
				Map<Integer, Set<Message>> ppMap = ckpt.P;
				if(ppMap == null) continue;
				for(Integer n : ppMap.keySet()) {
					Set<Message> ppSet = ppMap.get(n);
					if(ppSet == null) continue;
					for(Message m : ppSet) {
						PrePrepareMsg ppm = (PrePrepareMsg)m;
						Message ppMsg = new PrePrepareMsg(v, n, ppm.m, id, id, id, 0);
						O.add(ppMsg);
						maxN = Math.max(maxN, n);
					}
				}
			}
		}
		for(int i = h; i < maxN; i++) {
			boolean flag = false;
			for(Message msg : O) {
				PrePrepareMsg ppm = (PrePrepareMsg)msg;
				if(ppm.n == i) {
					flag = true;
					break;
				}
			}
			if(!flag) {
				Message ppMsg = new PrePrepareMsg(v, n, null, id, id, id, 0);
				N.add(ppMsg);
			}
		}
		Map<String, Set<Message>> map = new HashMap<>();
		map.put("V", V);
		map.put("O", O);
		map.put("N", N);
		n = maxN;
		return map;
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
//		 System.out.println("after encoding " + jsbuff);
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
			msg.print(tag, logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			// //通知server，即将关闭连接.(server需要从map中删除该client）
			// String clo = "";
			// bootstrap.socketChannel.writeAndFlush(clo);
//			关闭连接
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("发送失败");
		}
		

	}

	/**
	 * 发消息给其他节点
	 * @param msg
	 * @param tag
	 * @param logger
	 */
	public void sendMsgToOthers(Message msg, String tag, Logger logger) {
		// 注意，这里 neighbors 已经不包括本节点的IP跟port
		for (int i = 0; i < neighbors.size(); i ++) {
			msg.setRcvId(neighbors.get(i).getId());
			sendMsg(neighbors.get(i).getIP(), neighbors.get(i).getPort(), msg, tag, logger);
		}
	}


	public void setTimer(int n, long time) {
		Message timeOutMsg = new TimeOutMsg(v, n, id, id, time + config.TIMEOUT);
//		config.sendMsg(timeOutMsg, sendTag, this.logger);
	}



	public void sendTimer(String sIP, int sport, TimeMsg msg, Logger logger){
		String jsbuff = msg.encoder();
		try {
			NettyClientBootstrap bootstrap = new NettyClientBootstrap(sport, sIP, this.logger);
			bootstrap.socketChannel.writeAndFlush(jsbuff);
			bootstrap.eventLoopGroup.shutdownGracefully();
		} catch (InterruptedException e) {
			System.out.println("打点信息发送失败 " + e.getMessage());
			logger.error("打点信息发送失败 " + e.getMessage());
		} catch (Exception e) {
			System.out.println(String.format("链接失败 %s %d %s", sIP, sport, e.getMessage()));
		}
	}

}
