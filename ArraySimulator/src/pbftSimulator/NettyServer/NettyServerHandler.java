package pbftSimulator.NettyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import pbftSimulator.NettyMessage.*;

import java.net.InetSocketAddress;

public class NettyServerHandler extends SimpleChannelInboundHandler<BaseMsg> {
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel)ctx.channel());
    }
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, BaseMsg baseMsg) throws Exception {

//        if(MsgType.LOGIN.equals(baseMsg.getType())){
//        map中没有该client，需要记录其信息
        if(NettyChannelMap.get(baseMsg.getClientId())==null) {
            LoginMsg loginMsg = (LoginMsg) baseMsg;

            NettyChannelMap.add(loginMsg.getClientId(), (SocketChannel) channelHandlerContext.channel());

            InetSocketAddress insocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
            String ip = insocket.getAddress().getHostAddress();
            int port = insocket.getPort();
            System.out.println("ip: " + ip + "    port: " + port);

            System.out.println("client" + loginMsg.getClientId() + " 登录成功");
            
        }
//        }else{
//            if(NettyChannelMap.get(baseMsg.getClientId())==null){
//                    //说明未登录，或者连接断了，服务器向客户端发起登录请求，让客户端重新登录
//                    LoginMsg loginMsg=new LoginMsg();
//                    channelHandlerContext.channel().writeAndFlush(loginMsg);
//            }
//        }
        switch (baseMsg.getType()){
            case PING:{
                PingMsg pingMsg=(PingMsg)baseMsg;
                PingMsg replyPing=new PingMsg();
                NettyChannelMap.get(pingMsg.getClientId()).writeAndFlush(replyPing);

                InetSocketAddress insocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
                String ip = insocket.getAddress().getHostAddress();
                int port = insocket.getPort();
                System.out.println( "id:  " +  pingMsg.getClientId() + "ip: "+ ip +"    port: " + port);

            }break;
            case ASK:{
                //收到客户端的请求
                AskMsg askMsg=(AskMsg)baseMsg;
                if("authToken".equals(askMsg.getParams().getAuth())){

                    System.out.println("askMsg.getClientId(): " + askMsg.getClientId());

                    ReplyServerBody replyBody=new ReplyServerBody("server info $$$$ !!!");
                    ReplyMsg replyMsg=new ReplyMsg();
                    replyMsg.setBody(replyBody);
                    NettyChannelMap.get(askMsg.getClientId()).writeAndFlush(replyMsg);
                }
            }break;
            case REPLY:{
                //收到客户端回复
                ReplyMsg replyMsg=(ReplyMsg)baseMsg;
                ReplyClientBody clientBody=(ReplyClientBody)replyMsg.getBody();
                System.out.println("receive client msg: "+clientBody.getClientInfo());
            }break;

            default:break;
        }
        ReferenceCountUtil.release(baseMsg);
    }
}
