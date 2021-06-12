package pbftSimulator.message;

import shardSystem.transaction.Transaction;

public class RawTxMessage extends Message {
	private Transaction tx;
	public RawTxMessage(Transaction tx) {
		super(0, 0, 0);
		this.tx = tx;
		this.type = REQUEST;
		this.len = REQMSGLEN;
		// TODO Auto-generated constructor stub
	}

	public RawTxMessage(String jsbuff) {
		
	}

	public RawTxMessage() {
		// TODO Auto-generated constructor stub
	}

	public Transaction getTx() {
		return tx;
	}

	public void setTx(Transaction tx) {
		this.tx = tx;
	}

}
