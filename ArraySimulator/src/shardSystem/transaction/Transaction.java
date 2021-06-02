package shardSystem.transaction;

import java.util.Comparator;

import pbftSimulator.Utils;

public class Transaction {
	
	public String txId; // 待实现，标识交易的唯一性
	public String sender;
	public String recipient;
	public long timestamp;
	public double value;   
	public byte[] data;
	public String hCode;  // 哈希编码，作为判断交易是否重复的唯一标识符
	
	public long gasPrice;
	public long accountNonce;  

	public static Comparator<Transaction> cmp = new Comparator<Transaction>(){
		@Override
		public int compare(Transaction t1, Transaction t2) {
			return (int) (t1.gasPrice - t2.gasPrice);
		}
	};
	
	public Transaction(String sender, String recipient, double value, byte[] data, long timestamp, long gasPrice, long accountNonce) {
		this.sender = sender;
		this.recipient = recipient;
		this.value = value;
		if (data != null)
			this.data = data.clone();
		else this.data = data;
		this.timestamp = timestamp;

		this.gasPrice = gasPrice;
		this.accountNonce = accountNonce;

		StringBuffer content = new StringBuffer();
		content = content.append(sender)
					.append(recipient)
					.append(String.valueOf(timestamp))
					.append(String.valueOf(value))
					.append(data)
					.append(String.valueOf(gasPrice))
					.append(String.valueOf(accountNonce));
		this.hCode = Utils.getMD5Digest(content.toString());
	}

	public String getSender() { return this.sender; }

	public String getRecipient() { return this.recipient; }

	public double getValue() { return this.value; }

	public long getTimestamp() { return this.timestamp; }

	public long getGasPrice() { return this.gasPrice; }

	public long getAccountNonce() { return this.accountNonce; }

	public void setSender(String sender) { this.sender = sender; }

	public void setRecipient(String recipient) { this.recipient = recipient; }

	public void setValue(double value) { this.value = value; }

	public void setTimeStamp(long t) { this.timestamp = t; }

	public String toString() {
		return sender.concat("-").concat(recipient).concat("-").concat(String.valueOf(value));
	}
}
