package pbftSimulator.NettyServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

import pbftSimulator.PBFTSealer;
import pbftSimulator.message.Message;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.*;

public class PBFTSealerServerHandler extends SimpleChannelInboundHandler<String> {
    private PBFTSealer sealer;
    public PBFTSealerServerHandler(PBFTSealer sealer) {
		super();
        this.sealer = sealer;
	}
    
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel)ctx.channel());
    }
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, String jsbuff) throws Exception {
        Message baseMsg = null; 
        try {
            JsonObject js= new JsonParser().parse(jsbuff).getAsJsonObject();
            int type = js.get("type").getAsInt();

            switch (type) {
                case Message.REPLY:
                    this.sealer.logger.debug(this.sealer.name+" receive Reply");
                    baseMsg = new Gson().fromJson(jsbuff, ReplyMsg.class);
                    break;
                case Message.CLITIMEOUT:
                    this.sealer.logger.debug(this.sealer.name+" receive Timeout");
                    baseMsg = new Gson().fromJson(jsbuff, CliTimeOutMsg.class);
                    break;
                case Message.TRANSACTION:
                    this.sealer.logger.debug(this.sealer.name+" receive Transaction");
                    RawTxMessage rawTxMessage = new Gson().fromJson(jsbuff, RawTxMessage.class);
                    this.sealer.receiveTransactions(rawTxMessage);
                    break;
                default:
                    this.sealer.logger.debug("【Error】消息类型错误！");
            }
            if (type != Message.TRANSACTION && baseMsg != null)
                sealer.msgProcess(baseMsg);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("PBFTSealer exit.");
        } finally {
            ReferenceCountUtil.release(baseMsg);
        }
    }







}
