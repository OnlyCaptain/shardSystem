package pbftSimulator.replica;

public class ByztReplica extends Replica{
	
	public static final String BTZTPROCESSTAG = "ByztProcess";
	
	public static final String BTZTRECEIVETAG = "ByztReceive";
	
	public static final String BTZTSENDTAG = "ByztSend";
	
	public ByztReplica(int id, int[] netDlys, int[]netDlysToClis) {
		super(id, netDlys, netDlysToClis);
		receiveTag = BTZTRECEIVETAG;
		sendTag = BTZTSENDTAG;
	}
	
	
}