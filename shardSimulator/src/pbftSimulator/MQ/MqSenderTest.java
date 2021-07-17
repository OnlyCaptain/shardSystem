package pbftSimulator.MQ;

import java.util.ArrayList;

import pbftSimulator.Simulator;
import shardSystem.transaction.Transaction;

public class MqSenderTest {
    public static void main(String[] args) throws Exception {

        MqSender mqSender = new MqSender("FirstQueue");
        // String msg =  "send a test message";
        ArrayList<Transaction> txs = Simulator.getTxsFromFile("./data/Tx_500.csv");
		int start = 0;
        for (int i = 0; i < txs.size() ; i ++) {
            String msg = txs.get(i).encoder();
        	mqSender.sendMessage(mqSender.session, mqSender.producer, msg);        	
        }

        try {
            if (null != mqSender.connection)
                    mqSender.connection.close();
            } catch (Throwable ignore) {
            }


    }
}
