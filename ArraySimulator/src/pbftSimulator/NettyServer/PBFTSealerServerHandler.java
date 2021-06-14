package pbftSimulator.NettyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import pbftSimulator.MQ.MqSender;
import pbftSimulator.PBFTSealer;
import pbftSimulator.message.Message;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.RequestMsg;
import pbftSimulator.message.TimeOutMsg;
import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;

import java.net.InetSocketAddress;

import javax.management.relation.RelationException;

public class PBFTSealerServerHandler extends SimpleChannelInboundHandler<String> {
    private PBFTSealer sealer;
	
    public PBFTSealerServerHandler() {
		super();
        this.sealer = null;
		// TODO Auto-generated constructor stub
	}

    public PBFTSealerServerHandler(PBFTSealer sealer) {
		super();
        this.sealer = sealer;
		// TODO Auto-generated constructor stub
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
                case Message.REPLY:
                    this.sealer.logger.debug(this.sealer.name+" receive Reply");
                    baseMsg = new ReplyMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.CLITIMEOUT:
                    this.sealer.logger.debug(this.sealer.name+" receive Timeout");
                    baseMsg = new TimeOutMsg();
                    baseMsg = baseMsg.decoder(jsbuff);
                    break;
                case Message.TRANSACTION:
                    //将收到的Tx放入消息队列
                    MqSender mqSender = new MqSender();
                    
                    RawTxMessage msg = new RawTxMessage(jsbuff);
                    JSONArray ja = msg.getTxs();
                    for (int i = 0; i < ja.size(); i ++) {
                        Transaction txbuff = new Transaction(ja.get(i).toString());
                        mqSender.sendMessage(mqSender.session, mqSender.producer, txbuff.toString());
                    }

                    try {
                        if (null != mqSender.connection)
                            mqSender.connection.close();
                    } catch (Throwable ignore) {
                    }

                    break;
                default:
                    this.sealer.logger.debug("【Error】消息类型错误！");
            }
            if (baseMsg == null) return;
            sealer.msgProcess(baseMsg);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("PBFTSealer exit.");
        } finally {
            ReferenceCountUtil.release(baseMsg);
        }
    }
}
