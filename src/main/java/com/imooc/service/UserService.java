package com.imooc.service;



import com.imooc.netty.ChatMsg;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {

    /**
     * @Description: 判断用户名是否存在
     */
    public boolean queryUsernameIsExist(String username);

    /**
     * @Description: 查询用户是否存在
     */
    public Users queryUserForLogin(String username, String pwd);

    /**
     * @Description: 用户注册
     */
    public Users saveUser(Users user);

    /**
     *      修改用户记录
     */
    public  Users updateUserInfo(Users user);


    /**
     *            搜索朋友的前置条件
     * @param myUserId
     * @param friendUsername
     * @return
     */
    public Integer preconditionSearchFriends(String myUserId,String friendUsername);


    /**
     *            通过姓名来查用户信息
     * @param friendUsername
     * @return
     */
      public Users queryUserInfoByUsername(String friendUsername);


    /**
     *       添加好友请求记录，保存到数据库
     * @param myUserId
     * @param friendUsername
     */
    public Users sendFriendRequest(String myUserId, String friendUsername);


    /**
     *        （被请求方  查询  主动方）    查询好友请求
     * @param acceptUserId
     * @return
     */
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);


    /**
     *     删除（忽略） 好友请求
     * @param acceptUserId
     * @param sendUserId
     */
    public void deleteFriendRequest(String sendUserId,String acceptUserId);

    /**
     *       通过好友请求：分三步
     *       1. 保存好友
     *       2.逆向保存好友
     *       3.删除好友请求列表
     * @param acceptUserId
     * @param sendUserId
     */
    public void passFriendRequest(String sendUserId,String acceptUserId);


    /**
     *       查询我的好友列表
     * @param userId
     * @return
     */
    public  List<MyFriendsVO> queryMyFriends(String userId);


    /**
     *        保存聊天信息到数据库
     * @param chatMsg
     * @return
     */
    public String saveMsg(ChatMsg chatMsg);

    /**
     *    批量签收消息
     * @param msgIdList
     */
    public void updateMsgSigned(List<String> msgIdList);


    /**
     *    用户手机端获取未签收的消息列表
     * @return
     */
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);

}
