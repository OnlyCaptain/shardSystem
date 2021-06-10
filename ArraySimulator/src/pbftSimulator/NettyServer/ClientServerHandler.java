package pbftSimulator.NettyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import net.sf.json.JSONObject;
import pbftSimulator.MQ.MqSender;
import pbftSimulator.PBFTSealer;
import pbftSimulator.NettyMessage.*;
import pbftSimulator.message.Message;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.RequestMsg;
import pbftSimulator.message.TimeOutMsg;
import pbftSimulator.replica.Replica;

import java.net.InetSocketAddress;

import javax.management.relation.RelationException;

public class ClientServerHandler extends SimpleChannelInboundHandler<String> {
    private PBFTSealer client;
	
    public ClientServerHandler() {
		super();
        this.client = null;
		// TODO Auto-generated constructor stub
	}

    public ClientServerHandler(PBFTSealer client) {
		super();
        this.client = client;
		// TODO Auto-generated constructor stub
	}
    
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel)ctx.channel());
    }
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, String jsbuff) throws Exception {
        
        Message baseMsg = null; 
        try {
            JSONObject js = JSONObject.fromObject(jsbuff);
            int type = js.getInt("type");
            // this.client.logger.info("客户端收到："+jsbuff);
            switch (type) {
                case Message.REPLY:
                    this.client.logger.debug(this.client.name+" receive Reply");
                    baseMsg = new ReplyMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.CLITIMEOUT:
                    this.client.logger.debug(this.client.name+" receive Timeout");
                    baseMsg = new TimeOutMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.Transaction:
                    //将收到的Tx放入消息队列
                    MqSender mqSender = new MqSender();
                    mqSender.sendMessage(mqSender.session, mqSender.producer, jsbuff);

                    //关闭Sender
                    try {
                        if (null != mqSender.connection)
                            mqSender.connection.close();
                    } catch (Throwable ignore) {
                    }

                    break;
                default:
                    this.client.logger.debug("【Error】消息类型错误！");
            }
            client.msgProcess(baseMsg);
            if(NettyChannelMap.get(baseMsg.getClientId())==null) {
                // LoginMsg loginMsg = (LoginMsg) baseMsg;
    
                NettyChannelMap.add(baseMsg.getClientId(), (SocketChannel) channelHandlerContext.channel());
    
                InetSocketAddress insocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
    
                String ip = insocket.getAddress().getHostAddress();
                int port = insocket.getPort();
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("exit.");
        } finally {
            ReferenceCountUtil.release(baseMsg);
        }

//        map中没有该client，需要记录其信息
        
        //        判断消息类型，并作相应处理
        // switch (baseMsg.getType()){
        //     case PING:{
        //         PingMsg pingMsg=(PingMsg)baseMsg;
        //         PingMsg replyPing=new PingMsg();
        //         NettyChannelMap.get(pingMsg.getClientId()).writeAndFlush(replyPing);

        //         InetSocketAddress insocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
        //         String ip = insocket.getAddress().getHostAddress();
        //         int port = insocket.getPort();
        //         System.out.println( "id:  " +  pingMsg.getClientId() + "ip: "+ ip +"    port: " + port);

        //     }break;
        //     case ASK:{
        //         //收到客户端的请求
        //         AskMsg askMsg=(AskMsg)baseMsg;
        //         if("authToken".equals(askMsg.getParams().getAuth())){

        //             System.out.println("askMsg.getClientId(): " + askMsg.getClientId());

        //             ReplyServerBody replyBody=new ReplyServerBody("server info $$$$ !!!");
        //             ReplyMsg replyMsg=new ReplyMsg();
        //             replyMsg.setBody(replyBody);
        //             NettyChannelMap.get(askMsg.getClientId()).writeAndFlush(replyMsg);
        //         }
        //     }break;
        //     case REPLY:{
        //         //收到客户端回复
        //         ReplyMsg replyMsg=(ReplyMsg)baseMsg;
        //         ReplyClientBody clientBody=(ReplyClientBody)replyMsg.getBody();
        //         System.out.println("receive client msg: "+clientBody.getClientInfo());
        //     }break;

        //     default:break;
        // }
        
    }
}
