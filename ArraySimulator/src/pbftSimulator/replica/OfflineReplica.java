package pbftSimulator.replica;

import pbftSimulator.message.Message;

public class OfflineReplica extends Replica{
	
	public static final String name = "Offline_";
	public OfflineReplica(int id, int[] netDlys, int[] netDlysToClis) {
		super(name, id, netDlys, netDlysToClis);
	}
	
	public void msgProcess(Message msg) {
		msg.print("Disconnect", this.logger);
		return;
	}
	
	
}
