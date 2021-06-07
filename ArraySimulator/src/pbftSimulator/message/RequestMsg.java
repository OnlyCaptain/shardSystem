package pbftSimulator.message;

import net.sf.json.JSONObject;

public class RequestMsg extends Message {
	
	public String o;			
	
	public long t;			
	
	public int c;	
	
	public String m;   // transactions: [JSONObject, JSONObject, ... ]
	
	//消息结构
	//<REQUEST, o, t, c>:o表示客户端请求的操作;t表示客户端请求时间戳;c表示客户端id
	public RequestMsg(String o, String m, long t, int c, int sndId, int rcvId, long rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = REQUEST;
		this.len = REQMSGLEN;
		this.o = o;
		this.t = t;
		this.c = c;
		this.m = m;
	}

	public RequestMsg() {
		super(0, 0, 0);
		this.type = REQUEST;
		this.len = REQMSGLEN;
		this.o = "Message";
		this.t = 0;
		this.c = 0;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof RequestMsg) {
        	RequestMsg msg = (RequestMsg) obj;
            return (o == msg.o && t == msg.t && c == msg.c);
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
        String str = o + t + c;
        return str.hashCode();
    }
    
    public String toString() {
    	return super.toString() + "时间戳:"+t+";客户端编号:"+c;
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
		jsout.put("o", o);
		jsout.put("m", m);
		jsout.put("t", t);
		jsout.put("c", c);
		return jsout.toString();
	}

	public RequestMsg decoder(String jsin) throws Exception {
		RequestMsg output = new RequestMsg();
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.o = js.getString("o");
			output.m = js.getString("m");
			output.t = js.getLong("t");
			output.c = js.getInt("c");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		} 
		return output;
	}
	
}
