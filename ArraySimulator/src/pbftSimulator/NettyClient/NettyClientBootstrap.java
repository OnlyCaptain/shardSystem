package pbftSimulator.NettyClient;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import pbftSimulator.NettyMessage.AskMsg;
import pbftSimulator.NettyMessage.AskParams;
import pbftSimulator.NettyMessage.Constants;
import pbftSimulator.NettyMessage.LoginMsg;

import java.util.concurrent.TimeUnit;


public class NettyClientBootstrap {
    private int port;
    private String host;
    public SocketChannel socketChannel;
    private static final EventExecutorGroup group = new DefaultEventExecutorGroup(20);
    public NettyClientBootstrap(int port, String host) throws InterruptedException {
        this.port = port;
        this.host = host;
        start();
    }
    private void start() throws InterruptedException {
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true);
        bootstrap.group(eventLoopGroup);
        bootstrap.remoteAddress(host,port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new IdleStateHandler(20,10,0));
                socketChannel.pipeline().addLast(new ObjectEncoder());
                socketChannel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                socketChannel.pipeline().addLast(new NettyClientHandler());
            }
        });
        ChannelFuture future =bootstrap.connect(host,port).sync();
        if (future.isSuccess()) {
            socketChannel = (SocketChannel)future.channel();
            System.out.println("connect server  成功---------");
        }



    }






    //多线程调用client。暂时用不上。
    public static class NettyClient_thread extends Thread {
        private int clientId;
        private String clientId_str ;
        private int serverPort;

        public NettyClient_thread(int clientId , int serverPort) {
            this.clientId = clientId;
            this.clientId_str = String.format("%03d",clientId);
            this.serverPort = serverPort;
            System.out.println(clientId_str);
        }

        @Override
        public void run() {
            Constants.setClientId(clientId_str);
//            System.out.println(Constants);
            try {
                NettyClientBootstrap bootstrap = new NettyClientBootstrap(serverPort, "localhost");

                LoginMsg loginMsg = new LoginMsg();
                loginMsg.setPassword("yao");
                loginMsg.setUserName("robin");
                bootstrap.socketChannel.writeAndFlush(loginMsg);
                while (true) {
                    TimeUnit.SECONDS.sleep(10);
                    AskMsg askMsg = new AskMsg();
                    AskParams askParams = new AskParams();
                    askParams.setAuth("authToken");
                    askMsg.setParams(askParams);
                    bootstrap.socketChannel.writeAndFlush(askMsg);
                    break;
                }
            }catch (
                    Exception e) {
                e.printStackTrace();
            }
        }
    }



    public static void main(String[]args) throws Exception{

        int i = 50;
        int serverPort = 9999;
        NettyClient_thread t0 = new NettyClient_thread(i,serverPort);


//        NettyClient_thread t1 = new NettyClient_thread(61);


        t0.start();

//        TimeUnit.SECONDS.sleep(5);
//        t1.start();

    }
}

