package com.mrj.chat.service;

import com.mrj.chat.netty.ChatMsg;
import com.mrj.chat.pojo.FriendsRequest;
import com.mrj.chat.pojo.Users;
import com.mrj.chat.pojo.vo.FriendRequestVo;
import com.mrj.chat.pojo.vo.MyFriendsVo;

import java.io.IOException;
import java.util.List;

public interface UserService {
    //判断用户名存不存在
    public boolean queryUsernameIsExist(String username);

    public Users queryUserForLogin(String username,String password);
    //用户注册
    public Users saveUser(Users user) throws IOException;
    //修改用户记录
    public Users updateUserInfo(Users user);
   //搜索朋友的前置条件
    public Integer preconditionSearchFriends(String myuserId,String friendusername);
    //根据用户名查询用户对象
    public Users queryUserInfoByUsername(String username);
   //添加好友请求保存到数据库
    public void sendFriendRequest(String myuserId,String friendUsername);

    //查询发送请求相关信息
    public List<FriendRequestVo> queryFriendRequestList(String acceptUserId);
    //删除朋友请求记录
    public void deleteFriendRequest(String sendUserId,String acceptUserId);
   //通过好友请求。1.保存好友，2，逆向保存好友，3，删除好友请求记录
    public void passFriendRequest(String sendUserId,String acceptUserId);
  //查询好友列表
    public List<MyFriendsVo> queryMyFriends(String userId);
  //保存聊天消息到数据库
    public String saveMsg(ChatMsg chatMsg);
  //批量签收消息
    public void updateMsgSigned(List<String> msgIdList);
   //获取未签收消息列表
    public List<com.mrj.chat.pojo.ChatMsg> getUnreadMsgList(String acceptUserId);
}
