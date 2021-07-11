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
import pbftSimulator.Simulator;
import pbftSimulator.message.*;
import pbftSimulator.replica.Replica;
import shardSystem.shardNode;
import shardSystem.transaction.Transaction;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

                    RawTxMessage rawTxMessage = new RawTxMessage(jsbuff);
                    JSONArray m = rawTxMessage.getM();
                    // 划分出属于本分片的交易与不属于本分片的交易
                    Map<String, ArrayList<Transaction>> classifi = new HashMap<>();
                    for(int i=0;i<m.size();i++){
                        Transaction tx = new Transaction(m.get(i).toString());
                        String sendShard = this.sealer.queryShardID( tx.getSender());
                        String reciShard = this.sealer.queryShardID( tx.getRecipient());
                        if (!sendShard.equals(this.sealer.shardID) && !reciShard.equals(this.sealer.shardID)) {
                            if (!classifi.keySet().contains(sendShard)) {
                                classifi.put(sendShard, new ArrayList<Transaction>());
                            }
                            ArrayList<Transaction> buf = classifi.get(sendShard);
                            buf.add(tx);
                        }
                        else {
                            if (!classifi.keySet().contains(this.sealer.shardID)) {
                                classifi.put(this.sealer.shardID, new ArrayList<Transaction>());
                            }
                            ArrayList<Transaction> buf = classifi.get(this.sealer.shardID);
                            buf.add(tx);
                        }
                    }
                    int cur = 0, noCur = 0;
                    for (Map.Entry<String, ArrayList<Transaction>> entry : classifi.entrySet()) { 
                        if (entry.getKey().equals(this.sealer.shardID)) cur = entry.getValue().size();
                        else noCur += entry.getValue().size();
                    }  
                    this.sealer.logger.debug(String.format("在分片%s 中，属于本分片的交易共有：%d 条,不属于本分片的交易有：%d 条", this.sealer.shardID, cur, noCur));
                    
                    
                    //将本shard的Tx放入消息队列
                    MqSender mqSender = new MqSender(this.sealer.txPoolName);
                    ArrayList<Transaction> thisShardTxs = classifi.get(sealer.shardID);
                    if (null != thisShardTxs) {
                        for (Transaction thisShradTx : thisShardTxs) {
                            mqSender.sendMessage(mqSender.session, mqSender.producer, thisShradTx.encoder());
                        }
                    }
                    try {
                        if (null != mqSender.connection)
                            mqSender.connection.close();
                    } catch (Throwable ignore) {

                    }
                    this.sealer.logger.debug(classifi.toString());
                    //将其他shard的Tx发送到其PBFTSealer
                    for(String shard : classifi.keySet()){
                        if(shard.equals(sealer.shardID)){
                            continue;
                        }else{
                            sealer.sendToOtherShard(classifi.get(shard), shard);
                        }
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
