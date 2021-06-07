package pbftSimulator.message;

import net.sf.json.JSONObject;

public class CliTimeOutMsg extends Message {
	
	//消息结构
	public long t;

	public CliTimeOutMsg() {
	}

	//<CLITIMEOUT, t>: t表示request请求时间戳
	public CliTimeOutMsg(long t, int sndId, int rcvId, long rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = CLITIMEOUT;
		this.len = CLTMSGLEN;
		this.t = t;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof CliTimeOutMsg) {
        	CliTimeOutMsg msg = (CliTimeOutMsg) obj;
            return (t == msg.t);
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
        String str = "" + t;
        return str.hashCode();
    }
	
    public String toString() {
    	return super.toString() + "请求时间戳:"+t;
    }

	@Override
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("rcvId", rcvId);
		jsout.put("rcvtime", rcvtime);
		jsout.put("sndId", sndId);
		jsout.put("len", len);
		jsout.put("type", type);
		jsout.put("t", t);
		return jsout.toString();
	}

	@Override
	public CliTimeOutMsg decoder(String jsin) throws Exception {
		CliTimeOutMsg output = new CliTimeOutMsg();
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
			output.t = js.getInt("t");

		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		}
		return output;
	}
}
