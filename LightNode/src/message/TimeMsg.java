package message;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import transaction.Transaction;

import java.util.ArrayList;

public class TimeMsg {

	public final static String SendTag = "send";
	public final static String CommitTag = "commit";
	public long time;
	public JSONArray txs;   // transactions: [JSONObject, JSONObject, ... ]
	public String tag;

	public TimeMsg(Transaction[] txs, long time, String tag) {
		this.txs = new JSONArray();
		for (int i = 0; i < txs.length; i ++) {
			this.txs.add(txs[i]);
		}
		this.time = time;
		this.tag = tag;
	}

	public TimeMsg(ArrayList<Transaction> txs, long time, String tag) {
		this.txs = new JSONArray();
		for (int i = 0; i < txs.size(); i ++) {
			this.txs.add(txs.get(i));
		}
		this.time = time;
		this.tag = tag;
	}

	public TimeMsg(String jsbuff) {
		try {
			JSONObject js = JSONObject.fromObject(jsbuff);
			this.txs = js.getJSONArray("txs");
			this.time = js.getLong("time");
			this.tag = js.getString("tag");
		} catch (Exception e) {
			System.out.println("json 转换失败" + e.getMessage());
		}
	}

	public TimeMsg() {
	}

	public String toString() {
		return encoder();
	}
	public boolean equals(Object obj) {
        if (obj instanceof TimeMsg) {
        	TimeMsg msg = (TimeMsg) obj;
            return (txs == msg.txs && time == msg.time);
        }
        return super.equals(obj);
    }

	public JSONArray getTxs() {
		return txs;
	}

	public long getTime() {
		return time;
	}

	public String getTag() {
		return tag;
	}

	/**
	 * 对消息进行编码，用于网络通信
	 * @return 编码的字符串，格式采用JSON
	 */
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("txs", txs);
		jsout.put("time",time);
		jsout.put("tag", tag);
		return jsout.toString();
	}

	public static void main(String[] args) {
		Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100.0, 0);
		TimeMsg tdm = new TimeMsg(new Transaction[] {tx1}, 0, TimeMsg.SendTag);
		System.out.println(tdm.toString());
		TimeMsg tdm2 = new TimeMsg(tdm.toString());
		System.out.println(tdm2.toString());
	}

}
