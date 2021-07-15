package shardSystem;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import pbftSimulator.PairAddress;

import java.io.File;
import java.io.IOException;
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

    public static Map<String, ArrayList<PairAddress>> topos;
    public static Map<String, String> addrShard;
    public static final Level LOGLEVEL = Level.DEBUG;
    public static Map<String, Integer> PBFTSealer_ports;

    public static void Init(String configPath) {
        String content = "{}";
        try {
            File file=new File(configPath);
            content = FileUtils.readFileToString(file,"UTF-8");

            JSONObject jsonObject = JSONObject.fromObject(content);
            name = jsonObject.getString("name");
            env = jsonObject.getString("env");
            dataPath = jsonObject.getString("dataPath");
            consensus_protocol = jsonObject.getString("consensus_protocol");
            RN = jsonObject.getInt("RN");
            SHARDNODENUM = jsonObject.getInt("SHARDNODENUM");
            SHARDNUM = jsonObject.getInt("SHARDNUM");
            FN = jsonObject.getInt("FN");
            CN = jsonObject.getInt("CN");
            INFLIGHT = jsonObject.getInt("INFLIGHT");
            REQNUM = jsonObject.getInt("REQNUM");
            TIMEOUT = jsonObject.getInt("TIMEOUT");
            CLITIMEOUT = jsonObject.getInt("CLITIMEOUT");
            BASEDLYBTWRP = jsonObject.getInt("BASEDLYBTWRP");
            DLYRNGBTWRP = jsonObject.getInt("DLYRNGBTWRP");
            BASEDLYBTWRPANDCLI = jsonObject.getInt("BASEDLYBTWRPANDCLI");
            DLYRNGBTWRPANDCLI = jsonObject.getInt("DLYRNGBTWRPANDCLI");
            BANDWIDTH = jsonObject.getInt("BANDWIDTH");
            FACTOR = jsonObject.getDouble("FACTOR");
            COLLAPSEDELAY = jsonObject.getInt("COLLAPSEDELAY");
            SHOWDETAILINFO = jsonObject.getBoolean("SHOWDETAILINFO");
            BLOCKTXNUM = jsonObject.getInt("BLOCKTXNUM");
            SLICENUM = jsonObject.getInt("SLICENUM");
            REQTXSIZE = jsonObject.getInt("REQTXSIZE");
            BLOCK_GENERATION_TIME = jsonObject.getInt("BLOCK_GENERATION_TIME");
            REPLICA_PORT = jsonObject.getInt("REPLICA_PORT");
            PBFTSEALER_PORT = jsonObject.getInt("PBFTSEALER_PORT");
            COLLECTOR_PORT = jsonObject.getInt("COLLECTOR_PORT");
            COLLECTOR_IP = jsonObject.getString("COLLECTOR_IP");
            sharding_rule = jsonObject.getString("sharding_rule");
            RELAY_TX_FORWARD = jsonObject.getBoolean("RELAY_TX_FORWARD");

            JSONObject jsonTopo = jsonObject.getJSONObject("topo");
            topos  = new HashMap<>();
            int topoSize, topoEach;
            topoSize = jsonTopo.size();
            topoEach = jsonTopo.getJSONArray(jsonTopo.keys().next().toString()).size();

            assert(topoSize * topoEach == SHARDNODENUM * SHARDNUM);

            Iterator<String> topoKeys = jsonTopo.keys();
            while (topoKeys.hasNext()) {
                String shardID = topoKeys.next();
                JSONArray jsonShard = jsonTopo.getJSONArray(shardID);
                topos.put(shardID, new ArrayList<PairAddress>());
                for (int j = 0; j < SHARDNODENUM; j ++) {
                    JSONObject shardJ = jsonShard.getJSONObject(j);
                    topos.get(shardID).add(new PairAddress(j, shardJ.getString("IP"), shardJ.getInt("port")));
                }
            }

            JSONObject jsonAddr = jsonObject.getJSONObject("addrShard");
            addrShard  = new HashMap<> ();
            Iterator<String> addrKeys = jsonAddr.keys();
            while (addrKeys.hasNext()) {
                String shardID = addrKeys.next();
                JSONArray jsonShard = jsonAddr.getJSONArray(shardID);
                for (int j = 0; j < jsonShard.size(); j ++) {
                    String shardJ = jsonShard.getString(j);
                    addrShard.put(shardJ, shardID);
                }
            }

            if (env.equals("dev")) {
                JSONObject PBFTSealers = jsonObject.getJSONObject("PBFTSealers");
                PBFTSealer_ports = new HashMap<> ();
                Iterator<String> sealerKeys = PBFTSealers.keys();
                while (sealerKeys.hasNext()) {
                    String shardID = sealerKeys.next();
                    PBFTSealer_ports.put(shardID, PBFTSealers.getInt(shardID));
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
                String.valueOf(PBFTSEALER_PORT) + "\n" +
                topos.toString() + "\n" +
                addrShard.toString();
        if (env.equals("dev")) {
            jsbuf = jsbuf + "\n" +
                    PBFTSealer_ports.toString();
        }
        return jsbuf;
    }

    static {
        Init("shardSimulator/src/config-dev.json");
    }

    public static void reInit(String configPath) {
        Init(configPath);
    }

    public static void main(String[] args) {
        String configPath = "shardSimulator/src/config-prod.json";
        config.reInit(configPath);
        System.out.println(config.Print());
    }
}
