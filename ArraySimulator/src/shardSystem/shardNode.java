package shardSystem;

import pbftSimulator.replica.Replica;

public class shardNode extends Replica {
	public static String name = "shardNode_";

	public String shardID;    // 节点所属分片 ID
	public String IP;       // 节点的IP标识符
	public int Port;       // 节点的端口号


	public shardNode(int id, int[] netDlys, int[] netDlyToClis) {
		super(name, id, netDlys, netDlyToClis);

		// TODO Auto-generated constructor stub
	}

}
