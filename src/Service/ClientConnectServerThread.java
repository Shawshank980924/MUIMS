package Service;

import common.Message;
import common.MessageType;

import java.io.*;
import java.net.Socket;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class ClientConnectServerThread extends Thread{
    private Socket socket;
    public ClientConnectServerThread(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while(true){
            System.out.println("客户端正在等待服务端传来信息");
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    Message message_back = (Message) ois.readObject();
                    if(message_back.getMessageType().equals(MessageType.MESSAGE_GET_USER_LIST)){
                        String[] onlineUsers = message_back.getContent().split(" ");
                        System.out.println("\n========显示在线列表=======");
                        for (int i = 0; i < onlineUsers.length; i++) {
                            System.out.println(i+1+" "+onlineUsers[i]);
                        }
                    }
                    else if(message_back.getMessageType().equals(MessageType.MESSAGE_CLIENT_QUIT)){
                        System.out.println(message_back.getReceiver()+"客户端下socket关闭");
                        ClientThreadManage.deleteThread(message_back.getReceiver());
                        socket.close();
                        ois.close();
                        break;
                    }
                    else if(message_back.getMessageType().equals(MessageType.MESSAGE_PRIVATE_COMMON)){
                        System.out.println(message_back.getSendTime());
                        System.out.println(message_back.getSender()+"对"+message_back.getReceiver()+"说："+message_back.getContent());
                    }
                    else if(message_back.getMessageType().equals(MessageType.MESSAGE_PUBLIC_COMMON)){
                    System.out.println(message_back.getSender()+"对"+message_back.getReceiver()+"说"+message_back.getContent());

                    }
                    else if(message_back.getMessageType().equals(MessageType.MESSAGE_FILE)){
                        //BufferedOutputStream用来写入目标地址
                        System.out.println("接收到"+message_back.getSender()+"发来的文件数据，文件保存在"+message_back.getFilePath());
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(message_back.getFilePath()));
                        bufferedOutputStream.write(message_back.getFileDate(),0,message_back.getByteLen());
                        //关闭文件输出流
                        bufferedOutputStream.close();
                    }
                    else if(message_back.getMessageType().equals(MessageType.MESSAGE_NEWS)){
                        System.out.println(message_back.getSendTime());
                        System.out.print("接收到服务端推送给"+message_back.getReceiver()+"的消息：");
                        System.out.println(message_back.getContent());
                    }
                    else{
                        System.out.println("其他情况");
                    }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
