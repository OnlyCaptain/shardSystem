package pbftSimulator.NettyClient;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;


import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;


public class NettyClientBootstrap {
    private Logger logger;
    private int port;
    private String host;
    public SocketChannel socketChannel;
    public EventLoopGroup eventLoopGroup;
    private static final EventExecutorGroup group = new DefaultEventExecutorGroup(20);

    public NettyClientBootstrap(int port, String host, Logger logger) throws Exception {
        this.port = port;
        this.host = host;
        this.logger = logger;
        start();
    }

    private void start() throws Exception {
        eventLoopGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(eventLoopGroup);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,false);
        bootstrap.remoteAddress(host,port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ByteBuf delimiter = Unpooled.copiedBuffer("\t".getBytes());
                socketChannel.pipeline().addLast("framer", new DelimiterBasedFrameDecoder(1000*2048,delimiter));
                socketChannel.pipeline().addLast("decoder", new StringEncoder());
                socketChannel.pipeline().addLast("encoder", new StringDecoder());
//                socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024*500,0,4,0,4));
                socketChannel.pipeline().addLast(new NettyClientHandler());
            }
        });
        ChannelFuture future =bootstrap.connect(host,port).sync();
        if (future.isSuccess()) {
            socketChannel = (SocketChannel)future.channel();
            logger.info("connect server  成功---------");
        }
    }
};