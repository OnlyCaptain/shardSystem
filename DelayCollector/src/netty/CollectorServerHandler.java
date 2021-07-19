package netty;

import collector.Collector;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import pbftSimulator.message.TimeMsg;


public class CollectorServerHandler extends SimpleChannelInboundHandler<String> {
    private Collector collector;

    public CollectorServerHandler() {
		super();
	}

    public CollectorServerHandler(Collector collector) {
		super();
		System.out.println("创建collector监听");
        this.collector = collector;
	}
    
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel)ctx.channel());
    }

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, String jsbuff) throws Exception {
//        System.out.println("收到消息"+jsbuff);
        TimeMsg baseMsg = null;
        try {
            baseMsg = new Gson().fromJson(jsbuff, TimeMsg.class);
            if (baseMsg == null) {
                this.collector.logger.error("出错啦这里是Replica后端"+jsbuff);
                return;
            }
            System.out.println("记录打点时间："+baseMsg.getTime());
            this.collector.txMemory(baseMsg);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ShardNode exit.");
        } finally {
            ReferenceCountUtil.release(baseMsg);
        }
    }
}
