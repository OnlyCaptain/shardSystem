package transaction;

import java.util.Comparator;

import net.sf.json.JSONObject;
import collector.Utils;

public class Transaction {

	public Long txId; // 待实现，标识交易的唯一性
	public String sender;
	public String recipient;
	public double value;

	public long Broadcast;
	public int Monoxide_d1;
	public int Monoxide_d2;
	public int Metis_d1;
	public int Metis_d2;
	public int Proposed_d1;
	public int Proposed_d2;


	public String hCode;  // 哈希编码，作为判断交易是否重复的唯一标识符
	public byte[] data;
	public long timestamp;
	public Double gasPrice;
	public long accountNonce;

	public static Comparator<Transaction> cmp = new Comparator<Transaction>(){
		@Override
		public int compare(Transaction t1, Transaction t2) {
			return (int) (t1.gasPrice - t2.gasPrice);
		}
	};

	public Transaction() {
		this.sender = null;
		this.recipient = null;
		this.value = 0.0;
		this.data = null;
		this.timestamp = 0;
		this.gasPrice = 0.0;
		this.accountNonce = 0;

		this.Broadcast = 0;
		this.Monoxide_d1 = 0;
		this.Monoxide_d2 = 0;
		this.Metis_d1 = 0;
		this.Metis_d2 = 0;
		this.Proposed_d1 = 0;
		this.Proposed_d2 = 0;
		StringBuffer content = new StringBuffer();
		content = content.append(sender)
				.append(recipient)
				.append(String.valueOf(timestamp))
				.append(String.valueOf(value))
				.append(data)
				.append(String.valueOf(gasPrice))
				.append(String.valueOf(accountNonce))
				.append(String.valueOf(Broadcast))
				.append(String.valueOf(Monoxide_d1))
				.append(String.valueOf(Monoxide_d2))
				.append(String.valueOf(Metis_d1))
				.append(String.valueOf(Metis_d2))
				.append(String.valueOf(Proposed_d1))
				.append(String.valueOf(Proposed_d2));

		this.hCode = Utils.getMD5Digest(content.toString());
	}

	public Transaction(String jsbuff) {
		try {
			JSONObject js = JSONObject.fromObject(jsbuff);
			// txId = js.getLong("txId");
			sender = js.getString("sender");
			recipient = js.getString("recipient");
			value = js.getDouble("value");
			timestamp = js.getLong("timestamp");

			if (js.has("Broadcast")) Broadcast = js.getLong("Broadcast");
			if (js.has("Monoxide_d1")) Monoxide_d1 = js.getInt("Monoxide_d1");
			if (js.has("Monoxide_d2")) Monoxide_d2 = js.getInt("Monoxide_d2");
			if (js.has("Metis_d1")) Metis_d1 = js.getInt("Metis_d1");
			if (js.has("Metis_d2")) Metis_d2 = js.getInt("Metis_d2");
			if (js.has("Proposed_d1")) Proposed_d1 = js.getInt("Proposed_d1");
			if (js.has("Proposed_d2")) Proposed_d2 = js.getInt("Proposed_d2");

			if (!js.has("data") || js.getString("data").equals("null")) {
				data = null;
			}
			else data = js.getString("data").getBytes();
			gasPrice = js.getDouble("gasPrice");
			accountNonce = js.getLong("accountNonce");
		} catch (Exception e) {
			System.out.println("json 转换失败"+e.getMessage());
			System.out.println(jsbuff);
		}
		StringBuffer content = new StringBuffer();
		content = content.append(sender)
				.append(recipient)
				.append(String.valueOf(timestamp))
				.append(String.valueOf(value))
				.append(data)
				.append(String.valueOf(gasPrice))
				.append(String.valueOf(accountNonce))
				.append(String.valueOf(Broadcast))
				.append(String.valueOf(Monoxide_d1))
				.append(String.valueOf(Monoxide_d2))
				.append(String.valueOf(Metis_d1))
				.append(String.valueOf(Metis_d2))
				.append(String.valueOf(Proposed_d1))
				.append(String.valueOf(Proposed_d2));

		this.hCode = Utils.getMD5Digest(content.toString());
	}

	public Transaction(String sender, String recipient, double value, byte[] data,
					   long timestamp, Double gasPrice, long accountNonce,
					   int Broadcast, int Monoxide_d1, int Monoxide_d2, int Metis_d1, int Metis_d2, int Proposed_d1, int Proposed_d2) {
		this.sender = sender;
		this.recipient = recipient;
		this.value = value;
		if (data != null)
			this.data = data.clone();
		else this.data = data;
		this.timestamp = timestamp;
		this.Broadcast = Broadcast;
		this.Monoxide_d1 = Monoxide_d1;
		this.Monoxide_d2 = Monoxide_d2;
		this.Metis_d1 = Metis_d1;
		this.Metis_d2 = Metis_d2;
		this.Proposed_d1 = Proposed_d1;
		this.Proposed_d2 = Proposed_d2;
		this.gasPrice = gasPrice;
		this.accountNonce = accountNonce;

		StringBuffer content = new StringBuffer();
		content = content.append(sender)
				.append(recipient)
				.append(String.valueOf(timestamp))
				.append(String.valueOf(value))
				.append(data)
				.append(String.valueOf(gasPrice))
				.append(String.valueOf(accountNonce))
				.append(String.valueOf(Broadcast))
				.append(String.valueOf(Monoxide_d1))
				.append(String.valueOf(Monoxide_d2))
				.append(String.valueOf(Metis_d1))
				.append(String.valueOf(Metis_d2))
				.append(String.valueOf(Proposed_d1))
				.append(String.valueOf(Proposed_d2));

		this.hCode = Utils.getMD5Digest(content.toString());
	}

	public String getSender() { return this.sender; }

	public String getRecipient() { return this.recipient; }

	public double getValue() { return this.value; }

	public long getTimestamp() { return this.timestamp; }

	public Double getGasPrice() { return this.gasPrice; }

	public long getAccountNonce() { return this.accountNonce; }

	public void setSender(String sender) { this.sender = sender; }

	public void setRecipient(String recipient) { this.recipient = recipient; }

	public void setValue(double value) { this.value = value; }

	public void setTimestamp(long t) { this.timestamp = t; }

	public String toString() {
		return encoder();
	}

	public String getDigest() {
		return Utils.getMD5Digest(this.encoder());
	}

	public long getBroadcast() { return Broadcast; }

	public int getMonoxide_d1() { return Monoxide_d1; }

	public int getMonoxide_d2() { return Monoxide_d2; }

	public int getMetis_d1() { return Metis_d1; }

	public int getMetis_d2() { return Metis_d2; }

	public int getProposed_d1() { return Proposed_d1; }

	public int getProposed_d2() { return Proposed_d2; }

	/**
	 * 对消息进行编码，用于网络通信
	 * @return 编码的字符串，格式采用JSON
	 */
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("sender", sender);
		jsout.put("recipient", recipient);
		jsout.put("timestamp", 	timestamp);
		jsout.put("value", value);
		if (data == null)
			jsout.put("data", "null");
		else
			jsout.put("data", data.toString());
		jsout.put("gasPrice", gasPrice);
		jsout.put("accountNonce", accountNonce);
		jsout.put("Broadcast",Broadcast);
		jsout.put("Monoxide_d1",Monoxide_d1);
		jsout.put("Monoxide_d2",Monoxide_d2);
		jsout.put("Metis_d1",Metis_d1);
		jsout.put("Metis_d2",Metis_d2);
		jsout.put("Proposed_d1",Proposed_d1);
		jsout.put("Proposed_d2",Proposed_d2);
		return jsout.toString();
	}

}
