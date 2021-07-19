package shardSystem;

import com.google.gson.Gson;
import pbftSimulator.PBFTSealer;
import pbftSimulator.message.*;
import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * @author zhanjzh
 * 分片的节点。除了pbft之外的功能
 */
public class shardNode extends Replica {

	public static String NAME = "shardNode_";

	public String name;
	public String url;    // 数据库 url
	public int blockNum;

	public shardNode(String shardID, int id, String IP, int port) {
		super(NAME, shardID, id, IP, port);
		blockNum = 0;

		this.name = "shard_".concat(shardID).concat("_").concat(NAME).concat(String.valueOf(id));
		System.out.println(this.curWorkspace);
		url = "jdbc:sqlite:".concat(this.curWorkspace).concat(this.name).concat("-sqlite.db");
		createDB();
		this.logger.info(String.format("我是第 %s 分片的第 %d 号节点，ip地址：%s, 端口号 %d", this.shardID, this.id, this.IP, this.port));
		if (isPrimary())
			this.logger.info("我是primary");
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
					+ " digest text PRIMARY KEY NOT NULL,\n"
					+ " sender text NOT NULL,\n"
					+ " recipient text NOT NULL,\n"
					+ " timestamp integer NOT NULL,\n"
					+ " gasPrice real NOT NULL,\n"
					+ " accountNonce integer NOT NULL,\n"
					+ " value real NOT NULL,\n"
					+ " Broadcast integer NOT NULL,\n"
					+ " Monoxide_d1 integer NOT NULL,\n"
					+ " Monoxide_d2 integer NOT NULL,\n"
					+ " Metis_d1 integer NOT NULL,\n"
					+ " Metis_d2 integer NOT NULL,\n"
					+ " Proposed_d1 integer NOT NULL,\n"
					+ " Proposed_d2 integer NOT NULL\n"
					+ " );";
            stmt.execute(sql);
			logger.info("Connection to SQLite has been established: ".concat(this.url));

        } catch (SQLException e) {
            this.logger.error("创建数据库出错"+e.getMessage());
        }
    }

	/**
	 * 判断交易是否被执行过，true说明有，false说明没有
	 * @param tx
	 * @return
	 */
	public boolean isExecuted(Connection conn, Transaction tx) {
		String sql = "SELECT * FROM transactions where digest = ?";

        try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, tx.getDigest());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				this.logger.info("这个交易已经被执行过："+rs.getString("sender")
							+rs.getString("recipient")
							+rs.getDouble("value")
							+rs.getLong("timestamp")
							+rs.getLong("gasPrice")
							+rs.getLong("accountNonce")
							);
				return true;
			}
        } catch (SQLException e) {
            System.out.println("这里查询失败："+e.getMessage());
        }
		return false;
	}

	/**
	 * 处理交易
	 * TODO
	 * @param txs 交易类的数组
	 */
	public void txProcess(ArrayList<Transaction> txs) {
		Connection conn = this.connect();
		System.out.println("调用执行交易函数, 交易数" + txs.size());

		// 收集跨分片交易
		Map<String, ArrayList<Transaction>> crossTx = new HashMap<>();
		for (int i = 0; i < txs.size(); i ++) {
			Transaction tx = txs.get(i);

			if (!validateTx(tx)) {
				logger.warn("this is a invalid transaction. "+tx.toString());
				continue;
			}

			if (isExecuted(conn, tx)) {
//				System.out.println("执行过啦");
				continue;
			}
			txMemory(conn, tx);

			String sendShard = "null", reciShard = "null";
			// 这里可能需要判断是哪个地方的交易，然后再转发 relay Transaction。
			// 考虑到 Primary 的存在，可以把转发部分交给Primary。
			if (config.RELAY_TX_FORWARD) {
				try {
					ArrayList<String> shardIDs = this.queryShardIDs(tx);
					if (shardIDs.size() == 0) {
						System.out.println("Error!!, 交易没查到分片id");
						this.logger.error("Error!!, 交易没查到分片id"+tx.toString());
						continue;
					}
					sendShard = shardIDs.get(0);
					reciShard = shardIDs.get(1);
					if (isPrimary()) {
						if (!sendShard.equals(this.shardID) && !reciShard.equals(this.shardID)) {
							this.logger.error("收到了错误的交易，打包区块的时候没写好");
							this.logger.error(String.format("transaction: %s", tx.encoder()));
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
				} catch (NullPointerException e) {
					System.out.println(e.getMessage());
					System.out.println(String.format("发送分片：%s 接收分片: %s 本分片: %s",sendShard, reciShard, this.shardID));
				}
			}
		}

		if (config.RELAY_TX_FORWARD) {
			for (Map.Entry<String, ArrayList<Transaction>> entry : crossTx.entrySet()) {
				this.logger.debug(String.format("在分片%s 中，属于分片 %s 的跨分片交易有：%d 条", this.shardID, entry.getKey(), entry.getValue().size()));
			}
			for (Map.Entry<String, ArrayList<Transaction>> entry : crossTx.entrySet()) {
				sendCrossTx(entry.getValue(), entry.getKey());
			}
		}
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据地址查询该地址所在的分片。
	 * TODO
	 * @param addr 表示查询的地址（账户地址）
	 * @return	返回对应的分片ID
	 */
	public String queryShardID(String addr) {
		// 查询的规则有两种方式：
		String result = null;

		// 1. 根据尾数 mod
		String slice = addr.substring(addr.length()-config.SLICENUM, addr.length());
		result = config.addrShard.get(slice);

		if (result == null) {
			System.out.println("问题地址："+addr);
		}
		// 2. 根据地址数据库查询 
		// TODO
		return result;  // 一开始只有一个分片
	}

	public ArrayList<String> queryShardIDs(Transaction tx) {
		// 分片规则有三种： origin，monoxide，metis，proposed
		ArrayList<String> result = new ArrayList<>();
		String[] keys = config.topos.keySet().toArray(new String[config.SHARDNUM]);
		int s1, s2;
		switch (config.sharding_rule) {
			case "origin":
				String addr1 = tx.getSender();
				String slice1 = addr1.substring(addr1.length()-config.SLICENUM, addr1.length());
				result.add(config.addrShard.get(slice1));
				String addr2 = tx.getRecipient();
				String slice2 = addr2.substring(addr2.length()-config.SLICENUM, addr2.length());
				result.add(config.addrShard.get(slice2));
				break;
			case "monoxide":
				s1 = tx.getMonoxide_d1();
				s2 = tx.getMonoxide_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			case "metis":
				s1 = tx.getMetis_d1();
				s2 = tx.getMetis_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			case "proposed":
				s1 = tx.getProposed_d1();
				s2 = tx.getProposed_d2();
				result.add(keys[s1 % config.SHARDNUM]);
				result.add(keys[s2 % config.SHARDNUM]);
				break;
			default:
				System.out.println("Error!!, sharding rule is wrong");
		}
		return result;
	}


	/**
	 * 发送跨分片交易的后半段
	 */
	public void sendCrossTx(ArrayList<Transaction> txs, String targetShard) {
		RawTxMessage rt = new RawTxMessage(txs);
		String targetIP = config.topos.get(targetShard).get(0).getIP();
		this.sendMsg(targetIP, config.PBFTSEALER_PORT, rt, sendTag, this.logger);
	}

	/**
	 * 插入数据库
	 * @param tx 交易类
	 */
	public void txMemory(Connection conn, Transaction tx) {
		String sql = "INSERT INTO transactions(sender,recipient,value,timestamp,gasPrice,accountNonce,"
				+"digest,Broadcast,Monoxide_d1,Monoxide_d2,Metis_d1,Metis_d2,Proposed_d1,Proposed_d2)"
				+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
			pstmt.setLong(8,tx.getBroadcast());
			pstmt.setInt(9,tx.getMonoxide_d1());
			pstmt.setInt(10,tx.getMonoxide_d2());
			pstmt.setInt(11,tx.getMetis_d1());
			pstmt.setInt(12,tx.getMetis_d2());
			pstmt.setInt(13,tx.getProposed_d1());
			pstmt.setInt(14,tx.getProposed_d2());

            pstmt.executeUpdate();
			logger.info("insert finished: ");

        } catch (SQLException e) {
            System.out.println(this.name + "这里插入失败 " + tx.getDigest() + e.getMessage() + tx.encoder());
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

	@Override
	public void execute(Message msg, long time) {
		PrePrepareMsg mm = (PrePrepareMsg)msg;
		RequestMsg rem = null;
		ReplyMsg rm = null;
		if(mm.m != null) {
			rem = (RequestMsg)(mm.m);
			rm = new ReplyMsg(mm.v, rem.t, rem.c, id, "result", id, rem.c, time);
		}
		
		if((rem == null || !isInMsgCache(rm)) && mm.n == lastRepNum + 1 && commited(mm)) {
			lastRepNum++;
			setTimer(lastRepNum+1, time);
			if(rem != null) {
				ArrayList<Transaction> txs = rem.txs;
				System.out.print("正在上链中... ");
				txProcess(txs);
				this.blockNum ++;   // 块上链 + 1；
				// 处理交易，上链
				if (rem.txs.size() > 0 && isPrimary()) {
					TimeMsg tmsg = new TimeMsg(txs, System.currentTimeMillis(), TimeMsg.CommitTag);
					sendTimer(config.COLLECTOR_IP, config.COLLECTOR_PORT, tmsg, this.logger);
				}
				System.out.println(String.format("块上链成功，交易数：%d, 这是第 %d 个块", txs.size(), this.blockNum));

				sendMsg(sealerIPs.get(PBFTSealer.getCliId(rem.c)).getIP(), sealerIPs.get(PBFTSealer.getCliId(rem.c)).getPort(), rm, sendTag, this.logger);
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
				// config.sendMsgToOthers(checkptMsg, id, sendTag, this.logger);
				// sendMsgToOthers(checkptMsg, sendTag, this.logger);
			}
		}
	}


	
}
