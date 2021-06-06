package pbftSimulator.message;

import net.sf.json.JSONObject;

public class PrepareMsg extends Message {
	
	public int v;				
	
	public int n;			
	
	public String d;	
	
	public int i;

	public PrepareMsg() {

	}

	//消息结构
	//<PREPARE, v, n, d, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;i表示节点id
	public PrepareMsg(int v, int n, String d, int i, int sndId, int rcvId, long rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = PREPARE;
		this.len = PREMSGLEN;
		this.v = v;
		this.n = n;
		this.d = d;
		this.i = i;
	}
	
	public Message copy(int rcvId, long rcvtime) {
		return new PrepareMsg(v, n, new String(d), i, sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof PrepareMsg) {
        	PrepareMsg msg = (PrepareMsg) obj;
            return (v == msg.v && n == msg.n && d.equals(msg.d) && i == msg.i);
        }
        return super.equals(obj);
    }

	public int hashCode() {
        String str = "" + v + n + d + i;
        return str.hashCode();
    }
    
    public String toString() {
    	return super.toString() + "视图编号:"+v+";序列号:"+n;
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
		jsout.put("d", d);
		jsout.put("i", i);
		return jsout.toString();

	}

	@Override
	public PrepareMsg decoder(String jsin) throws Exception {
		PrepareMsg output = new PrepareMsg();
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
			output.v = js.getInt("v");
			output.n = js.getInt("n");
			output.d = js.getString("d");
			output.i = js.getInt("i");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		}
		return output;
	}
}
