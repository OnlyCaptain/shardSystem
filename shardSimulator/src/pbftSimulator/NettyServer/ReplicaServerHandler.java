package pbftSimulator.NettyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import net.sf.json.JSONObject;
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
            JSONObject js = JSONObject.fromObject(jsbuff);
            int type = js.getInt("type");
            switch (type) {
                case Message.REQUEST:
                    this.replica.logger.debug(replica.name+" receive Request");
                    baseMsg = new RequestMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.PREPREPARE:
                    this.replica.logger.debug(replica.name+" receive PrePrepare");
                    baseMsg = new PrePrepareMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.PREPARE:
                    this.replica.logger.debug(replica.name+" receive Prepare");
                    baseMsg = new PrepareMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.COMMIT:
                    this.replica.logger.debug(replica.name+" receive commit");
                    baseMsg = new CommitMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
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
