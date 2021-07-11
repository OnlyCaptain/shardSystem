package message;

import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import transaction.Transaction;

public class RawTxMessage extends Message {
	public JSONArray m;   // transactions: [JSONObject, JSONObject, ... ]
	
	public RawTxMessage(Transaction[] txs) {
		super(0, 0, 0);
		// this.tx = tx;
		m = new JSONArray();
		for (int i = 0; i < txs.length; i ++) {
			m.add(txs[i]);
		}
		this.type = Message.TRANSACTION;
	}

	public RawTxMessage(ArrayList<Transaction> txs) {
		super(0, 0, 0);
		// this.tx = tx;
		m = new JSONArray();
		for (int i = 0; i < txs.size(); i ++) {
			m.add(txs.get(i));
		}
		this.type = Message.TRANSACTION;
	}

	public RawTxMessage(String jsbuff) {
		try {
			JSONObject js = JSONObject.fromObject(jsbuff);
			rcvId = js.getInt("rcvId");
			rcvtime = js.getLong("rcvtime");
			sndId = js.getInt("sndId");
			m = js.getJSONArray("m");
			len = js.getLong("len");
			type = js.getInt("type");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
		} 
	}

	// public RawTxMessage() {
	// 	this.m = null;
	// 	// TODO Auto-generated constructor stub
	// }

	public JSONArray getTxs() {
		return m;
	}

	public void setTx(JSONArray m) {
		this.m = m;
	}

	public JSONArray getM() {
		return m;
	}

	public void setM(JSONArray m) {
		this.m = m;
	}

	public String toString() {
		return encoder();
	}

	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("rcvId", rcvId);
		jsout.put("rcvtime", rcvtime);
		jsout.put("sndId", sndId);
		jsout.put("len", len);
		jsout.put("type", type);
		jsout.put("m", m);
		return jsout.toString();
	}

	public static void main(String[] args) {
		Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100.0, 0);
		// Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100, 0);
		RawTxMessage rt = new RawTxMessage(new Transaction[] {tx1});
		System.out.println(rt.toString());

		RawTxMessage rt2 = new RawTxMessage(rt.toString());
		System.out.println(rt2.toString());
	}

}
