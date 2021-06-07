package pbftSimulator.message;

import net.sf.json.JSONObject;

public class PrePrepareMsg extends Message {
	
	public int v;				
	
	public int n;			
	
	public Message m;
	
	public int i;

	public PrePrepareMsg() {
		v = 0;
		n = 0;
		m = new Message();
		i = 0;
	}

	//消息结构
	//<RREPREPARE, v, n, m, i>:v表示视图编号;n表示序列号;m表示request消息;i表示节点id
	public PrePrepareMsg(int v, int n, Message m, int i, int sndId, int rcvId, long rcvtime) {
		super(sndId, rcvId, rcvtime);
		this.type = PREPREPARE;
		this.len = PPRMSGLEN;
		this.v = v;
		this.n = n;
		this.m = m;
		this.i = i;
	}
	
	public Message copy(int rcvId, long rcvtime) {
		//m是浅复制，不过没有关系，不会修改它的值
		return new PrePrepareMsg(v, n, m, i, sndId, rcvId, rcvtime);
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof PrePrepareMsg) {
        	PrePrepareMsg msg = (PrePrepareMsg) obj;
            return (v == msg.v && n == msg.n && i == msg.i && ((m == null && msg.m == null) || (m != null && m.equals(msg.m))));
        }
        return super.equals(obj);
    }
        
    public int hashCode() {
        String str = "" + v + n + i;
        return str.hashCode();
    }
    
    public String toString() {
    	return super.toString() + "视图编号:"+v+";序列号:"+n;
    }
    
    public String mString() {
    	if(m == null) {
    		return "";
    	}
    	return m.toString();
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
		if (m == null)
			jsout.put("m", "null");
		else jsout.put("m", m.encoder());
		jsout.put("i", i);
		return jsout.toString();
	}

	@Override
	public PrePrepareMsg decoder(String jsin) throws Exception {
		PrePrepareMsg output = new PrePrepareMsg();
		try {
			JSONObject js = JSONObject.fromObject(jsin);
			output.rcvId = js.getInt("rcvId");
			output.rcvtime = js.getLong("rcvtime");
			output.sndId = js.getInt("sndId");
			output.len = js.getLong("len");
			output.type = js.getInt("type");
			output.v = js.getInt("v");
			output.n = js.getInt("n");

			output.m = new RequestMsg();
			String r = js.getJSONObject("m").toString();
			// System.out.println("in Preprepare"+r);
			if (r.equals("null")) {
				System.out.println("Error in preprepareMsg.decoder()");
				output.m = new RequestMsg();
			}
			else {
				output.m = output.m.decoder(r);
			} 

			output.i = js.getInt("i");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			return null;
		}
		return output;
	}
}
