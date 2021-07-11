package pbftSimulator.MQ;


import javax.jms.JMSException;
import javax.jms.TextMessage;


public class MqListenerTest {
    public static void main(String[] args) throws JMSException {

        MqListener mqListener = new MqListener("txPool_0");

        while (true) {
            // 设置接收者接收消息的时间，这里设定为100s.即100s没收到新消息就会自动关闭
            TextMessage message = (TextMessage) mqListener.consumer.receive(100000);
            if (null != message) {
                System.out.println("收到消息" + message.getText());
            } else {
                break;
            }
        }

        //关闭listener
        try {
            if (null != mqListener.connection)
                mqListener.connection.close();
        } catch (Throwable ignore) {
        }

    }
}
