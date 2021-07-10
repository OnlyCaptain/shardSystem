package pbftSimulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import shardSystem.config;
import shardSystem.shardNode;
import shardSystem.transaction.Transaction;

public class Simulator {

	public static void main(String[] args) {
		// 第一个参数是配置文件
		String configJsonFileName = args[0];
		try {
			config.reInit(configJsonFileName);
		} catch (Exception e) {
			System.out.println("配置文件读取失败，请检查:"+configJsonFileName);
			return;
		}

		Map<String, ArrayList<PairAddress>> topos  = config.topos;
		Map<String, String> addrShard = config.addrShard;
		String curIP = Utils.getPublicIp();
		System.out.println("Local HostAddress "+curIP);   // ip
		System.out.println("config show: " + config.Print());

		// 开始获取自己在topos中是第几个。
		int currentID = 0;
		String [] shardLists = topos.keySet().toArray(new String[topos.size()]);
		String currentShardID = shardLists[0];
		boolean ready = false;
		for (int i = 0; i < shardLists.length; i ++) {
			ArrayList<PairAddress> curIPs = topos.get(shardLists[i]);
			for (int j = 0; j < curIPs.size(); j ++) {
				if (curIPs.get(j).getIP().equals(curIP)) {
					currentID = curIPs.get(j).getId();
					currentShardID = shardLists[i];
					ready = true;
					break;
				}
			}
			if (ready)
				break;
		}
		if (!ready) {
			System.out.println("Error!!"+currentShardID+" "+currentID);
		}
		System.out.println(String.format("这是第 %s 个分片的 %d 号节点", currentShardID, currentID));
		ArrayList<PairAddress> curShardIPs = topos.get(currentShardID);
		shardNode currentReplica = new shardNode(currentShardID, currentID, curShardIPs.get(currentID).getIP(), curShardIPs.get(currentID).getPort(), topos, addrShard);
	}

	/**
	 * 返回系统时间戳，按秒计
	 * @return	返回系统时间戳
	 */
	public static long getTimeStamp() {
		return System.currentTimeMillis();
	}

	public static ArrayList<Transaction> getTxsFromFile(String filepath) {
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			reader.readLine();//第一行信息，为标题信息，不用，如果需要，注释掉
			String line = null; 
			while((line=reader.readLine())!=null){ 
				String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				// String last = item[item.length-1];//这就是你要的数据了
				result.add(new Transaction(item[0], item[1], Double.parseDouble(item[2]), null, Long.parseLong(item[3]), Double.parseDouble(item[4]), 0));
				//int value = Integer.parseInt(last);//如果是数值，可以转化为数值
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 生成IP数组，后续需要改成从配置文件中读取
	 * @param n
	 * @return IP地址数组
	 */
	public static String[] netIPsInit(int n) {
		String[] result = new String[n];
		for (int i = 0; i < n; i ++) 
			result[i] = "127.0.0.1";
		return result;
	}

	public static int[] netPortsInit(int n) {
		Set<Integer> set = new HashSet<Integer>();
		int L=49152, H = 65535, p = 0, i = 0;
		Random rand = new Random(n);
		while (i < n) {
			p = L + rand.nextInt(H-L);
			if (set.contains(p)) 
				continue;
			if (Utils.isPortUsed(p)) 
				continue;
			set.add(p);
			i ++;
		}
		Integer[] temp = set.toArray(new Integer[] {});//关键语句
 		int[] result = new int[temp.length];
		for (int j = 0; j < temp.length; j++) {
			result[j] = temp[j].intValue();
		}
		return result;
	}
	
	/**
	 * 随机初始化replicas节点的拜占庭标签
	 * @param n	节点数量
	 * @param f	拜占庭节点数量
	 * @return	返回拜占庭标签数组（true为拜占庭节点，false为诚实节点）
	 */
	public static boolean[] byztDistriInit(int n, int f) {
		boolean[] byzt = new boolean[n];
		Random rand = new Random(111);
		while(f > 0) {
			int i = rand.nextInt(n);
			if(!byzt[i]) {
				byzt[i] = true;
				--f;
			}
		}
		return byzt;
	}
}
