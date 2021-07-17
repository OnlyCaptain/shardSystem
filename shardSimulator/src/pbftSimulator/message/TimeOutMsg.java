package pbftSimulator.message;

import com.google.gson.Gson;

public class TimeOutMsg extends Message {
	public int v;
	public int n;

	public TimeOutMsg() {
		super(0,0,0);
		this.type = TIMEOUT;
		this.len = TIMMSGLEN;
		this.v = 0;
		this.n = 0;
	}

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
		String str = new Gson().toJson(this);
		return str;
	}

}
