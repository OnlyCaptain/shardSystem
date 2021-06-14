package pbftSimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.FileUtils;
// import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import pbftSimulator.message.Message;
import pbftSimulator.replica.Replica;
import shardSystem.ByztShardNode;
import shardSystem.OfflineShardNode;
import shardSystem.shardNode;
import shardSystem.transaction.Transaction;

public class Simulator {
	
	public static final int RN = 7;  						//replicas节点的数量(rn)
	public static final int FN = 2;							//恶意节点的数量
	public static final int CN = 1;						//客户端数量
	public static final int INFLIGHT = 2000; 					//最多同时处理多少请求
	public static final int REQNUM = 1;					//请求消息总数量
	public static final int TIMEOUT = 500;					//节点超时设定(毫秒)
	public static final int CLITIMEOUT = 800;				//客户端超时设定(毫秒)
	public static final int BASEDLYBTWRP = 2;				//节点之间的基础网络时延
	public static final int DLYRNGBTWRP = 1;				//节点间的网络时延扰动范围
	public static final int BASEDLYBTWRPANDCLI = 10;		//节点与客户端之间的基础网络时延
	public static final int DLYRNGBTWRPANDCLI = 15;			//节点与客户端之间的网络时延扰动范围
	public static final int BANDWIDTH = 300000;			//节点间网络的额定带宽(bytes)(超过后时延呈指数级上升)
	public static final double FACTOR = 1.005;				//超出额定负载后的指数基数
	public static final int COLLAPSEDELAY = 10000;			//视为系统崩溃的网络时延
	public static final boolean SHOWDETAILINFO = true;		//是否显示完整的消息交互过程

	public static final int BLOCKTXNUM = 50;

	public static final int SHARDNUM = 3;     // 分片个数
	public static final int SHARDNODENUM = RN;   // 每个分片的节点数量
	public static final int SLICENUM = 2;    // 地址倒数几位，作为识别分片的依据
	public static final int PBFTSEALERPORT = 58052;

	public static final Level LOGLEVEL = Level.DEBUG;
	public static final int REQTXSIZE = 50;

	public static final int BLOCK_GENERATION_TIME = 1000000;

	//消息优先队列（按消息计划被处理的时间戳排序）
	public static Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);
	//正在网络中传播的消息的总大小
	public static long inFlyMsgLen = 0;
	
	//初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
	public static int[][] netDlys = netDlyBtwRpInit(RN);
	
	public static int[][] netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);
	
	public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);

	// 节点IPs and ports
	public static String[] IPs = netIPsInit(RN);
//	public static int[] ports = netPortsInit(RN);
	public static int[] ports = {64960, 65456, 61444, 51988, 51653, 63367, 60635};

	public static String[] clientIPs = netIPsInit(CN);
//	public static int[] clientPorts = netPortsInit(CN);
	public static int[] clientPorts = {58052, 65528, 59547};
	
	public static void main(String[] args) {
		//初始化包含FN个拜占庭意节点的RN个replicas
//		boolean[] byzts = byztDistriInit(RN, FN);
		// for (int i = 0; i < RN; i ++) {
		// 	System.out.print(String.valueOf(byzts[i]).concat(" "));
		// }
		// System.out.println("");
		// for (int i = 0; i < CN; i ++) {
		// 	System.out.print(String.valueOf(clientPorts[i]).concat(" "));
		// }
		// System.out.println();

		// boolean[] byzts = {true, false, false, false, false, false, true};

// 		int[] usefulPorts = netPortsInit(SHARDNODENUM * SHARDNUM);


		Map<String, ArrayList<PairAddress>> topos  = new HashMap<> ();
		String configJsonFileName = "./src/config.json";
		try {
			topos = getConfigJson(configJsonFileName);

			// Map<String, ArrayList<PairAddress>> topos = new HashMap<> ();

			// for (int i = 0; i < SHARDNUM; i ++) {
			// 	String shardID = String.valueOf(i);
			// 	topos.put(shardID, new ArrayList<PairAddress>());
			// 	for (int j = 0; j < SHARDNODENUM; j ++) {
			// 		topos.get(shardID).add(new PairAddress(j, "127.0.0.1", usefulPorts[i*SHARDNODENUM+j]));
			// 	}
			// }
			System.out.println(topos.toString());
			JSONObject js = JSONObject.fromObject(topos);
			System.out.println(js.toString());

			Replica[] reps = new Replica[SHARDNUM * SHARDNODENUM];
			for (int i = 0; i < SHARDNUM; i ++) {
				String shardID = String.valueOf(i);
				for (int j = 0; j < SHARDNODENUM; j ++) {
					reps[i*SHARDNODENUM + j] = new shardNode(shardID, j, topos.get(shardID).get(j).getIP(), topos.get(shardID).get(j).getPort(), netDlys[j], netDlysToClis[j], topos);
				}
			}

			Random rand = new Random(555);
			long requestNums = 0;
			ArrayList<Transaction> txs = getTxsFromFile("../data/Tx_500.csv");
			int start = 0;
			
			for(int i = 0; i < Math.min(INFLIGHT, REQNUM); i++) {
				ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(start, start+50));
				reps[0].pbftSealer.sendRequest(tx1);
				start += 50;
				requestNums++;
			}
		} catch (IOException e){
			//do nothing
			e.printStackTrace();
		}

//		if (msgQue.isEmpty()) 
//			System.out.println("Error!");
//		Message testMsg = msgQue.poll();
//		reps[0].sendMsg(reps[1].IP, reps[1].port, testMsg, "Testing", reps[0].logger);
		
//		long timestamp = 0;
//		// 消息处理
//		while(!msgQue.isEmpty()) {
//			// System.out.println("size of msgQue".concat(String.valueOf(msgQue.size())));
//			Message msg = msgQue.poll();
//			switch(msg.type) {
//				case Message.REPLY:
//				case Message.CLITIMEOUT:
//					clis[Client.getCliArrayIndex(msg.rcvId)].msgProcess(msg);
//					break;
//				default:
//					// 消息从这里发送到 primary 节点
//					reps[msg.rcvId].msgProcess(msg);
//			}
//			// 添加超时，如果这个Request超过某个时间不达到稳态，判定为共识失败。
//			if (msg.ifTimeOut(timestamp)) {
//				// System.out.println("timeout");
//				continue;
//			}
//			//如果还未达到稳定状态的request消息小于INFLIGHT，随机选择一个客户端发送请求消息
//			// if(requestNums - getStableRequestNum(clis) < INFLIGHT && requestNums < REQNUM) {
//			// 	clis[rand.nextInt(CN)].sendRequest(msg.rcvtime);
//			// 	requestNums++;
//			// }
//			inFlyMsgLen -= msg.len;
//			timestamp = msg.rcvtime;
//			if(getNetDelay(inFlyMsgLen, 0) > COLLAPSEDELAY ) {
//				System.out.println("【Error】网络消息总负载"+inFlyMsgLen
//						+"B,网络传播时延超过"+COLLAPSEDELAY/1000
//						+"秒，系统已严重拥堵，不可用！");
//				break;
//			}
//		}
//		long totalTime = 0;
//		long totalStableMsg = 0;
//		for(int i = 0; i < CN; i++) {
//			totalTime += clis[i].accTime;
//			totalStableMsg += clis[i].stableMsgNum();
//		}
//		double tps = getStableRequestNum(clis)/(double)(timestamp/1000);
//		System.out.println("【The end】消息平均确认时间为:"+totalTime/totalStableMsg
//				+"毫秒;消息吞吐量为:"+tps+"tps");
	}

	/**
	 * 返回系统时间戳，按秒计
	 * @return	返回系统时间戳
	 */
	public static long getTimeStamp() {
		return System.currentTimeMillis();
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

	/**
	 * 生成IP数组，后续需要改成从配置文件中读取
	 * @param n
	 * @return IP地址数组
	 */
	public static String[] netIPsInit(int n) {
		String[] result = new String[n];
		for (int i = 0; i < n; i ++) 
			result[i] = "127.0.0.1";
		return result;
	}

	public static int[] netPortsInit(int n) {
		Set<Integer> set = new HashSet<Integer>();
		int L=49152, H = 65535, p = 0, i = 0;
		Random rand = new Random(n);
		while (i < n) {
			p = L + rand.nextInt(H-L);
			if (set.contains(p)) 
				continue;
			if (Utils.isPortUsed(p)) 
				continue;
			set.add(p);
			i ++;
		}
		Integer[] temp = set.toArray(new Integer[] {});//关键语句
 		int[] result = new int[temp.length];
		for (int j = 0; j < temp.length; j++) {
			result[j] = temp[j].intValue();
		}
		return result;
	}
	
	/**
	 * 随机初始化replicas节点之间的基础网络传输延迟
	 * @param n 表示节点总数
	 * @return	返回节点之间的基础网络传输延迟数组
	 */
	public static int[][] netDlyBtwRpInit(int n){
		int[][] ltcs = new int[n][n];
		Random rand = new Random(999);
		for(int i = 0; i < n; ++i) 
			for(int j = 0; j < n; ++j) 
				if(i < j && ltcs[i][j] == 0) {
					ltcs[i][j] = BASEDLYBTWRP + rand.nextInt(DLYRNGBTWRP);
					ltcs[j][i] = ltcs[i][j];
				}
		return ltcs;
	}
	
	/**
     * 随机初始化客户端与各节点之间的基础网络传输延迟
     * @param n 表示节点数量
     * @param m 表示客户端数量
     * @return 返回客户端与各节点之间的基础网络传输延迟
     */
	public static int[][] netDlyBtwRpAndCliInit(int n, int m){
		int[][] ltcs = new int[n][m];
		Random rand = new Random(666);
		for(int i = 0; i < n; i++)
			for(int j = 0; j < m; j++)
				ltcs[i][j] = BASEDLYBTWRPANDCLI + rand.nextInt(DLYRNGBTWRPANDCLI);
		return ltcs;
	}
	
	/**
	 * 随机初始化replicas节点的拜占庭标签
	 * @param n	节点数量
	 * @param f	拜占庭节点数量
	 * @return	返回拜占庭标签数组（true为拜占庭节点，false为诚实节点）
	 */
	public static boolean[] byztDistriInit(int n, int f) {
		boolean[] byzt = new boolean[n];
		Random rand = new Random(111);
		while(f > 0) {
			int i = rand.nextInt(n);
			if(!byzt[i]) {
				byzt[i] = true;
				--f;
			}
		}
		return byzt;
	}
	
	public static void sendMsg(Message msg, String tag, Logger logger) {
		msg.print(tag, logger);
		msgQue.add(msg);
		inFlyMsgLen += msg.len;
	}
	
	public static void sendMsgToOthers(Message msg, int id, String tag, Logger logger) {
		for(int i = 0; i < RN; i++) {
			if(i != id) {
				Message m = msg.copy(i, msg.rcvtime + netDlys[id][i]);
				sendMsg(m, tag, logger);
			}
		}
	}
	
	public static void sendMsgToOthers(Set<Message> msgSet, int id, String tag, Logger logger) {
		if(msgSet == null) {
			return;
		}
		for(Message msg : msgSet) {
			sendMsgToOthers(msg, id, tag, logger);
		}
	}
	
	public static int getNetDelay(long inFlyMsgLen, int basedelay) {
		if(inFlyMsgLen < BANDWIDTH) {
			return basedelay;
		}else {
			return (int)Math.pow(FACTOR, inFlyMsgLen - BANDWIDTH) + basedelay;
		}
	}
	
	public static int getStableRequestNum(PBFTSealer[] clis) {
		int num = 0;
		for(int i = 0; i < clis.length; i++) {
			num += clis[i].stableMsgNum();
		}
		return num;
	}

	public static Map<String, ArrayList<PairAddress>> getConfigJson(String fileName) throws IOException {

		File file=new File(fileName);
		String content= FileUtils.readFileToString(file,"UTF-8");

		JSONObject jsonObject = JSONObject.fromObject(content);
		JSONObject jsonTopo = jsonObject.getJSONObject("topo");

		Map<String, ArrayList<PairAddress>> topos  = new HashMap<> ();

		for (int i = 0; i < SHARDNUM; i ++) {

			String shardID = String.valueOf(i);
			JSONArray jsonShard = jsonTopo.getJSONArray(shardID);
			topos.put(shardID, new ArrayList<PairAddress>());
			for (int j = 0; j < SHARDNODENUM; j ++) {
				JSONObject shardJ = jsonShard.getJSONObject(j);
				topos.get(shardID).add(new PairAddress(j, shardJ.getString("IP"), shardJ.getInt("port")));
			}
		}
		return topos;
	}

}
