package pbftSimulator.MQ;



public class MqSenderTest {
    public static void main(String[] args) throws Exception {

        MqSender mqSender = new MqSender();
        String msg =  "send a test message";
        mqSender.sendMessage(mqSender.session, mqSender.producer, msg);

        //关闭Sender
        try {
            if (null != mqSender.connection)
                    mqSender.connection.close();
            } catch (Throwable ignore) {
            }


    }
}
