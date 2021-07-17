package pbftSimulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

		switch (config.env) {
			case "dev": {
				shardNode[] systemReplicas = new shardNode[config.SHARDNUM*config.SHARDNODENUM];
				Iterator<String> shardKeys = topos.keySet().iterator();
				int ind = 0;
				while (shardKeys.hasNext()) {
					String curShardID = shardKeys.next();
					for (int id = 0; id < topos.get(curShardID).size(); id ++) {
						PairAddress curPair = topos.get(curShardID).get(id);
						systemReplicas[ind++] = new shardNode(curShardID, id, curPair.getIP(), curPair.getPort());
					}
				}
				break;
			}
			case "prod": {
				// 开始获取自己在topos中是第几个。
				int currentID = 0;
				Iterator<String> shardKeys = topos.keySet().iterator();
				String thisMachineShardID = "0";
				boolean ready = false;
				while (shardKeys.hasNext()) {
					String currentShardID = shardKeys.next();
					ArrayList<PairAddress> curIPs = topos.get(currentShardID);
					for (int j = 0; j < curIPs.size(); j ++) {
						if (curIPs.get(j).getIP().equals(curIP)) {
							currentID = curIPs.get(j).getId();
							thisMachineShardID = currentShardID;
							ready = true;
							break;
						}
					}
					if (ready) break;
				}

				if (!ready) {
					System.out.println("Error!! this machine is not registered. shardID: "+thisMachineShardID+", curID:"+currentID);
				}

				System.out.println(String.format("这是第 %s 个分片的 %d 号节点", thisMachineShardID, currentID));
				ArrayList<PairAddress> curShardIPs = topos.get(thisMachineShardID);
				PairAddress curPair = topos.get(thisMachineShardID).get(currentID);
				shardNode currentReplica = new shardNode(thisMachineShardID, currentID, curPair.getIP(), curPair.getPort());
				break;
			}
			default:
				System.out.println("Wrong env config, check it.");
		}

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
			String title[] = reader.readLine().split(",");    // 文件头，用来做 json 的索引
			for (int i = 0; i < title.length; i ++) title[i] = title[i].trim();

			String line = null;
			while((line=reader.readLine())!=null){
				JsonObject jstmp = new JsonObject();
				JsonObject jsin = new JsonObject();
				String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				for (int i = 0; i < item.length; i ++) {
					item[i] = item[i].trim();
					jstmp.addProperty(title[i], item[i].trim());
				}
				jsin.addProperty("sender", jstmp.get("sender").getAsString());
				jsin.addProperty("recipient", jstmp.get("recipient").getAsString());
				jsin.addProperty("value", Double.parseDouble(jstmp.get("value").getAsString()));
				jsin.addProperty("Monoxide_d1", Double.valueOf(jstmp.get("Monoxide_d1").getAsString()).intValue());
				jsin.addProperty("Monoxide_d2", Double.valueOf(jstmp.get("Monoxide_d2").getAsString()).intValue());
				jsin.addProperty("Metis_d1", Double.valueOf(jstmp.get("Metis_d1").getAsString()).intValue());
				jsin.addProperty("Metis_d2", Double.valueOf(jstmp.get("Metis_d2").getAsString()).intValue());
				jsin.addProperty("Proposed_d1", Double.valueOf(jstmp.get("Proposed_d1").getAsString()).intValue());
				jsin.addProperty("Proposed_d2", Double.valueOf(jstmp.get("Proposed_d2").getAsString()).intValue());
				jsin.addProperty("timestamp", System.currentTimeMillis());
				jsin.addProperty("gasPrice", 0);
				jsin.addProperty("accountNonce", 0);
				Transaction innerTx = new Gson().fromJson(jsin,Transaction.class);
				result.add(innerTx);
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
