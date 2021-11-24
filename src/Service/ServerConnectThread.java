package Service;

import common.Message;
import common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class ServerConnectThread extends Thread{
    private Socket socket ;
    private String userId;
    public ServerConnectThread(Socket socket,String userId) {
        this.socket = socket;
        this.userId = userId;

    }

    @Override
    public void run() {
        while(true){
            try {
                System.out.println("服务端与"+userId+"保持通信，持续监听");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message)ois.readObject();
                //消息的处理暂时省略

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
