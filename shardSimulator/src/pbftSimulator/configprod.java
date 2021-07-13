package pbftSimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;

//import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import pbftSimulator.message.Message;
import pbftSimulator.replica.Replica;
import shardSystem.ByztShardNode;
import shardSystem.OfflineShardNode;
import shardSystem.config;
import shardSystem.shardNode;
import shardSystem.transaction.Transaction;

public class configprod {
	
	public static final int RN = 7;  						//replicas节点的数量(rn)
	public static final int FN = 2;							//恶意节点的数量
	public static final int CN = 1;						//客户端数量
	public static final int INFLIGHT = 2000; 					//最多同时处理多少请求
	public static final int REQNUM = 1;					//请求消息总数量
	public static final int TIMEOUT = 500;					//节点超时设定(毫秒)
	public static final int CLITIMEOUT = 800;				//客户端超时设定(毫秒)
	public static final int BASEDLYBTWRP = 2;				//节点之间的基础网络时延
	public static final int DLYRNGBTWRP = 1;				//节点间的网络时延扰动范围
	public static final int BASEDLYBTWRPANDCLI = 10;		//节点与客户端之间的基础网络时延
	public static final int DLYRNGBTWRPANDCLI = 15;			//节点与客户端之间的网络时延扰动范围
	public static final int BANDWIDTH = 300000;			//节点间网络的额定带宽(bytes)(超过后时延呈指数级上升)
	public static final double FACTOR = 1.005;				//超出额定负载后的指数基数
	public static final int COLLAPSEDELAY = 10000;			//视为系统崩溃的网络时延
	public static final boolean SHOWDETAILINFO = true;		//是否显示完整的消息交互过程

	public static final int BLOCKTXNUM = 50;

	public static final int SHARDNUM = 1;
	public static final int SHARDNODENUM = 7;

	public static final Level LOGLEVEL = Level.DEBUG;
	public static final int REQTXSIZE = 50;


	
	
	public static void main(String[] args) {
		Map<String, ArrayList<PairAddress>> topos = new HashMap<> ();
		String filepath = "./shardSimulator/src/IPlists.csv";
		String filepath1 = "./shardSimulator/src/config-prod.json";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			reader.readLine();//第一行信息，为标题信息，不用，如果需要，注释掉
			String line = null; 
			int id = 0, p_i=0;
			int shardId = -1;
			while((line=reader.readLine())!=null){ 
				String item[] = line.split(",");
				String IP = item[0].replace("\"", "");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
				System.out.println(SHARDNODENUM);
				if (id % config.SHARDNODENUM == 0) {
					id = 0;
					shardId ++;
				}
				String SID = String.valueOf(shardId);
				if (!topos.keySet().contains(SID))
					topos.put(SID, new ArrayList<PairAddress>());
				topos.get(SID).add(new PairAddress(id++, IP, 60635));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}


		System.out.println(topos.toString());
		JSONObject js = JSONObject.fromObject(topos);
		System.out.println(js.toString());
		
		System.out.println(Utils.getPublicIp());

		
		
		try {
			createJsonFile(topos, filepath1);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	
	
	/**
	 * 将JSON数据格式化并保存到文件中
	 * @param jsonData 需要输出的json数
	 * @param filePath 输出的文件地址
	 * @return
	 */
	public static boolean createJsonFile(Object jsonData, String filePath) throws IOException {
		TypeUtils.compatibleWithFieldName = true;     //防止大写的首字母变小写
		String content = JSON.toJSONString(jsonData, SerializerFeature.PrettyFormat);    //格式化
	  // 标记文件生成是否成功
		File configprodjson = new File(filePath);
		String content1= FileUtils.readFileToString(configprodjson,"UTF-8");

		JSONObject jsonObject = JSONObject.fromObject(content1);
		jsonObject.put("topo",content);         //需要改的只有topo
		
		boolean flag = true;
	  // 生成json格式文件
		try {
	   // 保证创建一个新文件
			File file = new File(filePath);
			if (file.exists()) { // 如果已存在,删除旧文件
				file.delete();
			}
			file.createNewFile();
		   // 将格式化后的字符串写入文件
		   Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		   write.write(JSON.toJSONString(jsonObject));
		   write.flush();
		   write.close();
		} catch (Exception e) {
			flag = false;
		   e.printStackTrace();
		  }
		return flag;
	 }
	

}