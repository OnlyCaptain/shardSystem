package shardSystem.transaction;

import pbftSimulator.Utils;

public class Transaction {
	
	public String sender;
	public String recipient;
	public long timestamp;
	public double value;
	public String data;
	public String hCode;  // 哈希编码，作为判断交易是否重复的唯一标识符
	
	public Transaction(String sender, String recipient, double value, String data, long timestamp) {
		this.sender = sender;
		this.recipient = recipient;
		this.value = value;
		this.data = data;
		this.timestamp = timestamp;

		StringBuffer content = new StringBuffer();
		content = content.append(sender).append(recipient).append(String.valueOf(value)).append(data);
		this.hCode = Utils.getMD5Digest(content.toString());
	}

	public String getSender() { return this.sender; }

	public String getRecipient() { return this.recipient; }

	public double getValue() { return this.value; }

	public long getTimestamp() { return this.timestamp; }

	public void setSender(String sender) { this.sender = sender; }

	public void setRecipient(String recipient) { this.recipient = recipient; }

	public void setValue(double value) { this.value = value; }

	public void setTimeStamp(long t) { this.timestamp = t; }

	public String toString() {
		return sender.concat("-").concat(recipient).concat("-").concat(String.valueOf(value));
	}
}
