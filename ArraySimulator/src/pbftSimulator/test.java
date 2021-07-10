package pbftSimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import pbftSimulator.message.Message;
import pbftSimulator.replica.Replica;
import shardSystem.ByztShardNode;
import shardSystem.OfflineShardNode;
import shardSystem.config;
import shardSystem.shardNode;
import shardSystem.transaction.Transaction;

public class test {
	
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

	public static final int SHARDNUM = 1;
	public static final int SHARDNODENUM = 7;

	public static final Level LOGLEVEL = Level.DEBUG;
	public static final int REQTXSIZE = 50;

	//消息优先队列（按消息计划被处理的时间戳排序）
	public static Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);
	//正在网络中传播的消息的总大小
	public static long inFlyMsgLen = 0;
	
	//初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
	public static int[][] netDlys = netDlyBtwRpInit(RN);
	
	public static int[][] netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);
	
	public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);

	// 节点IPs and ports
	public static String[] IPs = netIPsInit(RN*3+3);
	public static int[] ports = netPortsInit(RN*3+3);
//	public static int[] ports = {64960, 65456, 61444, 51988, 51653, 63367, 60635};

	public static String[] clientIPs = netIPsInit(CN);
//	public static int[] clientPorts = netPortsInit(CN);
	public static int[] clientPorts = {58052, 65528, 59547};
	
	public static void main(String[] args) {
		int[] usefulPorts = netPortsInit(3*7 +3);
		for (int p: usefulPorts) {
			System.out.print(p+" ");
		}
		System.out.println();
		
		Map<String, ArrayList<PairAddress>> topos = new HashMap<> ();
		// topos.put("0", new ArrayList<PairAddress>());
		String filepath = "./ArraySimulator/src/IPlists.csv";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			reader.readLine();//第一行信息，为标题信息，不用，如果需要，注释掉
			String line = null; 
			int id = 0, p_i = 0;
			int shardId = -1;
			while((line=reader.readLine())!=null){ 
				String item[] = line.split(",");
//				String IP = item[0].replace("\"", "");;//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				String IP = "127.0.0.1";
				if (id % config.SHARDNODENUM == 0) {
					id = 0;
					shardId ++;
				}
				String SID = String.valueOf(shardId);
				if (!topos.keySet().contains(SID))
					topos.put(SID, new ArrayList<PairAddress>());
//				topos.get(SID).add(new PairAddress(id++, IP, 60635));
				topos.get(SID).add(new PairAddress(id++, IP, usefulPorts[p_i++]));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
		
		System.out.println(Utils.getPublicIp());
			
		String[] shards = topos.keySet().toArray(new String[topos.size()]);

		String[] hex = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
		Map<String, ArrayList<String>> addrs = new HashMap<>();

		int n = (int)Math.ceil(16*16 / topos.size()), k = n, curS = 0;
		// 两位 -> 
		for (int i = 0; i < 16; i ++) {
			for (int j = 0; j < 16; j ++) {
				if (!addrs.keySet().contains(shards[curS]))
					addrs.put(shards[curS], new ArrayList<String>());
				addrs.get(shards[curS]).add(hex[i].concat(hex[j]));
				k --;
				if (k < 0) {
					k = n;
					curS ++;
				}
			}
		}
		System.out.println(addrs.toString());
		JSONObject js2 = JSONObject.fromObject(addrs);
		System.out.println(js2.toString());
		Map<String, String> addrShard = new HashMap<>();

		try {
			addrShard = getAddrShard("./src/config.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(addrShard.toString());


		// Replica[] reps = new Replica[RN];
		// for(int i = 0; i < RN; i++) {
		// 	reps[i] = new shardNode(i, IPs[i], ports[i], netDlys[i], netDlysToClis[i], IPs, ports, clientIPs, clientPorts);
		// }
		
		// //初始化CN个
		// PBFTSealer[] clis = new PBFTSealer[CN];
		// for(int i = 0; i < CN; i++) {
		// 	//客户端的编号设置为负数
		// 	clis[i] = new PBFTSealer(PBFTSealer.getCliId(i), clientIPs[i], clientPorts[i], netDlysToNodes[i], IPs, ports); 
		// }
		



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
	
	public static Map<String, String> getAddrShard(String fileName) throws IOException {

		File file=new File(fileName);
		String content= FileUtils.readFileToString(file,"UTF-8");

		JSONObject jsonObject = JSONObject.fromObject(content);
		JSONObject jsonTopo = jsonObject.getJSONObject("addrShard");

		Map<String, String> addrShard  = new HashMap<> ();

		for (int i = 0; i < SHARDNUM; i ++) {

			String shardID = String.valueOf(i);
			JSONArray jsonShard = jsonTopo.getJSONArray(shardID);
			for (int j = 0; j < jsonShard.size(); j ++) {
				String shardJ = jsonShard.getString(j);
				addrShard.put(shardJ, shardID);
			}
		}
		return addrShard;
	}

}