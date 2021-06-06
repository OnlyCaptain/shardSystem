package pbftSimulator.NettyServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import pbftSimulator.NettyMessage.*;
import pbftSimulator.replica.Replica;


public class NettyServerBootstrap {
    private int port;
    private Replica replica;
    private SocketChannel socketChannel;

    /**
     * 构造函数. 被调用后运行bind()
     * @param port 服务端端口
     * @throws InterruptedException
     */
    public NettyServerBootstrap(int port, Replica replica) throws InterruptedException {
        this.port = port;
        this.replica = replica;
        System.out.println("我这里是节点"+this.replica.id+"的nettyBootstrap");
        bind();
    }

    /**
     * Server开启的核心代码。
     * 其中 NettyServerHandler是 Server “接收消息”的代码。
     * @throws InterruptedException
     */
    private void bind() throws InterruptedException {
        EventLoopGroup boss=new NioEventLoopGroup();
        EventLoopGroup worker=new NioEventLoopGroup();
        ServerBootstrap bootstrap=new ServerBootstrap();
        bootstrap.group(boss,worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline p = socketChannel.pipeline();
                p.addLast(new ObjectEncoder());
                p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                p.addLast(new ReplicaServerHandler(this.replica));
            }
        });
        ChannelFuture f= bootstrap.bind(port).sync();
        if(f.isSuccess()){
            System.out.println("server start---------------");
        }
    }



    /**
     * Server的测试代码
     * @param args
     * @throws InterruptedException
     */
    public static void main(String []args) throws InterruptedException {
        //服务端打开连接，传入server的port
        NettyServerBootstrap bootstrap=new NettyServerBootstrap(9999);

        //发一条ask消息（用于验证连接成功）
        while (true){
            SocketChannel channel=(SocketChannel)NettyChannelMap.get("001");
            if(channel!=null){
                AskMsg askMsg=new AskMsg();
                channel.writeAndFlush(askMsg);
            }
            break;
//            TimeUnit.SECONDS.sleep(10);
        }
    }
}
