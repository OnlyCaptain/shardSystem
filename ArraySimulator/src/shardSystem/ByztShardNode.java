package shardSystem;


import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import pbftSimulator.message.Message;
import pbftSimulator.message.PrePrepareMsg;
import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;


public class ByztShardNode extends Replica {
	
	public static final int SHARDNUM = 1;    // 分片的数量
	public static final int SLICENUM = 2;    // 地址倒数几位，作为识别分片的依据

	public static String NAME = "ByztShardNode_";

	public String shardID;    // 节点所属分片 ID
	public String name;
	public String url;    // 数据库 url
	public Map<String, String> addrShard;
	public Queue<Transaction> txPending;



	public ByztShardNode(int id, String IP, int port, int[] netDlys, int[] netDlyToClis, String[] IPs, int[] ports, String[] cIPs, int[] cports) {
		super(NAME, id, IP, port, netDlys, netDlyToClis, IPs, ports, cIPs, cports);
		this.name = NAME.concat(String.valueOf(id));
		System.out.println(this.curWorkspace);
		shardID = "0";
	}

	public void msgProcess(Message msg) {
		msg.print(receiveTag, this.logger);
	}
}