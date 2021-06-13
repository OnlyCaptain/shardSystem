package pbftSimulator.MQ;

import java.util.ArrayList;

import pbftSimulator.Simulator;
import shardSystem.transaction.Transaction;

public class MqSenderTest {
    public static void main(String[] args) throws Exception {

        MqSender mqSender = new MqSender();
        // String msg =  "send a test message";
        ArrayList<Transaction> txs = Simulator.getTxsFromFile("./data/Tx_500.csv");
		int start = 0;
		
		// for(int i = 0; i < 1; i++) {
        //     ArrayList<Transaction> tx1 = new ArrayList<>(txs.subList(start, start+50));
        //     clis[rand.nextInt(CN)].sendRequest(tx1);
		// 	start += 50;
		// 	requestNums++;
		// }
        
        for (int i = 0; i < txs.size() ; i ++) {
            String msg = txs.get(i).encoder();
        	mqSender.sendMessage(mqSender.session, mqSender.producer, msg);        	
        }

        //关闭Sender
        try {
            if (null != mqSender.connection)
                    mqSender.connection.close();
            } catch (Throwable ignore) {
            }


    }
}
