package Service;

import common.Message;
import common.MessageType;
import common.User;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;


/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class UserClientService {
    //方便再方法中调用User以及Socket对象，所以将装两个属性写为私有属性
    private User user;
    private Socket socket;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean checkUser(String userId, String passwd) throws IOException, ClassNotFoundException {
        user = new User(userId,passwd);
        socket = new Socket(InetAddress.getLocalHost(),9999);
        boolean b=false;
        //对象处理流发送user对象到服务端
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(user);
        //从服务器端接收信息
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Message message = (Message) ois.readObject();
        String messageType = message.getMessageType();
        if(messageType.equals(MessageType.MESSAGE_LOGIN_SUCCESS)){
            //登录成功开启一个线程来持有这个socket持续监听来自服务端发来的信息
            ClientConnectServerThread clientConnectServerThread = new ClientConnectServerThread(socket);
            clientConnectServerThread.start();
            //将该线程加入Manage类的hashmap中进行管理
            ClientThreadManage.addThread(userId,clientConnectServerThread);
            b = true;
        }else{
            //登录失败需要关闭socket和输出流
            oos.close();
            socket.close();
        }
        return b;
    }
    public void getUserList(){
        Message message = new Message();
        message.setMessageType(MessageType.MESSAGE_GET_USER_LIST);
        message.setSender(user.getUserId());
        System.out.println(user.getUserId()+"向服务器发送请求拉取在线用户列表");
        try {
            ObjectOutputStream oos = new ObjectOutputStream(ClientThreadManage.getThread(user.getUserId()).getSocket().getOutputStream());
            oos.writeObject(message);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void quitClient(){
        Message message = new Message();
        message.setSender(user.getUserId());
        message.setMessageType(MessageType.MESSAGE_CLIENT_QUIT);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
