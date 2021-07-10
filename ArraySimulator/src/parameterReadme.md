
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
	// public static final int SHARDNUM = 3;     // 分片个数
	public static final int SHARDNODENUM = RN;   // 每个分片的节点数量
	public static final int SLICENUM = 2;    // 地址倒数几位，作为识别分片的依据
	
	public static final Level LOGLEVEL = Level.DEBUG;
	public static final int REQTXSIZE = 50;
	
	public static final int BLOCK_GENERATION_TIME = 10000;
	public static final int REPLICA_PORT = 60635;
	public static final int PBFTSEALER_PORT = 58052;

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
public static Map<String, ArrayList<PairAddress>> topos;
	