package Service;

import java.util.HashMap;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class ClientThreadManage {
    public static HashMap<String,ClientConnectServerThread> threads = new HashMap<>();
    public static void addThread(String userId,ClientConnectServerThread thread){
        threads.put(userId,thread);
    }
    public static ClientConnectServerThread getThread(String userId){
        return threads.get(userId);
    }

}
