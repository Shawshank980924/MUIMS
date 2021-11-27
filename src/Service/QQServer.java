package Service;



import common.Message;
import common.MessageType;
import common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 小羊Shaun
 * @version 1.0
 * 服务器，通过端口9999与客户端 进行连接
 */
@SuppressWarnings("all")
public class QQServer {
    private ServerSocket ss = null;
    //ConcurrentHashMap相比于HashMap是线程安全的
    private static ConcurrentHashMap<String ,User> userList = new ConcurrentHashMap<>();
    static {
        userList.put("张三",new User("张三","12345"));
        userList.put("李四",new User("李四","12345"));
        userList.put("王二麻子",new User("王二麻子","12345"));
        userList.put("小羊Shaun",new User("小羊Shaun","12345"));
    }
    public boolean checkUser(String userId,String passwd){
        User user = userList.get(userId);
        boolean b =false;
        if(user == null ){
            System.out.println("用户 "+userId+"不存在");
        }
        else{
            if(user.getPasswd().equals(passwd)){
                b = true;
            }
            else{
                System.out.println("用户 "+userId+"密码错误");
            }
        }
        return b;
    }


    public QQServer() {
        try {
            //服务器持续监听9999端口
            ss = new ServerSocket(9999);
            //需要处理多个客户端的请求，所以是循环接收监听
            while(true){
                System.out.println("服务器正在监听9999端口");
                Socket socket = ss.accept();//接收客户端的传来的socket
                User user=null;//用于放socket接收的User对象
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());//用于接收socket传输的对象
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());//用于给客户端回送消息
                Message message = new Message();//用于存放发回的消息主体内容

                try {
                    user = (User) ois.readObject();
                    message.setReceiver(user.getUserId());
                    message.setSender("服务端"+ InetAddress.getLocalHost());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if(checkUser(user.getUserId(), user.getPasswd())){
                    //账号密码符合要求则建立线程持有该socket
                    ServerConnectThread serverConnectThread = new ServerConnectThread(socket, user.getUserId());
                    ServerThreadManage.addServerConnectThread(user.getUserId(),serverConnectThread);
                    serverConnectThread.start();
                    //向客户端传输写入登录成功的信息

                    message.setMessageType(MessageType.MESSAGE_LOGIN_SUCCESS);
                    message.setContent("userId"+user.getUserId()+"登录成功");
                    oos.writeObject(message);



                }else{
                    //账号密码验证失败返回登录失败信息
                    System.out.println("userId: "+ user.getUserId()+" passwd: "+user.getPasswd()+"登陆失败");
                    message.setMessageType(MessageType.MESSAGE_LOGIN_FAIL);
                    message.setContent("userId"+user.getUserId()+"登录失败");
                    oos.writeObject(message);
                    socket.close();
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //若退出了while循环需要关闭服务器的seversocket
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
