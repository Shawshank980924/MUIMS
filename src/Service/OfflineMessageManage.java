package Service;

import common.Message;
import javafx.beans.binding.MapExpression;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public interface OfflineMessageManage {
    static ConcurrentHashMap<String, Message> offlineMessages = new ConcurrentHashMap<>();
    public static void addOfflineMessage(String receiverId,Message message){
        offlineMessages.put(receiverId,message);
    }
    static void deleteMessage(String receiverId){
        offlineMessages.remove(receiverId);
    }
    static Message getMessage(String receiverId){
        return
                offlineMessages.get(receiverId);
    }
}
