package pbftSimulator.MQ;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;


public class MqSender {

    private static final int SEND_NUMBER = 5;
    // ConnectionFactory ：连接工厂，JMS 用它创建连接
    ConnectionFactory connectionFactory; // Connection ：JMS 客户端到JMS
    // Provider 的连接
    public Connection connection = null; // Session： 一个发送或接收消息的线程
    public Session session; // Destination ：消息的目的地;消息发送给谁.
    public Destination destination; // MessageProducer：消息发送者
    public MessageProducer producer; // TextMessage message;
    public String queueName;

//    public static void main(String[] args) {
    public MqSender(String queueName){
        this.queueName = queueName;

        // 构造ConnectionFactory实例对象，此处采用ActiveMq的实现jar
        connectionFactory = new ActiveMQConnectionFactory(
                ActiveMQConnection.DEFAULT_USER,
                ActiveMQConnection.DEFAULT_PASSWORD, "tcp://localhost:61616");
        try { // 构造从工厂得到连接对象
            connection = connectionFactory.createConnection();
            // 启动
            connection.start();
            // 获取操作连接
            session = connection.createSession(Boolean.TRUE,
                    Session.AUTO_ACKNOWLEDGE);
            // 获取session注意参数值FirstQueue是一个服务器的queue，须在在ActiveMq的console配置
            destination = session.createQueue(queueName);
            // 得到消息生成者【发送者】
            producer = session.createProducer(destination);
            // 设置不持久化，此处学习，实际根据项目决定
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

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

    /**
     * 发送消息
     * @param session
     * @param producer
     * @param msg 消息内容
     * @throws Exception
     */
    public static void sendMessage(Session session, MessageProducer producer,String msg)
            throws Exception {

        TextMessage message = session.createTextMessage(msg);
        // 发送消息到目的地方
        // System.out.println("发送消息：" + "ActiveMq 发送的消息" + msg);
        producer.send(message);

        session.commit();
    }

}

