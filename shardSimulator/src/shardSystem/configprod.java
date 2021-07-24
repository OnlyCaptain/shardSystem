package shardSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.log4j.Level;
import pbftSimulator.PairAddress;
import shardSystem.config;


public class configprod {

	public static void main(String[] args) throws Exception{
		config.reInit("/Users/zhanjianzhou/javaP/shardSystem/shardSimulator/src/config-prod.json");
		Map<String, ArrayList<PairAddress>> topos = new HashMap<> ();
		String filepath = "shardSimulator/src/IPlists.csv";
		String template = "shardSimulator/src/config-prod.json";
		String output = "shardSimulator/src/config.json";

		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();
		String content = new String(Files.readAllBytes(Paths.get(template)));
		JsonObject jsconfig = new JsonParser().parse(content).getAsJsonObject();
		ArrayList<String> IPs = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filepath));//换成你的文件名
			reader.readLine();//第一行信息，为标题信息，不用，如果需要，注释掉
			String line = null;
			while((line=reader.readLine())!=null){
				String item[] = line.split(",");
				String IP = item[0].replace("\"", "");//CSV格式文件为逗号分隔符文件，这里根据逗号切分

				IPs.add(IP);
			}
			int id = 0, shardId = -1;
			for (int i = 0; i < config.SHARDNODENUM*config.SHARDNUM; i ++) {
				if (id % config.SHARDNODENUM == 0) {
					id = 0;
					shardId ++;
				}
				String SID = String.valueOf(shardId);
				if (!topos.keySet().contains(SID))
					topos.put(SID, new ArrayList<PairAddress>());
				topos.get(SID).add(new PairAddress(id++, IPs.get(i), 60635));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(topos.toString());

//		jsconfig.remove("topo");
//		jsconfig.remove("COLLECTOR_IP");
		JsonObject jtop = new Gson().toJsonTree(topos).getAsJsonObject();
		jsconfig.add("topo", jtop);
		jsconfig.addProperty("COLLECTOR_IP", IPs.get(IPs.size()-1));

		System.out.println(jsconfig.toString());

		try (FileOutputStream fos = new FileOutputStream(output);
			 OutputStreamWriter isr = new OutputStreamWriter(fos,
					 StandardCharsets.UTF_8)) {

			gson.toJson(jsconfig, isr);
		}
	}
}
