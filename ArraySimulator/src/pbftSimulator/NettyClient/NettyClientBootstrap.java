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
import org.apache.log4j.Logger;


public class NettyClientBootstrap {
    private Logger logger;
    private int port;
    private String host;
    public SocketChannel socketChannel;
    public EventLoopGroup eventLoopGroup;
    private static final EventExecutorGroup group = new DefaultEventExecutorGroup(20);

    /**
     * 构造函数. start()
     * @param port 服务端端口
     * @param host 服务端地址
     * @throws InterruptedException
     */
    public NettyClientBootstrap(int port, String host, Logger logger) throws InterruptedException {
        this.port = port;
        this.host = host;
        this.logger = logger;
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Client 开启的核心代码。
     * 其中 NettyClientHandler Client “接收消息”的代码。
     * @throws InterruptedException
     */
    private void start() throws InterruptedException {
        eventLoopGroup=new NioEventLoopGroup();
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
            logger.info("connect server  成功---------");
        }
    }



    //多线程调用client。
    public static class NettyClient_thread extends Thread {
        private int clientId;
        private String clientId_str ;
        private int serverPort;

        public NettyClient_thread(int clientId , int serverPort) {
            this.clientId = clientId;
            this.clientId_str = String.format("%03d",clientId);
            this.serverPort = serverPort;
        }

        /**
         * 建立与 Server 的连接的代码。
         */
        @Override
        public void run() {
            //set clientId.  message会从Constants中获取该Id
            Constants.setClientId(clientId_str);
//            System.out.println(Constants);
            try {
                //与 Server 建立连接
                NettyClientBootstrap bootstrap = new NettyClientBootstrap(serverPort, "localhost", null);

                //登陆验证。首次登陆时在Server注册 Client的信息
                LoginMsg loginMsg = new LoginMsg();
                loginMsg.setPassword("");
                loginMsg.setUserName("");
                bootstrap.socketChannel.writeAndFlush(loginMsg);

//                发送心跳信息。用于验证连接没断开
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


    /**
     * Client 的测试代码
     * @param args
     * @throws Exception
     */
    public static void main(String[]args) throws Exception{
        //指定 ClientId 和服务端的 port
        int clinentId = 50;
        int serverPort = 9999;

        //使用多线程的方式建立与 Server 的连接
        NettyClient_thread t0 = new NettyClient_thread(clinentId, serverPort);

        t0.start();


    }
}

