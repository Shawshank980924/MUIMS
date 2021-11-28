package Service;

import common.Message;
import common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;

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

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void run() {
        while(true){

            try {
                System.out.println("服务端与"+userId+"保持通信，持续监听");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message)ois.readObject();
                //拉取在线列表
                if(message.getMessageType().equals(MessageType.MESSAGE_GET_USER_LIST)){
                    System.out.println("服务端接收到客户端"+message.getSender()+"请求拉取在线用户列表");
                    String str = "";

                    for (String s : ServerThreadManage.threads.keySet()) {
                        str+=s+" ";
                    }
                    //将在线用户集合str包装在message_back中发回给客户端
                    Message message_back = new Message();
                    message_back.setReceiver(message.getSender());
                    message_back.setContent(str);
                    message_back.setMessageType(MessageType.MESSAGE_GET_USER_LIST);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message_back);
                }
                else if(message.getMessageType().equals(MessageType.MESSAGE_CLIENT_QUIT)){
                    //回送客户端退出消息给客户端
                    System.out.println("客户端"+message.getSender()+"申请退出");
                    Message message_back = new Message();
                    message_back.setMessageType(MessageType.MESSAGE_CLIENT_QUIT);
                    message_back.setReceiver(message.getSender());
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message_back);
                    //服务端线程退出,socket关闭，线程池删除该线程
                    System.out.println("服务端"+message.getSender()+"socket关闭");
                    ServerThreadManage.deleteServerThread(message.getSender());
                    socket.close();
                    oos.close();
                    break;
                }
                else if(message.getMessageType().equals(MessageType.MESSAGE_PRIVATE_COMMON)){
                    //转发信息到目标用户
                    System.out.println(message.getSender()+"请求和"+message.getReceiver()+"聊天，服务器转发消息");
                    if(ServerThreadManage.getServerConnectThread(message.getReceiver())!=null){
                        //用户在线上，直接转发
                        ObjectOutputStream oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(message.getReceiver()).getSocket().getOutputStream());

                        oos.writeObject(message);
                    }
                    else{
                        //若还未上线，先存放在OfflineMessages 中
                        System.out.println("用户"+message.getReceiver()+"还未上线，暂存在服务器端");
                        OfflineMessageManage.addOfflineMessage(message.getReceiver(),message);
                    }
//                    //扩充功能可以给离线用户留言
//                        System.out.println(message.getReceiver()+"用户现在不在线");
//                        //用户不在，先开一个线程等待用户上线
//                        Runnable waitOnline = new Runnable() {
//                            @Override
//                            public void run() {
//                                //每隔一段时间确认对方是否上线
//                                while(ServerThreadManage.getServerConnectThread(message.getReceiver())==null){
//                                    try {
//                                        Thread.sleep(1000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                //上线则发送消息
//                                ObjectOutputStream oos = null;
//                                try {
//                                    oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(message.getReceiver()).getSocket().getOutputStream());
//                                    oos.writeObject(message);
//                                    System.out.println(message.getReceiver()+"用户已经上线，"+message.getSender()+"的留言已经成功发送给目标用户");
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        };
//                        //开启子线程准备用于将消息再用户在线时转发给对方
//                        new Thread(waitOnline).start();
                }
                else if(message.getMessageType().equals(MessageType.MESSAGE_PUBLIC_COMMON)){
                    System.out.println(message.getSender()+"申请和"+message.getReceiver()+"发送消息");
                    ObjectOutputStream oos ;
                    for (String s : ServerThreadManage.threads.keySet()) {
                        //碰到自己的线程跳过
                        if(s.equals(message.getSender()))continue;
                        oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(s).socket.getOutputStream());
                        oos.writeObject(message);
                    }


                }
                else if(message.getMessageType().equals(MessageType.MESSAGE_FILE)){
                    System.out.println(message.getSender()+"请求向"+message.getReceiver()+"发送文件");
                    //扩充功能可以给离线用户发文件
                    System.out.println(message.getReceiver()+"用户现在不在线");
                    //用户不在，先开一个线程等待用户上线
                    Runnable waitOnline = new Runnable() {
                        @Override
                        public void run() {
                            //每隔一段时间确认对方是否上线
                            while(ServerThreadManage.getServerConnectThread(message.getReceiver())==null){
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //上线则发送文件消息
                            ObjectOutputStream oos = null;
                            try {
                                oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(message.getReceiver()).getSocket().getOutputStream());
                                oos.writeObject(message);
                                System.out.println(message.getReceiver()+"用户已经上线，"+message.getSender()+"的文件已经成功发送给目标用户");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    //开启子线程准备用于将消息再用户在线时转发给对方
                    new Thread(waitOnline).start();
//                    if(ServerThreadManage.getServerConnectThread(message.getReceiver())!=null){
//                        //若该用户在线的话直接转发即可
//                        ObjectOutputStream oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(message.getReceiver()).socket.getOutputStream());
//                        oos.writeObject(message);
//                    }
//                    else{
//                        System.out.println("用户"+message.getReceiver()+"不在线，无法发送");
//                    }
                }
                else{
                    //
                    System.out.println("其他类型暂时不处理");
                }


            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
