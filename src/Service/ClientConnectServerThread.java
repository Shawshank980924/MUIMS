package Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class ClientConnectServerThread extends Thread{
    Socket socket;
    public ClientConnectServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while(true){
            System.out.println("客户端正在等待服务端传来信息");
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
