package pbftSimulator.replica;


import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import pbftSimulator.message.Message;
import pbftSimulator.message.PrePrepareMsg;


public class ByztReplica extends Replica {
	
	public static final String BTZTPROCESSTAG = "ByztProcess";
	
	public static final String BTZTRECEIVETAG = "ByztReceive";
	
	public static final String BTZTSENDTAG = "ByztSend";

	public static final String name = "Byztnode_";
	
	public ByztReplica(int id, String IP, int port, int[] netDlys, int[]netDlysToClis) {
		super(name, id, IP, port, netDlys, netDlysToClis);
		receiveTag = BTZTRECEIVETAG;
		sendTag = BTZTSENDTAG;
	}
	
	public void msgProcess(Message msg) {
		msg.print(receiveTag, this.logger);
		// switch(msg.type) {
		// 	case Message.REQUEST:
		// 		receiveRequest(msg);
		// 		break;
		// 	case Message.PREPREPARE:
		// 		receivePreprepare(msg);
		// 		break;
		// 	case Message.PREPARE:
		// 		receivePrepare(msg);
		// 		break;
		// 	case Message.COMMIT:
		// 		receiveCommit(msg);
		// 		break;
		// 	case Message.VIEWCHANGE:
		// 		receiveViewChange(msg);
		// 		break;
		// 	case Message.NEWVIEW:
		// 		receiveNewView(msg);
		// 		break;
		// 	case Message.TIMEOUT:
		// 		receiveTimeOut(msg);
		// 		break;
		// 	case Message.CHECKPOINT:
		// 		receiveCheckPoint(msg);
		// 		break;
		// 	default:
		// 		this.logger.info("【Error】消息类型错误！");
		// 		return;
		// }
		// //收集所有符合条件的prePrepare消息,并进行后续处理
		// Set<Message> prePrepareMsgSet = msgCache.get(Message.PREPREPARE); 
		// Queue<PrePrepareMsg> executeQ = new PriorityQueue<>(nCmp);
		// if(prePrepareMsgSet == null) return; 
		// for(Message m : prePrepareMsgSet) {
		// 	PrePrepareMsg mm = (PrePrepareMsg)m;
		// 	if(mm.v >= v && mm.n >= lastRepNum + 1) {
		// 		sendCommit(m, msg.rcvtime);
		// 		executeQ.add(mm);			
		// 	}
		// }
		// while(!executeQ.isEmpty()) {
		// 	execute(executeQ.poll(), msg.rcvtime);
		// }
		// //垃圾处理
		// garbageCollect();
	}
	
	
}