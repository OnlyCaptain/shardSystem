package pbftSimulator.NettyServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import pbftSimulator.PBFTSealer;
import shardSystem.config;

public class PBFTTest {
    public PBFTSealer pbftSealer;
    public PBFTTest() {
        this.pbftSealer = new PBFTSealer("0", 0, "127.0.0.1", 62458);
    }

    public static void main(String[] args) throws InterruptedException {
        config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config-dev-oneshard.json");
        PBFTTest pbftTest = new PBFTTest();
        new Thread(pbftTest.pbftSealer).start();
    }
}
