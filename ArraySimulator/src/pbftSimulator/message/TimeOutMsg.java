package pbftSimulator.message;

import net.sf.json.JSONObject;

public class TimeOutMsg extends Message {
	
	public int v;				
	
	public int n;

	public TimeOutMsg() {
	}

	//消息结构
	//<TIMEOUT, v, n>:v表示视图编号;n表示序号;
	public TimeOutMsg(int v, int n, int sndId, int rcvId, long rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = TIMEOUT;
		this.len = TIMMSGLEN;
		this.v = v;
		this.n = n;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof TimeOutMsg) {
        	TimeOutMsg msg = (TimeOutMsg) obj;
            return (v == msg.v && n == msg.n);
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
        String str = "" + v + n;
        return str.hashCode();
    }
    
    public String toString() {
    	return super.toString() + "视图编号:"+v+";序号:"+n;
    }

	@Override
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("rcvId", rcvId);
		jsout.put("rcvtime", rcvtime);
		jsout.put("sndId", sndId);
		jsout.put("len", len);
		jsout.put("type", type);
		jsout.put("v", v);
		jsout.put("n", n);
		return jsout.toString();
	}

	@Override
	public TimeOutMsg decoder(String jsin) throws Exception {
		TimeOutMsg output = new TimeOutMsg();
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
			output.v = js.getInt("v");
			output.n = js.getInt("n");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		}
		return output;
	}
}
