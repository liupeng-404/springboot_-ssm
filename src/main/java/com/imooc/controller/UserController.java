package com.imooc.controller;

import com.imooc.enums.OperatorFriendRequestTypeEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.pojo.ChatMsg;
import com.imooc.pojo.User;
import com.imooc.pojo.Users;
import com.imooc.pojo.bo.UsersBO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.pojo.vo.UsersVO;
import com.imooc.service.impl.UserServiceImpl;

import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.IMoocJSONResult;

import com.imooc.utils.MD5Utils;
import org.springframework.beans.BeanUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserServiceImpl userService;


    @Autowired
    private FastDFSClient fastDFSClient;

    /**
     * @Description: 用户注册/登录
     */
    @PostMapping("/registOrLogin")
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {

        // 0. 判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空...");
        }

//        System.out.println("registOrLoginregistOrLoginregistOrLogin");
//        System.out.println(user.getUsername()+"123445566666");
        // 1. 判断用户名是否存在，如果存在就登录，如果不存在则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist) {
            // 1.1 登录
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));
        
            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确...");
            }
        } else {
            // 1.2 注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);

        }

        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userResult, userVO);



        return IMoocJSONResult.ok(userVO);
    }



    /**
     * @Description: 上传用户头像   base64格式
     * @RequestBody UsersBO userBO
     * String userId,String faceData
     */
    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {

        // 获取前端传过来的base64字符串, 然后转换为文件对象再上传

        String base64Data = userBO.getFaceData();
        String userFacePath = "C:\\MXpicture\\" + userBO.getUserId()+"\\"+userBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath, base64Data);

        System.out.println("111111111111111111"+userBO.getUserId());
        System.out.println("222222222222222222"+base64Data);

        // 上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

//		"dhawuidhwaiuh3u89u98432.png"
//		"dhawuidhwaiuh3u89u98432_80x80.png"

        // 获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        // 更细用户头像
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        Users result = userService.updateUserInfo(user);

        UsersVO usersVO=new UsersVO();
        BeanUtils.copyProperties(user,usersVO);
        return IMoocJSONResult.ok(usersVO);
    }





    /**
     *
     *         搜索好友接口 : 根据账号做匹配查询而不是模糊查询
     */
    @PostMapping("/search")
    public IMoocJSONResult search(String myUserId,String friendUsername) throws Exception {



        if (StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUsername)){
                  return  IMoocJSONResult.errorMsg("");
        }
        //前置条件  -1. 搜索的用户如果不存在，返回【无此用户】
        //前置条件  -2. 搜索的用户如果是你自己，返回【不能添加自己】
        //前置条件  -3. 搜索的用户如果已经是你的好用，返回【该用户已是你的好友】

        Integer status=userService.preconditionSearchFriends(myUserId,friendUsername);
        if (status== SearchFriendsStatusEnum.SUCCESS.status){
             Users users=userService.queryUserInfoByUsername(friendUsername);
             UsersVO usersVO=new UsersVO();
             BeanUtils.copyProperties(users,usersVO);
             return IMoocJSONResult.ok(usersVO);
        }else {
             String errorMsg=SearchFriendsStatusEnum.getMsgByKey(status);
             return IMoocJSONResult.errorMsg(errorMsg);
        }

    }


    /**
     *          发送添加好友的请求
     * @param myUserId
     * @param friendUsername
     * @return
     * @throws Exception
     */

    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId,String friendUsername) throws Exception{



        if (StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUsername)){
            return  IMoocJSONResult.errorMsg("");
        }
        //前置条件  -1. 搜索的用户如果不存在，返回【无此用户】
        //前置条件  -2. 搜索的用户如果是你自己，返回【不能添加自己】
        //前置条件  -3. 搜索的用户如果已经是你的好用，返回【该用户已是你的好友】

        Integer status=userService.preconditionSearchFriends(myUserId,friendUsername);
        if (status== SearchFriendsStatusEnum.SUCCESS.status){
            Users users=userService.sendFriendRequest(myUserId,friendUsername);

        }else {
            String errorMsg=SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        return IMoocJSONResult.ok();
    }


    /**
     *             查询  有多人想加你为好友
     * @param userId
     * @return
     * @throws Exception
     */
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId) throws Exception{

          //1. 判断 userId不能为空
        if (StringUtils.isBlank(userId)){
            return  IMoocJSONResult.errorMsg("");
        }
        //2. 查询用户接收到的朋友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }


    /**
     *      接受方  通过 或者 忽略 朋友请求
     *
     * @param acceptUserId
     * @param sendUserId
     * @param operType
     * @return
     * @throws Exception
     */
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,
                                             String sendUserId,Integer operType) throws Exception{

        //1. 判断三个参数 不能为空
        if (StringUtils.isBlank(acceptUserId)||
                StringUtils.isBlank(sendUserId)||
                operType==null){
            return  IMoocJSONResult.errorMsg("");
        }
        //2. 如果 operType 没有对应的枚举值，则直接抛出空错误信息
        if(StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))){
            return  IMoocJSONResult.errorMsg("");
        }
        //3. 判断如果是 忽略好友请求，则直接删除好友请求的数据库的记录
        if(operType==OperatorFriendRequestTypeEnum.IGNORE.type){
            userService.deleteFriendRequest(sendUserId,acceptUserId);
        }
          else if(operType==OperatorFriendRequestTypeEnum.PASS.type){
            userService.passFriendRequest(sendUserId,acceptUserId);
        }
        //4. 数据库中查询 好友列表 ,谁接受好友请求，就更新谁的好友列表
        List<MyFriendsVO> myFriendsVOList=userService.queryMyFriends(acceptUserId);

        return IMoocJSONResult.ok(myFriendsVOList);
    }


    /**
     *
     *        获取好友列表
     * @param userId
     * @return
     * @throws Exception
     */

    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) throws Exception{

        //1. 判断 userId 是否为空
        if (StringUtils.isBlank(userId)){
             return IMoocJSONResult.errorMsg("");
        }
        //2. 数据库中查询 好友列表
        List<MyFriendsVO> myFriendsVOList=userService.queryMyFriends(userId);

        return IMoocJSONResult.ok(myFriendsVOList);
    }


    /**
     *      用户手机端获取未签收的消息列表
     * @param acceptUserId
     * @return
     * @throws Exception
     */
    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) throws Exception{

        //1. 判断 acceptUserId 是否为空
        if (StringUtils.isBlank(acceptUserId)){
            return IMoocJSONResult.errorMsg("");
        }

        //2.查询列表
        List<com.imooc.pojo.ChatMsg> unreadMsgList=userService.getUnReadMsgList(acceptUserId);
        return IMoocJSONResult.ok(unreadMsgList);
    }

}
