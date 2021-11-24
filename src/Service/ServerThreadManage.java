package Service;

import java.util.HashMap;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public interface ServerThreadManage {
    public static HashMap<String,ServerConnectThread>threads = new HashMap<>();
    public static void addServerConnectThread(String userId,ServerConnectThread thread){
        threads.put(userId,thread);

    }
    public  static  ServerConnectThread getServerConnectThread(String userId){
        return threads.get(userId);
    }

}
