package shardSystem;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import pbftSimulator.Utils;
import pbftSimulator.message.Message;
import pbftSimulator.replica.ByztReplica;
import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;

public class Test {
	
	public static final int RN = 7;  						//replicas节点的数量(rn)
	public static final int FN = 2;							//恶意节点的数量
	public static final int CN = 3;						//客户端数量
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
	//消息优先队列（按消息计划被处理的时间戳排序）
	public static Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);
	//正在网络中传播的消息的总大小
	public static long inFlyMsgLen = 0;

	//初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
	public static int[][] netDlys = netDlyBtwRpInit(RN);

	public static int[][] netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);
	
	public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);
		
	public static void main(String[] args) {
		//初始化包含FN个拜占庭意节点的RN个replicas
		boolean[] byzts = byztDistriInit(RN, FN);
		for (int i = 0; i < RN; i ++) {
			System.out.print(String.valueOf(byzts[i]).concat(" "));
		}
		System.out.println();
		// boolean[] byzts = {true, false, false, false, false, false, true};
		shardNode[] reps = new shardNode[RN];
		Transaction tx1 = new Transaction("0x111", "0x222", 123, null, 0, 1, 0);

		reps[0] = new shardNode(0, netDlys[0], netDlysToClis[0]);
		reps[0].txProcess(tx1);
		reps[0].sendMsg();
		// for(int i = 0; i < RN; i++) {
		// 	if(byzts[i]) {
		// 		reps[i] = new ByztReplica(i, netDlys[i], netDlysToClis[i]);
		// 	}else {
		// 		reps[i] = new shardNode(i, netDlys[i], netDlysToClis[i]);
		// 		reps[i].txProcess(tx1);
		// 	}
		// }






		
	}
	// public Test() {
	// 	// TODO Auto-generated constructor stub
	// }

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

}
