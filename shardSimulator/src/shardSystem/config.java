package shardSystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Level;
import pbftSimulator.PairAddress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class config {
    public static String name;
    public static String env;
    public static String dataPath;
    public static String consensus_protocol;
    public static int RN;
    public static int SHARDNODENUM;
    public static int SHARDNUM;
    public static int FN;
    public static int CN;
    public static int INFLIGHT;
    public static int REQNUM;
    public static int TIMEOUT;
    public static int CLITIMEOUT;
    public static int BASEDLYBTWRP;
    public static int DLYRNGBTWRP;
    public static int BASEDLYBTWRPANDCLI;
    public static int DLYRNGBTWRPANDCLI;
    public static int BANDWIDTH;
    public static double FACTOR;
    public static int COLLAPSEDELAY;
    public static boolean SHOWDETAILINFO;
    public static int BLOCKTXNUM;
    public static int SLICENUM;
    public static int REQTXSIZE;
    public static int BLOCK_GENERATION_TIME;
    public static int REPLICA_PORT;
    public static int PBFTSEALER_PORT;
    public static int COLLECTOR_PORT;
    public static String COLLECTOR_IP;
    public static String sharding_rule;
    public static boolean RELAY_TX_FORWARD;
    public static int MESSAGE_SIZE;

    public static Map<String, ArrayList<PairAddress>> topos;
    public static Map<String, String> addrShard;
    public static final Level LOGLEVEL = Level.DEBUG;
    public static Map<String, Integer> PBFTSealer_ports;

    public static void Init(String configPath) {
        String content = "{}";
        try {
            File file=new File(configPath);
            content = new String(Files.readAllBytes(Paths.get(configPath)));

            JsonObject jsonObject = new JsonParser().parse(content).getAsJsonObject();
            name = jsonObject.get("name").getAsString();
            env = jsonObject.get("env").getAsString();
            dataPath = jsonObject.get("dataPath").getAsString();
            consensus_protocol = jsonObject.get("consensus_protocol").getAsString();
            RN = jsonObject.get("RN").getAsInt();
            SHARDNODENUM = jsonObject.get("SHARDNODENUM").getAsInt();
            SHARDNUM = jsonObject.get("SHARDNUM").getAsInt();
            FN = jsonObject.get("FN").getAsInt();
            CN = jsonObject.get("CN").getAsInt();
            INFLIGHT = jsonObject.get("INFLIGHT").getAsInt();
            REQNUM = jsonObject.get("REQNUM").getAsInt();
            TIMEOUT = jsonObject.get("TIMEOUT").getAsInt();
            CLITIMEOUT = jsonObject.get("CLITIMEOUT").getAsInt();
            BASEDLYBTWRP = jsonObject.get("BASEDLYBTWRP").getAsInt();
            DLYRNGBTWRP = jsonObject.get("DLYRNGBTWRP").getAsInt();
            BASEDLYBTWRPANDCLI = jsonObject.get("BASEDLYBTWRPANDCLI").getAsInt();
            DLYRNGBTWRPANDCLI = jsonObject.get("DLYRNGBTWRPANDCLI").getAsInt();
            BANDWIDTH = jsonObject.get("BANDWIDTH").getAsInt();
            FACTOR = jsonObject.get("FACTOR").getAsDouble();
            COLLAPSEDELAY = jsonObject.get("COLLAPSEDELAY").getAsInt();
            SHOWDETAILINFO = jsonObject.get("SHOWDETAILINFO").getAsBoolean();
            BLOCKTXNUM = jsonObject.get("BLOCKTXNUM").getAsInt();
            SLICENUM = jsonObject.get("SLICENUM").getAsInt();
            REQTXSIZE = jsonObject.get("REQTXSIZE").getAsInt();
            BLOCK_GENERATION_TIME = jsonObject.get("BLOCK_GENERATION_TIME").getAsInt();
            REPLICA_PORT = jsonObject.get("REPLICA_PORT").getAsInt();
            PBFTSEALER_PORT = jsonObject.get("PBFTSEALER_PORT").getAsInt();
            COLLECTOR_PORT = jsonObject.get("COLLECTOR_PORT").getAsInt();
            COLLECTOR_IP = jsonObject.get("COLLECTOR_IP").getAsString();
            sharding_rule = jsonObject.get("sharding_rule").getAsString();
            RELAY_TX_FORWARD = jsonObject.get("RELAY_TX_FORWARD").getAsBoolean();
            MESSAGE_SIZE = jsonObject.get("MESSAGE_SIZE").getAsInt();


            JsonObject jsonTopo = jsonObject.get("topo").getAsJsonObject();
            topos  = new HashMap<>();
            int topoSize, topoEach;
            String keys[] = jsonTopo.keySet().toArray(new String[SHARDNUM]);
            topoSize = jsonTopo.size();
            topoEach = jsonTopo.get(keys[0]).getAsJsonArray().size();

            assert(topoSize * topoEach == SHARDNODENUM * SHARDNUM);

            Iterator<String> topoKeys = jsonTopo.keySet().iterator();
            while (topoKeys.hasNext()) {
                String shardID = topoKeys.next();
                JsonArray jsonShard = jsonTopo.get(shardID).getAsJsonArray();
                topos.put(shardID, new ArrayList<PairAddress>());
                for (int j = 0; j < SHARDNODENUM; j ++) {
                    JsonObject shardJ = jsonShard.get(j).getAsJsonObject();
                    topos.get(shardID).add(new PairAddress(j, shardJ.get("IP").getAsString(), shardJ.get("port").getAsInt()));
                }
            }

            JsonObject jsonAddr = jsonObject.get("addrShard").getAsJsonObject();
            addrShard  = new HashMap<> ();
            Iterator<String> addrKeys = jsonAddr.keySet().iterator();
            while (addrKeys.hasNext()) {
                String shardID = addrKeys.next();
                JsonArray jsonShard = jsonAddr.get(shardID).getAsJsonArray();
                for (int j = 0; j < jsonShard.size(); j ++) {
                    String shardJ = jsonShard.get(j).getAsString();
                    addrShard.put(shardJ, shardID);
                }
            }

            if (env.equals("dev")) {
                JsonObject PBFTSealers = jsonObject.get("PBFTSealers").getAsJsonObject();
                PBFTSealer_ports = new HashMap<> ();
                Iterator<String> sealerKeys = PBFTSealers.keySet().iterator();
                while (sealerKeys.hasNext()) {
                    String shardID = sealerKeys.next();
                    PBFTSealer_ports.put(shardID, PBFTSealers.get(shardID).getAsInt());
                }
            }

        } catch (IOException e) {
            System.out.println("Read config file Error");
            return;
        } catch (Exception e) {
            throw e;
        }
    }

    public static String Print() {
        String jsbuf = name + " | "+env+" | "+dataPath+" | "+consensus_protocol+" | "+
                sharding_rule + " | " +
                String.valueOf(RN)+" | "+
                String.valueOf(SHARDNODENUM)+" | "+
                String.valueOf(SHARDNUM)+" | "+
                String.valueOf(FN)+" | "+
                String.valueOf(CN)+" | "+
                String.valueOf(INFLIGHT)+" | "+
                String.valueOf(REQNUM)+" | "+
                String.valueOf(TIMEOUT)+" | "+
                String.valueOf(CLITIMEOUT)+" | "+
                String.valueOf(BASEDLYBTWRP)+" | "+
                String.valueOf(DLYRNGBTWRP)+" | "+
                String.valueOf(BASEDLYBTWRPANDCLI)+" | "+
                String.valueOf(DLYRNGBTWRPANDCLI)+" | "+
                String.valueOf(BANDWIDTH)+" | "+
                String.valueOf(FACTOR)+" | "+
                String.valueOf(COLLAPSEDELAY)+" | "+
                String.valueOf(SHOWDETAILINFO)+" | "+
                String.valueOf(BLOCKTXNUM)+" | "+
                String.valueOf(SLICENUM)+" | "+
                String.valueOf(REQTXSIZE)+" | "+
                String.valueOf(BLOCK_GENERATION_TIME)+" | "+
                String.valueOf(REPLICA_PORT)+" | "+
                String.valueOf(MESSAGE_SIZE) + " | " +
                String.valueOf(PBFTSEALER_PORT) + "\n" +
                topos.toString() + "\n";
                //addrShard.toString();
        if (env.equals("dev")) {
            jsbuf = jsbuf + "\n" +
                    PBFTSealer_ports.toString();
        }
        return jsbuf;
    }

//    static {
//        Init("shardSimulator/src/config-dev.json");
//    }

    public static void reInit(String configPath) {
        Init(configPath);
    }

    public static void main(String[] args) {
        String configPath = "shardSimulator/src/config-prod.json";
        config.reInit(configPath);
        System.out.println(config.Print());
    }
}
