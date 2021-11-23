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

* 由于存在多个客户端，服务端可能会产生多个socket，同客户端相同需要使用线程来持有不同的socket，同时建立一个`ServerThreadManage`类来管理所有的线程

* 账号密码的验证做了简化，没有使用数据库的知识，简单把id和密码固定为一个定值，后期可以通过hashmap储存，或者使用数据库
