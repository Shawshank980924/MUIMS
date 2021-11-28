package view;

import Service.ClientConnectServerThread;
import Service.MessageClientService;
import Service.UserClientService;

import java.io.IOException;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class QQView {
    //用于显示登录和选择界面,启动  程序
    boolean loop = true;
    UserClientService userClientService = new UserClientService();
    MessageClientService messageClientService = new MessageClientService();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new QQView().mainMenu();
    }
    public void mainMenu() throws IOException, ClassNotFoundException {
        String key ="";
        while(loop){
            System.out.println("==========欢迎使用多用户即时聊天系统============");
            System.out.println("1: 登录");
            System.out.println("9: 退出系统 ");
            System.out.print("请输入相应的序号：\n");
            key = Utility.readString(1);
            switch (key){
                case "1":
                    String userId = "";
                    String passwd = "";
                    System.out.print("请输入账号");
                    userId = Utility.readString(10);
                    System.out.print("请输入密码");
                    passwd = Utility.readString(50);
                    if(userClientService.checkUser(userId,passwd)){
                        while(loop){
                            String option;
                            //登录成功显示二级菜单
                            System.out.println("============欢迎(用户 "+userId+")登录多用户即时聊天系统");
                            System.out.println("===============用户选择菜单================");
                            System.out.println("1:显示用户在线列表");
                            System.out.println("2:私          聊");
                            System.out.println("3:传  输   文  件");
                            System.out.println("4:群          聊");
                            System.out.println("9:退  出   系  统");
                            option = Utility.readString(1);
                            switch (option){
                                case "1":
//                                System.out.println("显示用户列表 ");
                                    userClientService.getUserList();
                                    break;

                                case "2":
//                                    System.out.println("私聊");
                                    System.out.println("请输入你想聊天的用户ID");
                                    String friendId = Utility.readString(10);
                                    System.out.println("请输入聊天内容");
                                    String content = Utility.readString(100);
                                    messageClientService.PrivateChat(userId,friendId,content);
                                    break;
                                case "3":
//                                    System.out.println("传输文件");
                                    System.out.println("请输入发送文件的用户Id（在线）");
                                    String receiverId = Utility.readString(10);
                                    System.out.println("请输入本机文件地址");
                                    String localPath = Utility.readString(100);
                                    System.out.println("请输入目标文件保存地址");
                                    String targetPath = Utility.readString(100);
                                    messageClientService.fileSend(userId,receiverId,localPath,targetPath);

                                    break;

                                case "4":
//                                    System.out.println("群聊");
                                    System.out.println("请输入群聊内容");
                                    String public_content = Utility.readString(100);
                                    //群聊只要自己的id和群聊的内容即可
                                    messageClientService.publicChat(userId,public_content);
                                    break;
                                case "9":
                                    loop = false;
                                    userClientService.quitClient();
//                                    System.out.println("==========退出系统===========");
                                    break;

                            }
                        }

                    }
                    else{
                        System.out.println("登录失败");
                    }
                    break;
                case "9":
                    loop = false;
                    System.out.println("==========退出系统===========");
                    break;

            }
        }
    }
}
