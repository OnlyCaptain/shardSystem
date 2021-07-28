package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;


public class NettyClientHandler extends SimpleChannelInboundHandler<String> {
    // @Override
    // public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    //     if (evt instanceof IdleStateEvent) {
    //         IdleStateEvent e = (IdleStateEvent) evt;
    //         switch (e.state()) {
    //             case WRITER_IDLE:
    //                 PingMsg pingMsg=new PingMsg();
    //                 ctx.writeAndFlush(pingMsg);
    //                 System.out.println("send ping to server----------");
    //                 break;
    //             default:
    //                 break;
    //         }
    //     }
    // }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String baseMsg) throws Exception {
        // MsgType msgType=baseMsg.getType();
        System.out.println("Receive: "+baseMsg+ " after send.");

//        判断消息类型，并作相应处理
        // switch (msgType){
        //     case LOGIN:{
        //         //向服务器发起登录
        //         LoginMsg loginMsg=new LoginMsg();
        //         loginMsg.setPassword("yao");
        //         loginMsg.setUserName("robin");
        //         channelHandlerContext.writeAndFlush(loginMsg);
        //     }break;
        //     case PING:{
        //         System.out.println("receive ping from server----------");
        //         InetSocketAddress insocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
        //         String ip = insocket.getAddress().getHostAddress();
        //         int port = insocket.getPort();
        //         System.out.println("ip: "+ ip +"    port: " + port);
        //     }break;
        //     case ASK:{
        //         ReplyClientBody replyClientBody=new ReplyClientBody("client info **** !!!");
        //         ReplyMsg replyMsg=new ReplyMsg();
        //         replyMsg.setBody(replyClientBody);
        //         channelHandlerContext.writeAndFlush(replyMsg);
        //     }break;
        //     case REPLY:{
        //         ReplyMsg replyMsg=(ReplyMsg)baseMsg;
        //         ReplyServerBody replyServerBody=(ReplyServerBody)replyMsg.getBody();
        //         System.out.println("receive client msg: "+replyServerBody.getServerInfo());
        //     }
        //     default:break;
        // }
        // ReferenceCountUtil.release(msgType);
    }
}
