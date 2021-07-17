package pbftSimulator.NettyServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import pbftSimulator.message.*;
import pbftSimulator.replica.Replica;

public class ReplicaServerHandler extends SimpleChannelInboundHandler<String> {
    private Replica replica;
	
    public ReplicaServerHandler() {
		super();
	}

    public ReplicaServerHandler(Replica replica) {
		super();
        this.replica = replica;
	}
    
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel)ctx.channel());
    }
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, String jsbuff) throws Exception {
        Message baseMsg = null; 
        try {
            JsonObject js = new JsonParser().parse(jsbuff).getAsJsonObject();
            int type = js.get("type").getAsInt();
//            System.out.println(String.format("收到了 %d 类消息：%s", type, jsbuff));
            switch (type) {
                case Message.REQUEST:
                    baseMsg = new Gson().fromJson(jsbuff, RequestMsg.class);
                    this.replica.logger.debug(replica.name+" receive Request" + baseMsg.encoder());
                    break;
                case Message.PREPREPARE:
                    baseMsg = new Gson().fromJson(jsbuff, PrePrepareMsg.class);
                    this.replica.logger.debug(replica.name+" receive PrePrepare" + baseMsg.encoder());
                    break;
                case Message.PREPARE:
                    baseMsg = new Gson().fromJson(jsbuff, PrepareMsg.class);
                    this.replica.logger.debug(replica.name+" receive Prepare" + baseMsg.encoder());
                    break;
                case Message.COMMIT:
                    baseMsg = new Gson().fromJson(jsbuff, CommitMsg.class);
                    this.replica.logger.debug(replica.name+" receive commit" + baseMsg.encoder());
                    break;
                case Message.VIEWCHANGE:
                    break;
                case Message.NEWVIEW:
                    break;
                case Message.TIMEOUT:
                    break;
                case Message.CHECKPOINT:
                    break;
                default:
                    this.replica.logger.info("【Error】消息类型错误！");
                    return;
            }
            if (baseMsg == null) {
                this.replica.logger.debug("这里是Replica后端"+jsbuff);
                return;
            }
            replica.msgProcess(baseMsg);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ShardNode exit.");
        } finally {
            ReferenceCountUtil.release(baseMsg);
        }
    }
}
