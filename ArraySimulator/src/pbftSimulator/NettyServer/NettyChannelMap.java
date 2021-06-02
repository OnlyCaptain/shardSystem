package pbftSimulator.NettyServer;

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NettyChannelMap {
    private static Map<String, SocketChannel> map=new ConcurrentHashMap<String, SocketChannel>();
    public static void add(String clientId,SocketChannel socketChannel){
        map.put(clientId,socketChannel);
    }
    public static Channel get(String clientId){
       return map.get(clientId);
    }
    public static void remove(SocketChannel socketChannel){
        for (Map.Entry entry:map.entrySet()){
            if (entry.getValue()==socketChannel){
                map.remove(entry.getKey());
            }
        }
    }
    public static void ptintAll(){
        for (Map.Entry entry:map.entrySet()){
            map.remove(entry.getKey());
            System.out.println(entry.getKey());
            System.out.println(map.get(entry.getKey()));

        }
    }

}
