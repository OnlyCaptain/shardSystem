package pbftSimulator.message;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import shardSystem.transaction.Transaction;

import java.util.ArrayList;

public class TimeMsg {

	public long curTime;    //构造TimeMsg的时间,在构造函数中初始化

	public JSONArray m;   // transactions: [JSONObject, JSONObject, ... ]

	//消息结构
	public TimeMsg(Transaction[] txs) {

		m = new JSONArray();
		for (int i = 0; i < txs.length; i ++) {
			m.add(txs[i]);
		}
		this.curTime = getTimeStamp();
	}

	public TimeMsg(ArrayList<Transaction> txs) {

		m = new JSONArray();
		for (int i = 0; i < txs.size(); i ++) {
			m.add(txs.get(i));
		}
		this.curTime = getTimeStamp();
	}

	public TimeMsg(String jsbuff) {
		this.curTime = getTimeStamp();
		try {
			JSONObject js = JSONObject.fromObject(jsbuff);
			m = js.getJSONArray("m");
		} catch (Exception e) {
			System.out.println("json 转换失败" + e.getMessage());
		}
	}



	public TimeMsg() {
		this.curTime = getTimeStamp();
	}
	public String toString() {
		return encoder();
	}
	public boolean equals(Object obj) {
        if (obj instanceof TimeMsg) {
        	TimeMsg msg = (TimeMsg) obj;
            return (m == msg.m && curTime == msg.curTime);
        }
        return super.equals(obj);
    }


	/**
	 * 返回系统时间戳，按秒计
	 * @return	返回系统时间戳
	 */
	public static long getTimeStamp() {
		return System.currentTimeMillis();
	}

	/**
	 * 对消息进行编码，用于网络通信
	 * @return 编码的字符串，格式采用JSON
	 */
	public String encoder() {
		JSONObject jsout = new JSONObject();
		jsout.put("m", m);
		jsout.put("curTime",curTime);
		return jsout.toString();
	}

//	public TimeMsg decoder(String jsin) throws Exception {
//		TimeMsg output = new TimeMsg();
//		try {
//			JSONObject js = JSONObject.fromObject(jsin);
//			output.m = js.getJSONArray("m");
//			output.curTime = js.getLong("curTime");
//		} catch (Exception e) {
//			System.out.println("json 转换失败"+e.getMessage());
//			return null;
//		}
//		return output;
//	}


	public static void main(String[] args) {
		Transaction tx1 = new Transaction("send", "recip", 1.11, null, 0, 100.0, 0);
		TimeMsg tdm = new TimeMsg(new Transaction[] {tx1});
		System.out.println(tdm.toString());

		TimeMsg tdm2 = new TimeMsg(tdm.toString());
		System.out.println(tdm2.toString());
	}


}
