# 多用户即时通讯系统

## 需求分析

* 用户登录：通过账号和密码登录
* 拉取在线用户列表：客户端向 服务器发送请求，服务器将在线用户列表传回
* 私聊：指定用户后发送消息；发送文件，向指定用户发送本地路径下的文件到对方的电脑路径
* 群聊：向所有的在线用户发送消息
* 无异常退出：区别于单机版，客户端的退出需要与服务端协调 ，做到无异常退出
* 服务器推送新闻：服务器向所有的在线用户发送消息

## 服务端和客户端的Socket编程

### 服务端

* 因为可能有多个客户端向服务端请求服务，为了各个客户端的请求互不影响，每个客户端与服务端连接的socket都被一个独立的线程持有
* 服务端可能需要向所有的socket群发消息，所有需要管理所有的与客户端相连的socket，这里采用Hashmap来实现

### 客户端

* 为了统一与服务端的文件传输形式，统一用Message或者User对象包装数据后通过对象处理流来传输
* 文件传输和消息传输采用不同的socket，同服务端客户端的socket也 需要使用hashmap进行管理

![image-20211122143047472](https://i.loli.net/2021/11/22/FjxLCWshRNHtGMK.png)

## 登录界面

这里需要完成两个界面：

1. 未登录时的界面：

显示登录和退出两个选项，登录需要提示输入账号和密码，与服务端交互验证的服务暂时留白

2. 登录成功后显示菜单

选择私聊、群聊、发文件、拉取在线用户名单、退出



## 客户端登录验证服务

在登录账号的阶段需要将账号和密码打包到User对象中通过对象处理流输出到Socket中给服务器进行验证，需要实现以下功能：

* 客户端和服务端增加了一个Common包用于储存通过socket传输的共享类，需要注意这两个包内的类的定义方法属性必须保持相同，否则将导致无法对Message对象拆包

* 增加一个Service包用于为客户端提供服务
* 增加账号验证服务类`UserConnectService`用于处理验证账号密码

* 客户端在`UserConnectService`类中的`checkUser`函数中包装User对象通过socket发送给服务器并读取服务端发回的Message对象，拆包得到返回信息的类型，通过类型判断是否登录成功
* 若登录成功，启动一个`ClientConnectServerThread`线程持有该Socket，持续监听服务端socket的是否发送数据
* 因为一个客户端可以有多个账户登录，类比一台电脑上登录多个QQ，需要对所有的线程进行统一管理，在Service包中增加`ClientThreadManage`类，用`HashMap`构建`UserId`与`ClientConnectServerThread`的映射关系，提供封装方法增加映射，为方便调用，属性和方法都设置为静态

```java
public class UserClientService {
    //方便再方法中调用User以及Socket对象，所以将装两个属性写为私有属性
    private User user;
    private Socket socket;

    public boolean checkUser(String userId,String passwd) throws IOException, ClassNotFoundException {
        user = new User(userId,passwd);
        socket = new Socket(InetAddress.getLocalHost(),9999);
        boolean b=false;
        //对象处理流发送user对象到服务端
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(user);
        oos.flush();
        socket.shutdownOutput();
        //从服务器端接收信息
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Message message = (Message) ois.readObject();
        MessageType messageType = message.getMessageType();
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
}
```

## 服务端登录验证服务

服务端持续监听端口，接收客户端发来的User信息，判断是否账号密码是否符合，并回送Message信息

* `QQSever`类用于循环监听端口，接收客户端发来的socket输出流拆包判断`userId`和`passwd`是否符合要求，符合要求建立线程，返回成功信息；不符合要求关闭socket

* 由于存在多个客户端，服务端可能会产生多个socket，同客户端相同需要使用线程来持有不同的socket，同时建立一个`ServerThreadManage`类来管理所有的线程
* 账号密码的验证做了简化，没有使用数据库的知识，先简单把id和密码固定为一个定值，然后可以通过`hashmap`储存，或者使用数据库，写一个check函数实现判断

```java
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
```



* `hashmap`可以优化为`ConcurrentHashMap`线程安全，通过staic属性和静态代码块初始化用户信息

```java
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
```

登录服务写完，项目的基本框架已见雏形，客户端和服务端的通信已经打通

## 客户端请求在线用户列表

* 在登录成功后显示二级菜单，客户端向服务端对应的socket请求在线用户列表

* 在`ClientService`中增加`getUserList`方法，向目标socket发送Message对象，增加`MessageType`一个常量`MESSAGE_GET_USER_LIST`

* 服务端线程持有的socket接收Message，拆包判断`MessageType`类型，新建Message，获取`ServerThreadManage`类下的哈希Map，调用`ketset()`方法，遍历key值，连接放入String变量中，空格隔开，作为`Message content`通过socket发回

```java
if(message.getMessageType().equals(MessageType.MESSAGE_GET_USER_LIST)){
    System.out.println("服务端接收到客户端"+message.getSender()+"请求拉取在线用户列表");
    String str = "";

    for (String s : ServerThreadManage.threads.keySet()) {
        str+=s+" ";
    }
```

* 客户端在线程持有的socket获取message拆包，通过split方法分割string，打印输出

```java
if(message_back.getMessageType().equals(MessageType.MESSAGE_GET_USER_LIST)){
                        String[] onlineUsers = message_back.getContent().split(" ");
                        System.out.println("\n========显示在线列表=======");
                        for (int i = 0; i < onlineUsers.length; i++) {
                            System.out.println(i+1+" "+onlineUsers[i]);
                        }
                    }
```

## 客户端指定和指定用户iD私聊

* 新建`MessageClientService`类用于处理含content内容的消息相关的服务
* 该类中新增`PrivateChat`方法，传出`senderid`和`receiverid`和`content`，包装在`Message`对象中，同时标记type类型，通过id在`ManageThread`类中找到相应的线程和对应的socket，最后发送给服务端
* 服务端简单转发即可
* 离线用户可以在服务端开一个线程专门循环检查用户是否上线，若上线，再发送过去

```java
 else if(message.getMessageType().equals(MessageType.MESSAGE_PRIVATE_COMMON)){
                    //转发信息到目标用户
                    System.out.println(message.getSender()+"请求和"+message.getReceiver()+"聊天，服务器转发消息");
                    //扩充功能可以给离线用户留言
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
                                //上线则发送消息
                                ObjectOutputStream oos = null;
                                try {
                                    oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(message.getReceiver()).getSocket().getOutputStream());
                                    oos.writeObject(message);
                                    System.out.println(message.getReceiver()+"用户已经上线，"+message.getSender()+"的留言已经成功发送给目标用户");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        //开启子线程准备用于将消息再用户在线时转发给对方
                        new Thread(waitOnline).start();
```

## 客户端和服务端无异常退出

* 在`UserClientService`端增加`quitClient`方法，将包含senderid和`quit`type的message发给服务端

```
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
```

* 服务端识别message，回送信息后关闭socket以及服务端对应的线程，并从线程池中删除该线程

```java
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
```



* 客户端接收到发回的信息，关闭客户端的socket和线程，并从客户端的线程池中删除该线程

```java
else if(message_back.getMessageType().equals(MessageType.MESSAGE_CLIENT_QUIT)){
    System.out.println(message_back.getReceiver()+"客户端下socket关闭");
    ClientThreadManage.deleteThread(message_back.getReceiver());
    socket.close();
    ois.close();
    break;
}
```

## 客户端与所有在线用户id群聊

* 在`MessageClientService`中增加`publicChat`方法，将时间，senderid以及receiverid打包传送给服务端，标记为群发消息标记

```java
public void publicChat(String senderId,String content){

        //将senderid content time 以及type写入message对象
        Message message = new Message();
        message.setSender(senderId);
        message.setMessageType(MessageType.MESSAGE_PUBLIC_COMMON);
        message.setContent(content);
        message.setSendTime(new Date().toString());
        message.setReceiver("所有人");
        System.out.println(message.getSendTime());
        System.out.println(message.getSender()+" 对 "+message.getReceiver()+"说："+message.getContent());
        try {
            //将message对象发送给服务端
            ObjectOutputStream oos = new ObjectOutputStream(ClientThreadManage.getThread(senderId).getSocket().getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
```

* 服务端将消息转发给所有的在线用户，用keyset()方法取出`hashmap`中所有的key，for循环遍历一一发送即可

```java
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
```

## 客户端发送给指定id用户文件

* 通过路径指定文件源地址和目的地址，在Message内容中增加`filePath fileData byteLen `属性

```java
	private String filePath;//目标文件路径
    private int byteLen;//文件长度
    private byte[] fileData;//文件数据保存在字节数组中
```



* 在`MessageClientService`增加`fileSend`方法，通过`BufferedInputStream`包装`FileInputSteam`写入byte数组中然后包装在Message对象中发送给服务端

```java
public void fileSend(String senderId,String receiverId,String localPath,String targetPath){
        Message message = new Message();
        //标记为文件消息类型，记录senderid receiverId tagetPath
        message.setReceiver(receiverId);
        message.setSender(senderId);
        message.setMessageType(MessageType.MESSAGE_FILE);
        message.setFilePath(targetPath);
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(localPath));
            ObjectOutputStream oos = new ObjectOutputStream(ClientThreadManage.getThread(senderId).getSocket().getOutputStream());

            //byte数组用于接收文件数据,reaLen记录接收的字节长度
            int readLen = 0;
            byte[] bytes = new byte[(int)new File(localPath).length()];
            if((readLen = bufferedInputStream.read(bytes))!=0) {
                //将数据字节数组和数组长度的信息包装在message对象中传输给服务端
                message.setFileDate(bytes);
                message.setByteLen(readLen);
                oos.writeObject(message);
                //关闭文件输入流
                bufferedInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
```



* 服务端做在线判断然后直接中转后，发给目标id用户，若离线开启一个线程循环判断是否上线，上线后发给目标id用户

```java
else if(message.getMessageType().equals(MessageType.MESSAGE_FILE)){
                    System.out.println(message.getSender()+"请求向"+message.getReceiver()+"发送文件");
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
```

* 客户端接收message对象并通过`fileoutputstream`发到目的文件地址

```java
else if(message_back.getMessageType().equals(MessageType.MESSAGE_FILE)){
                        //BufferedOutputStream用来写入目标地址
                        System.out.println("接收到"+message_back.getSender()+"发来的文件数据，文件保存在"+message_back.getFilePath());
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(message_back.getFilePath()));
                        bufferedOutputStream.write(message_back.getFileDate(),0,message_back.getByteLen());
                        //关闭文件输出流
                        bufferedOutputStream.close();
                    }
```

## 服务端推送消息给所有客户端

* 服务端在`QQServer`中开启一个线程专门用于给其他的客户端推送消息，输入信息后包装为message对象，通过遍历线程key锁定对应的socket，发送即可

```java
Runnable NewsSend = new Runnable() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.setSender("服务端");
                    message.setMessageType(MessageType.MESSAGE_NEWS);
                    while (!ss.isClosed()){
                        System.out.println("服务端推送消息服务已启动，请输入想对所有人推送的消息[输入“exit”关闭推送服务]：");
                        String str = Utility.readString(100);
                        if(!str.equals("exit")){
                            //向所有在线的客户端发送消息
                            message.setContent(str);
                            message.setSendTime(new Date().toString());
                            for (String s : ServerThreadManage.threads.keySet()) {
                                try {
                                    message.setReceiver(s);
                                    ObjectOutputStream oos = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(s).getSocket().getOutputStream());
                                    oos.writeObject(message);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }else {
                            //退出该线程，关闭推送服务
                            break;
                        }
                    }
                }
            };
            new Thread(NewsSend).start();
```



* 在`MessageType`中增加`MESSAGRE_NEWS`类，客户端接收message对象后直接打印信息即可

```java
else if(message_back.getMessageType().equals(MessageType.MESSAGE_NEWS)){
                        System.out.println(message_back.getSendTime());
                        System.out.print("接收到服务端推送给"+message_back.getReceiver()+"的消息：");
                        System.out.println(message_back.getContent());
                    }
```

## 一些优化

关于离线发送文件和离线留言的实现方式，我采用了再开一个线程，但是这样对cpu资源的占用过高，若用户持续不在线，线程将一直占用，视频中老师在服务器端开了离线message对象的`hashmap`，每当有一个用户登录验证后同时在`hashmap`中寻找是否存在关于他的离线留言

这里我把离线消息的部分优化了一下

* 服务端增加`OfflineMessageManage`的类，hashmap用于管理离线消息，注意这里只支持一个id放一个离线消息

```java
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
```



* `ServerThread`的run方法中判断是否在线，不在线放入`hashmap`中
* 在`QQSever()`方法中验证账号发送成功Message对象后，判断`hashmap`中是否存在该id的离线消息，若存在直接进行一个转发，注意要重新new一个输出流，不可用之前验证成功登录的输出流

```java
if(checkUser(user.getUserId(), user.getPasswd())){
                    //账号密码符合要求则建立线程持有该socket
                    ServerConnectThread serverConnectThread = new ServerConnectThread(socket, user.getUserId());
                    ServerThreadManage.addServerConnectThread(user.getUserId(),serverConnectThread);
                    serverConnectThread.start();
                    //向客户端传输写入登录成功的信息

                    message.setMessageType(MessageType.MESSAGE_LOGIN_SUCCESS);
                    message.setContent("userId"+user.getUserId()+"登录成功");
                    oos.writeObject(message);
                    //判断OfflineMessages中是否有发给该用户的离线消息
                    if(OfflineMessageManage.getMessage(user.getUserId())!=null){
                        Message offlinemessage = OfflineMessageManage.getMessage(user.getUserId());
                        //因为上面的oos已经对应了一个ois，而offlinemessage是在线程中重新new一个ois的，这里输出时也要重新new一个，否则回报错
                        ObjectOutputStream oos_ = new ObjectOutputStream(ServerThreadManage.getServerConnectThread(user.getUserId()).getSocket().getOutputStream());
                        oos_.writeObject(offlinemessage);
                        System.out.println("用户"+offlinemessage.getSender()+"给用户"+offlinemessage.getReceiver()+"的离线留言已经发送成功");
                        //将该离线留言在hashmap中删除
                        OfflineMessageManage.deleteMessage(user.getUserId());
                    }
                }
```

