package Service;

import common.Message;
import common.MessageType;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.Date;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public class MessageClientService {
    public void PrivateChat(String senderId,String receiverId,String content){

        Message message = new Message();
        message.setSendTime(new Date().toString());
        message.setSender(senderId);
        message.setReceiver(receiverId);
        message.setContent(content);
        message.setMessageType(MessageType.MESSAGE_PRIVATE_COMMON);
        System.out.println(message.getSendTime());
        System.out.println(senderId+"对"+receiverId+"说："+content);
        try {
            //获取发送端的socket发送数据给服务端转发
            ObjectOutputStream oos = new ObjectOutputStream(ClientThreadManage.getThread(senderId).getSocket().getOutputStream());
            oos.writeObject(message);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
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
}
