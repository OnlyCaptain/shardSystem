package shardSystem.transaction;

import java.util.Comparator;

import com.google.gson.JsonParser;
import lombok.Data;
import pbftSimulator.Utils;
import com.google.gson.Gson;

@Data
public class Transaction {
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
	}

	public String toString() {
		String str = new Gson().toJson(this);
		return str;
	}

	public String encoder() {
		String str = new Gson().toJson(this);
		return str;
	}

	public String getDigest() {
		return Utils.getMD5Digest(this.toString());
	}

	public static void main(String[] args) {
		Transaction tx1 = new Transaction();
		System.out.println("编码："+tx1.toString());
		String encode = new Gson().toJsonTree(tx1).toString();
		Transaction tx2 = new Gson().fromJson(encode, Transaction.class);
		System.out.println("解码之后重新构造："+tx2.toString());
		System.out.println(tx1.getDigest());
		System.out.println(tx2.getDigest());
//		System.out.println("测试 data 注解：" + tx2.getSender());
	}
}
