package pbftSimulator.MQ;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;


public class MqListener {

    // ConnectionFactory ：连接工厂，JMS 用它创建连接
    ConnectionFactory connectionFactory;
    // Connection ：JMS 客户端到JMS Provider 的连接
    Connection connection = null;
    // Session： 一个发送或接收消息的线程
    Session session;
    // Destination ：消息的目的地;消息发送给谁.
    Destination destination;
    // 消费者，消息接收者
    MessageConsumer consumer;


//    public static void main(String[] args) {
    public MqListener(){

        connectionFactory = new ActiveMQConnectionFactory(
                ActiveMQConnection.DEFAULT_USER,
                ActiveMQConnection.DEFAULT_PASSWORD, "tcp://localhost:61616");
        try {
            // 构造从工厂得到连接对象
            connection = connectionFactory.createConnection();
            // 启动
            connection.start();
            // 获取操作连接
            session = connection.createSession(Boolean.FALSE,
                    Session.AUTO_ACKNOWLEDGE);
            // 获取session注意参数值FirstQueue是一个服务器的queue，须在在ActiveMq的console配置
            destination = session.createQueue("FirstQueue");
            consumer = session.createConsumer(destination);

        } catch (Exception e) {
            e.printStackTrace();
        }
        //关闭连接
//        finally {
//            try {
//                if (null != connection)
//                    connection.close();
//            } catch (Throwable ignore) {
//            }
//        }
    }
}
