package shardSystem;

import pbftSimulator.PBFTSealer;
import pbftSimulator.PairAddress;
import pbftSimulator.Simulator;
import pbftSimulator.message.CheckPointMsg;
import pbftSimulator.message.LastReply;
import pbftSimulator.message.Message;
import pbftSimulator.message.PrePrepareMsg;
import pbftSimulator.message.RawTxMessage;
import pbftSimulator.message.ReplyMsg;
import pbftSimulator.message.RequestMsg;
import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import org.apache.log4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * @author zhanjzh
 * 分片的节点。除了pbft之外的功能
 */
public class shardNode extends Replica {

	public static String NAME = "shardNode_";

	public String name;
	public String url;    // 数据库 url
	public static Map<String, String> addrShard = getAddrShard(new String[]{"0", "1", "2"});
	public Queue<Transaction> txPending;

	public shardNode(String shardID, int id, String IP, int port, int[] netDlys, int[] netDlyToClis, Map<String, ArrayList<PairAddress>> topos) {
		super(NAME, shardID, id, IP, port, netDlys, netDlyToClis, topos);

		this.name = "shard_".concat(shardID).concat("_").concat(NAME).concat(String.valueOf(id));
		System.out.println(this.curWorkspace);
		url = "jdbc:sqlite:".concat(this.curWorkspace).concat(this.name).concat("-sqlite.db");
		createDB();
		txPending = new PriorityQueue<>(Transaction.cmp);
		
	}

	/**
     * Connect to node database
	 * @return 返回根据 url 连接的数据库
     */
	private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

	/**
     * Connect to node database
	 * Create transaction tx;
     */
    private void createDB() {
		Connection conn = connect();
        try {
			Statement stmt = conn.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS transactions (\n" 
					// + " id integer PRIMARY KEY AUTOINCREMENT,\n" 
					+ " digest text PRIMARY KEY NOT NULL,\n"
					+ " sender text NOT NULL,\n"
					+ " recipient text NOT NULL,\n"
					+ " timestamp integer NOT NULL,\n"
					+ " gasPrice real NOT NULL,\n"
					+ " accountNonce integer NOT NULL,\n"
					+ " value real NOT NULL\n"
					+ " );";
            stmt.execute(sql);
			logger.info("Connection to SQLite has been established: ".concat(this.url));

        } catch (SQLException e) {
            this.logger.error("创建数据库出错"+e.getMessage());
        } finally {
			logger.info("创建数据库完成");
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

	public static Map<String, String> getAddrShard(String[] shards) {
		// 地址映射分片表
		String[] hex = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
		Map<String, String> addrs = new HashMap<>();
		int n = (int)Math.ceil(16*16 / Simulator.SHARDNUM), k = n, curS = 0;
		// 两位 -> 
		for (int i = 0; i < 16; i ++) {
			for (int j = 0; j < 16; j ++) {
				addrs.put(hex[i].concat(hex[j]), shards[curS]);
				k --;
				if (k < 0) {
					k = n;
					curS ++;
				}
			}
		}
		// System.out.println("查询中"+addrs.toString());
		return addrs;
	}

	/**
	 * 判断交易是否被执行过，true说明有，false说明没有
	 * @param tx
	 * @return
	 */
	public boolean isExecuted(Transaction tx) {
		boolean executedFlag = false;
		String sql = "SELECT * FROM transactions where digest = ?";

		Connection conn = connect();
        try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			// Statement stmt = conn.createStatement();
			pstmt.setString(1, tx.getDigest());
            // 
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				executedFlag = true;
				this.logger.info("这个交易已经被执行过："+rs.getString("sender")
							+rs.getString("recipient")
							+rs.getDouble("value")
							+rs.getLong("timestamp")
							+rs.getLong("gasPrice")
							+rs.getLong("accountNonce")
							);
			}
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
		return executedFlag;
	}

	/**
	 * 处理交易
	 * TODO
	 * @param txs 交易类的数组
	 */
	public void txProcess(ArrayList<Transaction> txs) {
		// 收集跨分片交易
		Map<String, ArrayList<Transaction>> crossTx = new HashMap<>();
		for (int i = 0; i < txs.size(); i ++) {
			Transaction tx = txs.get(i);
			if (!validateTx(tx)) {
				logger.warn("this is a invalid transaction. "+tx.toString());
				continue;
			}
			if (isExecuted(tx)) {
				// logger.warn("")
				continue;
			}
			// txPending.add(tx);
			// 这里可能需要判断是哪个地方的交易，然后再转发 relay Transaction。
			// 考虑到 Primary 的存在，可以把转发部分交给Primary。
			if (isPrimary()) {
				String sendShard = queryShardID(tx.getSender());
				String reciShard = queryShardID(tx.getRecipient());
				if (!sendShard.equals(this.shardID) && !reciShard.equals(this.shardID)) {
					this.logger.error("收到了错误的交易，打包区块的时候没写好");
					this.logger.error(String.format("sender: %s, recipient: %s, sendSHARD: %s, reciSHARD: %s", tx.getSender(), tx.getRecipient(), sendShard, reciShard));
				} else if (!sendShard.equals(this.shardID) && reciShard.equals(this.shardID)) {
					this.logger.debug("这个是relay tx的后半部分，停止转发");
				} else if (sendShard.equals(this.shardID) && !reciShard.equals(this.shardID)) {
					this.logger.debug("这个是relay tx的前半部分，需要转发");
					if (!crossTx.keySet().contains(reciShard)) {
						crossTx.put(reciShard, new ArrayList<Transaction>());
					}
					ArrayList<Transaction> buf = crossTx.get(reciShard);
					buf.add(tx);
				} else {
					this.logger.debug("存在片内交易");
				}
			}
			// 以下是存交易
			Connection conn = connect();
			txMemory(conn, tx);
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}
		for (Map.Entry<String, ArrayList<Transaction>> entry : crossTx.entrySet()) { 
			this.logger.debug(String.format("在分片%s 中，属于分片 %s 的跨分片交易有：%d 条", this.shardID, entry.getKey(), entry.getValue().size()));
		}  
                    
		for (Map.Entry<String, ArrayList<Transaction>> entry : crossTx.entrySet()) { 
			sendCrossTx(entry.getValue(), entry.getKey());
		} 
	}

	/**
	 * 根据地址查询该地址所在的分片。
	 * TODO
	 * @param addr 表示查询的地址（账户地址）
	 * @return	返回对应的分片ID
	 */
	public static String queryShardID(String addr) {
		// 查询的规则有两种方式：
		String result;

		// 1. 根据尾数 mod
		String slice = addr.substring(addr.length()-Simulator.SLICENUM, addr.length());
		result = addrShard.get(slice);

		// 2. 根据地址数据库查询 
		// TODO
		return result;  // 一开始只有一个分片
	}

	/**
	 * 发送跨分片交易的后半段
	 */
	public void sendCrossTx(ArrayList<Transaction> txs, String targetShard) {
		RawTxMessage rt = new RawTxMessage(txs);
		this.sendMsg(clients.get(0).getIP(), Simulator.PBFTSEALERPORT+Integer.parseInt(targetShard), rt, sendTag, this.logger);
	}

	/**
	 * 插入数据库
	 * @param tx 交易类
	 */
	public void txMemory(Connection conn, Transaction tx) {
		String sql = "INSERT INTO transactions(sender, recipient, value, timestamp, gasPrice, accountNonce, digest) VALUES(?,?,?,?,?,?,?)";
		// Connection conn = connect();
        try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			// Statement stmt = conn.createStatement();
			pstmt.setString(1, tx.getSender());
			pstmt.setString(2, tx.getRecipient());
			pstmt.setDouble(3, tx.getValue());
			pstmt.setLong(4, tx.getTimestamp());
			pstmt.setDouble(5, tx.getGasPrice());
			pstmt.setLong(6, tx.getAccountNonce());
			pstmt.setString(7, tx.getDigest());

            pstmt.executeUpdate();
			logger.info("Connection to SQLite has been established: ".concat(this.url));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
	}
	
	/**
	 * 从数据库中输出打印
	 */
	public void printTx() {
		String sql = "select * from transactions";

		Connection conn = connect();
        try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			System.out.println("Sender\t"+"Recipient\t"+"Value\t"+"timestamp\t"+"gasPrice\t"+"accountNonce\t");
			while (rs.next()) {
				System.out.println(rs.getString("sender")+"\t"
							+rs.getString("recipient")+"\t\t"
							+rs.getDouble("value")+"\t"
							+rs.getLong("timestamp")+"\t\t"
							+rs.getLong("gasPrice")+"\t\t"
							+rs.getLong("accountNonce")+"\t\t");
			}
			logger.info("Database print finished ".concat(this.url));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
	}

	/**
	 * 验证交易是否合法，包括：交易nonce是否大于账户最新nonce，检查余额是否合法
	 * TODO
	 * @param tx 交易类
	 * @return 返回交易是否合法  
	 * */
	public boolean validateTx(Transaction tx) {
		return true;
	}

	
	public void execute(Message msg, long time) {
		PrePrepareMsg mm = (PrePrepareMsg)msg;
		RequestMsg rem = null;
		ReplyMsg rm = null;
		if(mm.m != null) {
			rem = (RequestMsg)(mm.m);
			rm = new ReplyMsg(mm.v, rem.t, rem.c, id, "result", id, rem.c, time + netDlyToClis[PBFTSealer.getCliArrayIndex(rem.c)]);
		}
		
		if((rem == null || !isInMsgCache(rm)) && mm.n == lastRepNum + 1 && commited(mm)) {
			lastRepNum++;
			setTimer(lastRepNum+1, time);
			if(rem != null) {
				// Simulator.sendMsg(rm, sendTag, this.logger);
				// this.logger.info("再次确认request的消息结构："+rem.m.get(0));
				// executeTx();
				ArrayList<Transaction> txs = new ArrayList<>();
				for (int i = 0; i < rem.m.size(); i ++) {
					txs.add(new Transaction(rem.m.get(i).toString()));
				}
				txProcess(txs);
				// 处理交易
				sendMsg(clients.get(PBFTSealer.getCliId(rem.c)).getIP(), clients.get(PBFTSealer.getCliId(rem.c)).getPort(), rm, sendTag, this.logger);
				LastReply llp = lastReplyMap.get(rem.c);
				if(llp == null) {
					llp = new LastReply(rem.c, rem.t, "result");
					lastReplyMap.put(rem.c, llp);
				}
				llp.t = rem.t;
				reqStats.put(rem, STABLE);
				
			}
			//周期性发送checkpoint消息
			if(mm.n % K == 0) {
				Message checkptMsg = new CheckPointMsg(v, mm.n, lastReplyMap, id, id, id, time);
				addMessageToCache(checkptMsg);
				// Simulator.sendMsgToOthers(checkptMsg, id, sendTag, this.logger);
				// sendMsgToOthers(checkptMsg, sendTag, this.logger);
			}
		}
	}


	
}
