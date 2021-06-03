package shardSystem;

import pbftSimulator.replica.Replica;
import shardSystem.transaction.Transaction;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * @author zhanjzh
 * 分片的节点。除了pbft之外的功能
 */
public class shardNode extends Replica {
	
	public static final int SHARDNUM = 1;    // 分片的数量
	public static final int SLICENUM = 2;    // 地址倒数几位，作为识别分片的依据

	public static String NAME = "shardNode_";

	public String shardID;    // 节点所属分片 ID
	public String IP;       // 节点的IP标识符
	public int Port;       // 节点的端口号，作为服务端监听使用的
	public String name;
	public String url;    // 数据库 url
	public Map<String, String> addrShard;
	public Queue<Transaction> txPending;



	public shardNode(int id, int[] netDlys, int[] netDlyToClis) {
		super(NAME, id, netDlys, netDlyToClis);
		this.name = NAME.concat(String.valueOf(id));

		System.out.println(this.curWorkspace);

		shardID = "0";
		IP = "127.0.0.1";
		Port = 2010;
		url = "jdbc:sqlite:".concat(this.curWorkspace).concat(this.name).concat("-sqlite.db");
		createDB();

		txPending = new PriorityQueue<>(Transaction.cmp);

		// 地址映射分片表
		String[] hex = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
		addrShard = new HashMap<>();
		int n = (int)Math.ceil(16*16 / SHARDNUM), k = n, curS = 0;
		// 两位 -> 
		for (int i = 0; i < 16; i ++) {
			for (int j = 0; j < 16; j ++) {
				addrShard.put(hex[i].concat(hex[j]), String.valueOf(curS));
				k --;
				if (k <= 0) {
					k = n;
					curS ++;
				}
			}
		}
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
					+ " id integer PRIMARY KEY AUTOINCREMENT,\n" 
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
	 * 处理交易
	 * TODO
	 * @param tx 交易类
	 */
	public void txProcess(Transaction tx) {
		if (!validateTx(tx)) {
			logger.warning("this is a invalid transaction. "+tx.toString());
			return;
		}
//		if (shardID == queryShardID(tx.getSender())) {
			txPending.add(tx);
			txMemory(tx);
			printTx();
//		}
		// TODO
	}

	/**
	 * 根据地址查询该地址所在的分片。
	 * TODO
	 * @param addr 表示查询的地址（账户地址）
	 * @return	返回对应的分片ID
	 */
	public String queryShardID(String addr) {
		// 查询的规则有两种方式：
		String result;

		// 1. 根据尾数 mod
		String slice = addr.substring(addr.length()-SLICENUM, addr.length());
		result = addrShard.get(slice);

		// 2. 根据地址数据库查询 
		// TODO
		return result;  // 一开始只有一个分片
	}

	/**
	 * 发送跨分片交易的后半段
	 */
	public void sendCrossTx() {
		
	}

	/**
	 * 插入数据库
	 * @param tx 交易类
	 */
	public void txMemory(Transaction tx) {
		String sql = "INSERT INTO transactions(sender, recipient, value, timestamp, gasPrice, accountNonce) VALUES(?,?,?,?,?,?)";

		Connection conn = connect();
        try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			// Statement stmt = conn.createStatement();
			pstmt.setString(1, tx.getSender());
			pstmt.setString(2, tx.getRecipient());
			pstmt.setDouble(3, tx.getValue());
			pstmt.setLong(4, tx.getTimestamp());
			pstmt.setDouble(5, tx.getGasPrice());
			pstmt.setLong(6, tx.getAccountNonce());

            pstmt.executeUpdate();
			logger.info("Connection to SQLite has been established: ".concat(this.url));

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
	
}
