package common;

/**
 * @author 小羊Shaun
 * @version 1.0
 */
public interface MessageType {
    //用传回的信息中的信息来反映登录是否成功
    String MESSAGE_LOGIN_SUCCESS = "1";
    String MESSAGE_LOGIN_FAIL = "2";
    String MESSAGE_GET_USER_LIST= "3";
    String MESSAGE_CLIENT_QUIT= "4";
    String MESSAGE_PRIVATE_COMMON= "5";
    String MESSAGE_PUBLIC_COMMON= "6";
    String MESSAGE_FILE= "7";

}
