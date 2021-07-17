package message;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class Message {
	
	public static final int TRANSACTION = 8;
	public static final int REQUEST = 0;		
	public static final int PREPREPARE = 1;
	public static final int PREPARE = 2;
	public static final int COMMIT = 3;
	public static final int REPLY = 4;
	public static final int CHECKPOINT = 5;
	public static final int VIEWCHANGE = 6;
	public static final int NEWVIEW = 7;
	
	public static final int TIMEOUT = 8;					//用来提醒节点超时的虚拟消息
	public static final int CLITIMEOUT = 9;					//用来提醒客户端超时的虚拟消息
	public static final long PRQMSGLEN = 0;					//PreRequest消息是虚拟消息
	public static final long REQMSGLEN = 100;				//Request消息的大小(bytes),可按实际情况设置
	public static final long PPRMSGLEN = 4 + REQMSGLEN;		//RrePrepare消息的大小
	public static final long PREMSGLEN = 36;				//Prepare消息的大小
	public static final long COMMSGLEN = 36;				//Commit消息的大小
	public static final long REPMSGLEN = 16;				//Reply消息的大小
	public static final long CKPMSGBASELEN = 4;				//CheckPoint消息的基础大小（还需要动态加上s集合大小）
	public static final long VCHMSGBASELEN = 4;				//ViewChange消息的基础大小
	public static final long NEVMSGBASELEN = 3;				//NewView消息的基础大小
	public static final long LASTREPLEN = REPMSGLEN - 3;	//LastReply的大小
	public static final long TIMMSGLEN = 0;					//TimeOut消息是虚拟消息
	public static final long CLTMSGLEN = 0;					//CliTimeOut消息是虚拟消息
	
	public static final int TIMEOUT_TIME = 500;
	public static Comparator<Message> cmp = new Comparator<Message>(){
		@Override
		public int compare(Message c1, Message c2) {
			return (int) (c1.rcvtime - c2.rcvtime);
		}
	};
	
	public int type;				//消息类型	
	public int sndId;				//消息发送端id
	public int rcvId;  				//消息接收端id
	public long rcvtime;  			//消息接收时间
	public long len;				//消息大小
	public String clientId;         // 发消息记录

	public Message(int sndId, int rcvId, long rcvtime) {
		this.sndId = sndId;
		this.rcvId = rcvId;
		this.rcvtime = rcvtime;
		this.clientId = String.format("%03d", sndId);
	}

	public Message() {
		this.sndId = 0;
		this.rcvId = 0;
		this.rcvtime = 0;
		this.clientId = String.format("%03d", sndId);
	}
	
	public void print(String tag, Logger logger) {
		String prefix = "【"+tag+"】";
		// System.out.println(prefix+toString());
		logger.info(prefix+toString());
	}
	
	public static long accumulateLen(Set<Message> set) {
		long len = 0L;
		if(set != null) {
			for(Message m : set) {
				len += m.len;
			}
		}
		return len;
	}
	
	public static long accumulateLen(Map<Integer, Set<Message>> map) {
		long len = 0L;
		if(map != null) {
			for(Integer n : map.keySet()) {
				len += accumulateLen(map.get(n));
			}
		}
		return len;
	}
	
	public Message copy(int rcvId, long rcvtime) {
		return new Message(sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof Message) {
        	Message msg = (Message) obj;
            return (type == msg.type && sndId == msg.sndId && rcvId == msg.rcvId && rcvtime == msg.rcvtime);
        }
        return super.equals(obj);
    }
    
    public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getSndId() {
		return sndId;
	}

	public void setSndId(int sndId) {
		this.sndId = sndId;
	}

	public int getRcvId() {
		return rcvId;
	}

	public void setRcvId(int rcvId) {
		this.rcvId = rcvId;
	}

	public long getRcvtime() {
		return rcvtime;
	}

	public void setRcvtime(long rcvtime) {
		this.rcvtime = rcvtime;
	}

	public long getLen() {
		return len;
	}

	public void setLen(long len) {
		this.len = len;
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public static int getRequest() {
		return REQUEST;
	}

	public static int getPreprepare() {
		return PREPREPARE;
	}

	public static int getPrepare() {
		return PREPARE;
	}

	public static int getCommit() {
		return COMMIT;
	}

	public static int getReply() {
		return REPLY;
	}

	public static int getCheckpoint() {
		return CHECKPOINT;
	}

	public static int getViewchange() {
		return VIEWCHANGE;
	}

	public static int getNewview() {
		return NEWVIEW;
	}

	public static int getTimeout() {
		return TIMEOUT;
	}

	public static int getClitimeout() {
		return CLITIMEOUT;
	}

	public static long getPrqmsglen() {
		return PRQMSGLEN;
	}

	public static long getReqmsglen() {
		return REQMSGLEN;
	}

	public static long getPprmsglen() {
		return PPRMSGLEN;
	}

	public static long getPremsglen() {
		return PREMSGLEN;
	}

	public static long getCommsglen() {
		return COMMSGLEN;
	}

	public static long getRepmsglen() {
		return REPMSGLEN;
	}

	public static long getCkpmsgbaselen() {
		return CKPMSGBASELEN;
	}

	public static long getVchmsgbaselen() {
		return VCHMSGBASELEN;
	}

	public static long getNevmsgbaselen() {
		return NEVMSGBASELEN;
	}

	public static long getLastreplen() {
		return LASTREPLEN;
	}

	public static long getTimmsglen() {
		return TIMMSGLEN;
	}

	public static long getCltmsglen() {
		return CLTMSGLEN;
	}

	public static Comparator<Message> getCmp() {
		return cmp;
	}

	public int hashCode() {
        String str = "" + type + sndId + rcvId + rcvtime;
        return str.hashCode();
    }

	public boolean ifTimeOut(long time) {
		if (Math.abs(rcvtime-time) >= TIMEOUT_TIME) 
			return true;
		return false;
	}
    
    public String toString() {
		String[] typeName = {"Request","PrePrepare","Prepare","Commit","Reply"
				,"CheckPoint","ViewChange","NewView","TimeOut","CliTimeOut"};
		return "消息类型:"+typeName[type]+";发送者id:"
				+sndId+";接收者id:"+rcvId+";消息接收时间戳:"+rcvtime+";";
    }

	/**
	 * 对消息进行编码，用于网络通信
	 * @return 编码的字符串，格式采用JSON
	 */
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("rcvId", rcvId);
		jsout.put("rcvtime", rcvtime);
		jsout.put("sndId", sndId);
		jsout.put("len", len);
		jsout.put("type", type);
		return jsout.toString();
	}

	public Message decoder(String jsin) throws Exception {
		Message output = new Message(0,0,0);
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		} 
		return output;
	}

}