package pbftSimulator;

public class PairAddress {
	
	private String IP;
	private int port;
	private int id;
	
	public PairAddress() {
		// TODO Auto-generated constructor stub
		IP = "127.0.0.1";
		port = 0;
		id = 0;
	}
	
	/**
	 * 
	 * @param IP
	 * @param port
	 */
	public PairAddress(String IP, int port, int id) {
		this.IP = IP;
		this.port = port;
		this.id = id;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
