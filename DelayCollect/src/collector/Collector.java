package collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import message.TimeMsg;
import net.sf.json.JSONArray;
import netty.CollectorServerHandler;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import message.Message;
import message.RawTxMessage;
import netty.NettyClientBootstrap;
import transaction.Transaction;

/**
 * @author zhanjzh
 *	light node in shard system.
 *  only send transactions.
 */
public class Collector {
	
	public static final int COLLECTOR_PORT = 57050;
	private static final Level LOGLEVEL = Level.INFO;
	
	public String IP;
	public int port;
	public Logger logger;
	public String curWorkspace;

	public String name;
	public String url;    // 数据库 url

	public EventLoopGroup boss;
	public EventLoopGroup worker;
	
	
	public Collector(String IP, int port) {
		this.IP = IP;
		this.port = port;
		this.name = "collector";
		this.curWorkspace = "./workspace/";
		buildWorkspace();

		StringBuffer buf = new StringBuffer("./workspace/");
		curWorkspace = buf.append(this.name).append("/").toString();
		buildWorkspace();

		url = "jdbc:sqlite:".concat(this.curWorkspace).concat(this.name).concat("-sqlite.db");
		createDB();
	}

	public void start() {
		try {
			bind();
		} catch (InterruptedException e) { e.printStackTrace(); }
	}

	public void stop() {
		try {
			// Shut down all event loops to terminate all threads.
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		} catch (Exception e) {
			e.printStackTrace();
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
			String sql = "CREATE TABLE IF NOT EXISTS transactionsTime (\n"
					// + " id integer PRIMARY KEY AUTOINCREMENT,\n"
					+ " digest text PRIMARY KEY NOT NULL,\n"
					+ " sender text NOT NULL,\n"
					+ " recipient text NOT NULL,\n"
					+ " timestamp integer NOT NULL,\n"
					+ " gasPrice real NOT NULL,\n"
					+ " accountNonce integer NOT NULL,\n"
					+ " value real NOT NULL,\n"
					+ " sendTime integer,\n"
					+ " commitTime integer\n"
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
	
	public void buildWorkspace() {
		File dir = new File(this.curWorkspace);
		if (dir.exists()) {
			 System.out.println("Dir OK");
		}
		else if (dir.mkdirs()) {
	        System.out.println("创建目录" + curWorkspace + "成功！");
        } else {
            System.out.println("创建目录" + curWorkspace + "失败！");
        }
		logger = Logger.getLogger(this.name);
		logger.removeAllAppenders(); 
		try {
			Layout layout = new PatternLayout("%-d{yyyy-MM-dd HH:mm:ss} [ %l:%r ms ] %n[%p] %m%n");
			FileAppender appender = new FileAppender(layout, this.curWorkspace.concat(this.name).concat(".log"));
			appender.setAppend(false);
			logger.setLevel(LOGLEVEL);
			logger.setAdditivity(false); 
			appender.activateOptions(); 
			logger.addAppender(appender);
			logger.info("Create log file ".concat(this.name));
		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}
	}

	/**
	 * 插入数据库
	 * @param tmsg 交易时间信息
	 */
	public void txMemory(TimeMsg tmsg) {
		Connection conn = this.connect();
		String recordSend = "INSERT INTO transactionsTime(sender, recipient, value, timestamp, gasPrice, accountNonce, digest, sendTime) VALUES(?,?,?,?,?,?,?,?)";
		String recordCommit = "INSERT INTO transactionsTime(sender, recipient, value, timestamp, gasPrice, accountNonce, digest, commitTime) VALUES(?,?,?,?,?,?,?,?)";
		String updateCommit = "update transactionsTime set commitTime=? where digest=?";
		String queryExist = "SELECT digest FROM transactionsTime where digest = ?";
		JSONArray txs = tmsg.getTxs();

		String tag = tmsg.getTag();
		Long time = tmsg.getTime();

		switch (tag) {
			case TimeMsg.SendTag: {
				for (int i = 0; i < txs.size(); i ++) {
					Transaction tx = new Transaction(txs.get(i).toString());
					try {
						PreparedStatement pstmt = conn.prepareStatement(queryExist);
						pstmt.setString(1, tx.getDigest());
						ResultSet rs = pstmt.executeQuery();
						if (rs.next()) {
							System.out.println("这个交易已经被 发 过一次");
							continue;
						}
						pstmt = conn.prepareStatement(recordSend);
						pstmt.setString(1, tx.getSender());
						pstmt.setString(2, tx.getRecipient());
						pstmt.setDouble(3, tx.getValue());
						pstmt.setLong(4, tx.getTimestamp());
						pstmt.setDouble(5, tx.getGasPrice());
						pstmt.setLong(6, tx.getAccountNonce());
						pstmt.setString(7, tx.getDigest());
						pstmt.setLong(8, time);
						pstmt.executeUpdate();
						logger.info("send记录中...");
					} catch (SQLException e) {
						System.out.println(e.getMessage());
					}
				}
				break;
			}
			case TimeMsg.CommitTag: {
				for (int i = 0; i < txs.size(); i ++) {
					Transaction tx = new Transaction(txs.get(i).toString());
					try {
						PreparedStatement pstmt = conn.prepareStatement(queryExist);
						pstmt.setString(1, tx.getDigest());
						ResultSet rs = pstmt.executeQuery();
						if (rs.next()) {
							System.out.println("这个交易被记录了");
							pstmt = conn.prepareStatement(updateCommit);
							pstmt.setLong(1, time);
							pstmt.setString(2, tx.getDigest());
							pstmt.executeUpdate();
						}
						else {
							System.out.println("这个交易还没有被记录过");
							pstmt = conn.prepareStatement(recordCommit);
							pstmt.setString(1, tx.getSender());
							pstmt.setString(2, tx.getRecipient());
							pstmt.setDouble(3, tx.getValue());
							pstmt.setLong(4, tx.getTimestamp());
							pstmt.setDouble(5, tx.getGasPrice());
							pstmt.setLong(6, tx.getAccountNonce());
							pstmt.setString(7, tx.getDigest());
							pstmt.setLong(8, time);
							pstmt.executeUpdate();
						}
						logger.info("commit记录中...");
					} catch (SQLException e) {
						System.out.println(e.getMessage());
					}
				}
				break;
			}
			default:
				System.out.println("收到了错误的TimeMsg，这里是txMemory");
		}
	}

	/**
	 * Server开启的核心代码。
	 * 其中 NettyServerHandler是 Server “接收消息”的代码。
	 * @throws InterruptedException
	 */
	private void bind() throws InterruptedException {
		boss=new NioEventLoopGroup();
		worker=new NioEventLoopGroup();
		ServerBootstrap bootstrap=new ServerBootstrap();
		bootstrap.group(boss,worker);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.option(ChannelOption.SO_BACKLOG, 128);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel socketChannel) throws Exception {
				ChannelPipeline p = socketChannel.pipeline();
				p.addLast(new ObjectEncoder());
				p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
				p.addLast(new CollectorServerHandler(Collector.this));
			}
		});
		ChannelFuture f= bootstrap.bind(this.port).sync();
		if(f.isSuccess()){
			System.out.println("Collector server start---------------");
			System.out.println(String.format("%s listen in port %d, IP %s", this.name, this.port, this.IP));
		}
	}

	public static void main(String[] args) throws InterruptedException {

		String curIP = Utils.getPublicIp();
		System.out.println("Local HostAddress "+curIP);   // ip
		Collector collector = new Collector(curIP, Collector.COLLECTOR_PORT);
		collector.start();
	}
	
};
