package pbftSimulator.message;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import shardSystem.transaction.Transaction;

import java.util.ArrayList;

@Data
public class TimeMsg {

	public final static String SendTag = "send";
	public final static String CommitTag = "commit";
	public long time;
	public JsonArray txs;   // transactions: [JSONObject, JSONObject, ... ]
	public String tag;

	public TimeMsg(Transaction[] txs, long time, String tag) {
		this.txs = new JsonArray();
		for (int i = 0; i < txs.length; i ++) {
			JsonObject innerObject = new Gson().toJsonTree(txs[i]).getAsJsonObject();
			this.txs.add(innerObject);
		}
		this.time = time;
		this.tag = tag;
	}

	public TimeMsg(ArrayList<Transaction> txs, long time, String tag) {
		this.txs = new JsonArray();
		for (int i = 0; i < txs.size(); i ++) {
			JsonObject innerObject = new Gson().toJsonTree(txs.get(i)).getAsJsonObject();
			this.txs.add(innerObject);
		}
		this.time = time;
		this.tag = tag;
	}

	public TimeMsg() {
		this.time = 0;
		this.tag = "";
		this.txs = new JsonArray();
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

	public JsonArray getTxs() {
		return txs;
	}

	public long getTime() {
		return time;
	}

	/**
	 * 对消息进行编码，用于网络通信
	 * @return 编码的字符串，格式采用JSON
	 */
	public String encoder() {
		String str = new Gson().toJson(this);
		return str;
	}

	public static void main(String[] args) {
		Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100.0, 0, 0, 0, 0, 0, 0, 0, 0);
		TimeMsg tdm = new TimeMsg(new Transaction[] {tx1}, 0, TimeMsg.SendTag);
		System.out.println(tdm.toString());
		TimeMsg tdm2 = new Gson().fromJson(tdm.encoder(), TimeMsg.class);
		System.out.println(tdm2.toString());
	}

}
