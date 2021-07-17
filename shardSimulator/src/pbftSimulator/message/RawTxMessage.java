package pbftSimulator.message;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import shardSystem.transaction.Transaction;

public class RawTxMessage extends Message {
	public JsonArray txs;  // transactions: [JSONObject, JSONObject, ... ]
	
	public RawTxMessage(Transaction[] m) {
		super(0, 0, 0);
		this.txs = new JsonArray();
		for (int i = 0; i < m.length; i ++) {
			JsonObject innerObject = new Gson().toJsonTree(m[i]).getAsJsonObject();
			this.txs.add(innerObject);
		}
		this.type = Message.TRANSACTION;
	}

	public RawTxMessage(ArrayList<Transaction> m) {
		super(0, 0, 0);
		this.txs = new JsonArray();
		for (int i = 0; i < m.size(); i ++) {
			JsonObject innerObject = new Gson().toJsonTree(m.get(i)).getAsJsonObject();
			this.txs.add(innerObject);
		}
		this.type = Message.TRANSACTION;
	}

	public RawTxMessage() {
		this.txs = new JsonArray();
	}

	public String toString() {
		return encoder();
	}

	public String encoder() {
		String str = new Gson().toJson(this);
		return str;
	}

	public static void main(String[] args) {
		Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100.0, 0, 0, 0, 0, 0, 0, 0, 0);
		// Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100, 0);
		RawTxMessage rt = new RawTxMessage(new Transaction[] {tx1});
		System.out.println(rt.encoder());

		RawTxMessage rt2 = new Gson().fromJson(rt.encoder(), RawTxMessage.class);
		System.out.println(rt2.toString());
	}

	public JsonArray getTxs() {
		return this.txs;
	}
}
