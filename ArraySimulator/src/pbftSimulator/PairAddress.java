package pbftSimulator;

public class PairAddress {
	
	private int id;
	private String IP;
	private int port;
	
	public PairAddress() {
		// TODO Auto-generated constructor stub
		id = 0;
		IP = "127.0.0.1";
		port = 0;
	}
	
	/**
	 * 
	 * @param IP
	 * @param port
	 */
	public PairAddress(int id, String IP, int port) {
		this.id = id;
		this.IP = IP;
		this.port = port;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
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

	
	public String encoder() {
		String result = String.format("(%d,%s,%d)", this.id, this.IP, this.port);
		return result;
	}

	public String toString() {
		return encoder();
	}

}
