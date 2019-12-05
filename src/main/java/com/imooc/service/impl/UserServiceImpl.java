package com.imooc.service.impl;

import com.imooc.enums.MsgActionEnum;
import com.imooc.enums.MsgSignFlagEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.mapper.*;

import com.imooc.netty.ChatMsg;
import com.imooc.netty.DataContent;
import com.imooc.netty.UserChannelRel;
import com.imooc.pojo.FriendsRequest;
import com.imooc.pojo.MyFriends;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);
        return result != null ? true : false;
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {

        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);
        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users users) {

        String userId=sid.nextShort();
        //TODO   为每一个用户生成 一个唯一的 二维码
        users.setQrcode("ww");
        users.setId(userId);
        usersMapper.insert(users);
        return users;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
            usersMapper.updateByPrimaryKeySelective(user);
            return queryUserById(user.getId());
    }



    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        //1. 搜索的用户如果不存在，返回【无此用户】
        Users users=queryUserInfoByUsername(friendUsername);
        if (users==null){
              return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }

        //2. 搜索的用户如果是你自己，返回【不能添加自己】
        if (users.getId().equals(myUserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //3. 搜索的用户如果已经是你的好用，返回【该用户已是你的好友】
        Example mfexample=new Example(MyFriends.class);
        Example.Criteria mfcriteria=mfexample.createCriteria();
        mfcriteria.andEqualTo("myUserId",myUserId);
        mfcriteria.andEqualTo("myFriendUserId",users.getId());
        MyFriends myFriendsResult=myFriendsMapper.selectOneByExample(mfexample);
        if (myFriendsResult!=null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    // 通过 id  来 查询用户
    private Users queryUserById(String userId){
         return usersMapper.selectByPrimaryKey(userId);
    }


    //通过 姓名 来查询用户
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String friendUsername){

          Example example=new Example(Users.class);
          Example.Criteria criteria=example.createCriteria();
          criteria.andEqualTo("username",friendUsername);
          return usersMapper.selectOneByExample(example);
    }



    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users sendFriendRequest(String myUserId, String friendUsername) {
                //根据用户名把 朋友信息查询出来
                Users friend=queryUserInfoByUsername(friendUsername);
                //1.查询发送好友请求记录表
                Example fre=new Example(FriendsRequest.class);
                Example.Criteria frc=fre.createCriteria();
                frc.andEqualTo("sendUserId",myUserId);
                frc.andEqualTo("acceptUserId",friend.getId());
                FriendsRequest friendsRequest=friendsRequestMapper.selectOneByExample(fre);
                if (friendsRequest==null){
                    //如果不是你的好友，且 好友记录没有添加，则新增好友请求记录
                    String requestId=sid.nextShort();
                    FriendsRequest request=new FriendsRequest();
                    request.setId(requestId);
                    request.setSendUserId(myUserId);
                    request.setAcceptUserId(friend.getId());
                    request.setRequestDateTime(new Date());
                    friendsRequestMapper.insert(request);
                }

                 return friend;
    }


    /**
     *      查询好友请求
     * @param acceptUserId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {

        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId,String acceptUserId) {
            Example example=new Example(FriendsRequest.class);
            Example.Criteria criteria=example.createCriteria();
            criteria.andEqualTo("acceptUserId",acceptUserId);
            criteria.andEqualTo("sendUserId",sendUserId);
            friendsRequestMapper.deleteByExample(example);
    }



    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId,String acceptUserId) {
        saveFriends(sendUserId,acceptUserId);//1.正向保存
        saveFriends(acceptUserId,sendUserId);//2.逆向保存
        deleteFriendRequest(sendUserId,acceptUserId);// 3.删除好友请求列表

        //使用 websocket 主动推送消息到请求发起者，更新他的通讯录列表为最新

        Channel sendChannel=UserChannelRel.get(sendUserId);
        if (sendChannel!=null){
             //使用 websocket 主动推送消息到请求发起者，更新他的通讯录
            DataContent dataContent=new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush(new
                    TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {

        List<MyFriendsVO> myFriendsVOList =usersMapperCustom.queryMyFriends(userId);
        return myFriendsVOList;
    }




    private void saveFriends(String sendUserId,String acceptUserId) {
           MyFriends myFriends=new MyFriends();
           String recordId=sid.nextShort();
           myFriends.setId(recordId);
           myFriends.setMyFriendUserId(acceptUserId);
           myFriends.setMyUserId(sendUserId);
           myFriendsMapper.insert(myFriends);
    }


    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.imooc.pojo.ChatMsg msgDB=new com.imooc.pojo.ChatMsg();
        String msgId=sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());
        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Override
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample=new Example(com.imooc.pojo.ChatMsg.class);
        Example.Criteria chatCriteria =chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag",0);
        chatCriteria.andEqualTo("acceptUserId",acceptUserId);
        List<com.imooc.pojo.ChatMsg> result=chatMsgMapper.selectByExample(chatExample);
        return result;
    }


}
