package com.mrj.chat.controller;

import com.mrj.chat.enums.OperatorFriendRequestTypeEnum;
import com.mrj.chat.enums.SearchFriendsStatusEnum;
import com.mrj.chat.pojo.ChatMsg;
import com.mrj.chat.pojo.Users;
import com.mrj.chat.pojo.bo.UsersBo;
import com.mrj.chat.pojo.vo.MyFriendsVo;
import com.mrj.chat.pojo.vo.UsersVo;
import com.mrj.chat.service.UserService;
import com.mrj.chat.utils.FastDFSClient;
import com.mrj.chat.utils.FileUtils;
import com.mrj.chat.utils.IMoocJSONResult;
import com.mrj.chat.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    //登录或者注册
    @PostMapping("registerOrLogin")
    public IMoocJSONResult registerOrLogin(@RequestBody Users user) throws Exception {
        //判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername())||StringUtils.isBlank(user.getPassword())){
             return IMoocJSONResult.errorMsg("用户名或密码不能为空");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist){
            //存在就登录
           userResult = userService.queryUserForLogin(user.getUsername(),MD5Utils.getMD5Str(user.getPassword()));
           if(userResult==null){
               return IMoocJSONResult.errorMsg("用户名或密码不正确");
           }
        }else{
            //不存在就注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }
        //将部分属性拷贝到Uservo
        UsersVo userVo = new UsersVo();
        BeanUtils.copyProperties(userResult,userVo);
        return IMoocJSONResult.ok(userVo);
    }

    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBo usersBo) throws Exception {
    //获取前端传过来的base64字符串.然后转换为文件对象再上传
        String faceData = usersBo.getFaceData();
        //先定义一个本地地址
        String userFacePath = "D:\\"+ usersBo.getUserId()+"userface64.png";
        //转换成文件对象
        FileUtils.base64ToFile(userFacePath,faceData);

        //转换成mutifile然后上传
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        //完成上传，会传回一个地址。这个时候存了两份。一个大的，一个小的（有具体方法）
        //"xxx.png"和“xxx_80*80.png”
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        //获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0]+thump+arr[1];

        //更新用户头像
        Users user = new Users();
        user.setId(usersBo.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);
        //一定要返回完整的user，否则前端显示不完整
        Users result = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(result);
    }

    @PostMapping("setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBo usersBo){

        Users user = new Users();
        user.setId(usersBo.getUserId());
        System.out.println(usersBo.getNickname());
        user.setNickname(usersBo.getNickname());

        Users result = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(result);
       }

      //搜索好友接口,这是匹配查询而不是模糊查询
     @PostMapping("search")
    public IMoocJSONResult searchUser(String myuserId,String friendUsername) {
         //判空
         if (StringUtils.isBlank(myuserId) || StringUtils.isBlank(friendUsername)) {
             return IMoocJSONResult.errorMsg("不能为空");
         }
         //前置条件.用户不存在，返回没有用户,搜索的是自己，返回不能添加自己，搜索的已经是好友，返回该用户已经是好友
         Integer status = userService.preconditionSearchFriends(myuserId, friendUsername);
         if (status == SearchFriendsStatusEnum.SUCCESS.status){
             Users user = userService.queryUserInfoByUsername(friendUsername);
             //不能返回敏感信息。所以返回Uservo
             UsersVo usersVo = new UsersVo();
             BeanUtils.copyProperties(user,usersVo);
             return IMoocJSONResult.ok(usersVo);
         }else{
             //根据相关status返回信息
             String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
             //返回前端错误信息
             return IMoocJSONResult.errorMsg(errorMsg);
         }
     }


    @PostMapping("addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myuserId,String friendUsername) {
        //判空
        if (StringUtils.isBlank(myuserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("不能为空");
        }

        //在添加朋友的时候也应该做前置条件的判断。因为用户有可能绕过搜索的接口进行操作
        //前置条件.用户不存在，返回没有用户,搜索的是自己，返回不能添加自己，搜索的已经是好友，返回该用户已经是好友
        Integer status = userService.preconditionSearchFriends(myuserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status){
            //如果符合添加条件就进行添加
            userService.sendFriendRequest(myuserId,friendUsername);
        }else{
            //根据相关status返回信息
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            //返回前端错误信息
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        //返回可以添加的状态
        return IMoocJSONResult.ok();
    }

    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId){
        //判空

        if(StringUtils.isBlank(userId)){
            return IMoocJSONResult.errorMsg("未知错误");
        }
        //查询用户接收到的朋友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,String sendUserId,Integer operType){
        //判空
        if(StringUtils.isBlank(acceptUserId)||StringUtils.isBlank(sendUserId)||operType ==null){
            return IMoocJSONResult.errorMsg("未知错误");
        }
        //如果没有对应的枚举。就抛出错误
        if(StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))){
            return IMoocJSONResult.errorMsg("未知错误");
        }
        //根据请求类型进行操作
        if(operType == OperatorFriendRequestTypeEnum.IGNORE.type){
             //如果忽略好友请求。则直接删除好友请求数据的数据库表记录
            userService.deleteFriendRequest(sendUserId,acceptUserId);
        }else if(operType ==OperatorFriendRequestTypeEnum.PASS.type){
            //如果通过请求，则添加好友到数据库表中，然后删除请求
            userService.passFriendRequest(sendUserId,acceptUserId);
        }
        //数据库查询好友列表。进行列表更新.接收者没有问题了,但是发送者也要同时更新
        List<MyFriendsVo> friends = userService.queryMyFriends(acceptUserId);

        return IMoocJSONResult.ok(friends);
    }
   //查新我的好友列表
    @PostMapping("myFriends")
    public IMoocJSONResult myFriends(String userId){
        //判空
        if(StringUtils.isBlank(userId)){
            return IMoocJSONResult.errorMsg("未知错误");
        }
        //查询好友列表
        List<MyFriendsVo> myFriends = userService.queryMyFriends(userId);
        return IMoocJSONResult.ok(myFriends);
    }

    //用户手机端获取未签收的消息列表,也就是未读的消息
    @PostMapping("getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId){
        //判空
        if(StringUtils.isBlank(acceptUserId)){
            return IMoocJSONResult.errorMsg("未知错误");
        }
        //查询消息列表
        List<ChatMsg> result = userService.getUnreadMsgList(acceptUserId);
        return IMoocJSONResult.ok(result);
    }

}
