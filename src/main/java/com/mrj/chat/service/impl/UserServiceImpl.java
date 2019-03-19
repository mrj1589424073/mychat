package com.mrj.chat.service.impl;

import com.mrj.chat.enums.MsgActionEnum;
import com.mrj.chat.enums.MsgSignFlagEnum;
import com.mrj.chat.enums.SearchFriendsStatusEnum;
import com.mrj.chat.mapper.*;
import com.mrj.chat.netty.ChatMsg;
import com.mrj.chat.netty.DataContent;
import com.mrj.chat.netty.UserChannelRel;
import com.mrj.chat.org.n3r.idworker.Sid;
import com.mrj.chat.pojo.FriendsRequest;
import com.mrj.chat.pojo.MyFriends;
import com.mrj.chat.pojo.Users;
import com.mrj.chat.pojo.vo.FriendRequestVo;
import com.mrj.chat.pojo.vo.MyFriendsVo;
import com.mrj.chat.service.UserService;
import com.mrj.chat.utils.FastDFSClient;
import com.mrj.chat.utils.FileUtils;
import com.mrj.chat.utils.JsonUtils;
import com.mrj.chat.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
//默认的别名是首字母小写的类名
public class UserServiceImpl implements UserService {
    @Autowired
    private UsersMapper usersMapper;
    //生成唯一的主键
    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Autowired
    private UsersMapperCustom usersMapperCustom;
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {
        //selectone（）用于唯一属性的识别,参数是对象
        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);
        return result != null?true:false;
    }
    //添加事务的支持。后边是事务级别
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username,String password) {
       //tkMapper提供的类，通过提供的条件进行相关操作
        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        //将前端传过来的值和数据库中的 值进行匹配
        criteria.andEqualTo("username",username);
        criteria.andEqualTo("password",password);

        //将查询到的值返回
        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users saveUser(Users user) {
        //生成一个唯一的Id
        String userId = sid.nextShort();
        //为每个用户生成一个唯一的二维码
        String qrCodePath = "D://user"+userId+"qrcode.png";
        //weiliao_qrcode：[username]生成这样的,第一个参数是路径，第二个是样式
        qrCodeUtils.createQRCode(qrCodePath,"weiliao_qrcode:"+user.getUsername());
        //将生成的二维码图片上传
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(qrCodeUrl);
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);
        return user;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users updateUserInfo(Users user) {
        //根据主键进行全部更新或部分更新
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myuserId, String friendusername) {
        //1.搜索用户不存在.
       Users user = queryUserInfoByUsername(friendusername);
       if(user == null){
           return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
       }

       //2.搜索的账号是你自己
        if(user.getId().equals(myuserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        //3.搜索的已经是你的好友
        Example userself = new Example(MyFriends.class);
        Example.Criteria uc = userself.createCriteria();
        //第一个是实体类中的属性
        uc.andEqualTo("myUserId",myuserId);
        uc.andEqualTo("myFriendUserId",user.getId());
        MyFriends myFriendRel = myFriendsMapper.selectOneByExample(userself);
        if(myFriendRel != null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        //可以添加。。
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    public Users queryUserInfoByUsername(String username){
        Example user = new Example(Users.class);
        Example.Criteria uc = user.createCriteria();
        uc.andEqualTo("username",username);
        return usersMapper.selectOneByExample(user);
    }
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myuserId, String friendUsername) {
        //根据用户名把朋友的信息查询出来
        Users friend = queryUserInfoByUsername(friendUsername);
        //查询发送好友请求记录表，为了防止重复发送
        Example user = new Example(FriendsRequest.class);
        Example.Criteria uc = user.createCriteria();
        uc.andEqualTo("sendUserId",myuserId);
        uc.andEqualTo("acceptUserId",friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(user);
        if(friendsRequest == null){
            //如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();

            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myuserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());

            friendsRequestMapper.insert(request);

        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVo> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example user = new Example(FriendsRequest.class);
        Example.Criteria uc = user.createCriteria();
        uc.andEqualTo("sendUserId",sendUserId);
        uc.andEqualTo("acceptUserId",acceptUserId);
        friendsRequestMapper.deleteByExample(user);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
     //进行双向保存
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        //删除已经处理过的请求
        deleteFriendRequest(sendUserId,acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null){
            //使用WEbsocket主动推送到消息发起者,更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(new TextWebSocketFrame(
                    JsonUtils.objectToJson(dataContent)
            ));
        }
    }
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVo> queryMyFriends(String userId) {
       List<MyFriendsVo> myfriends = usersMapperCustom.queryMyFriends(userId);
        return myfriends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        //这是与数据库相对应的pojo类
        com.mrj.chat.pojo.ChatMsg msgDB = new com.mrj.chat.pojo.ChatMsg();
        String msgId = sid.nextShort();
        System.out.println("生成的Id是:"+msgId);
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
      usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.mrj.chat.pojo.ChatMsg> getUnreadMsgList(String acceptUserId) {
        Example chatExample = new Example(com.mrj.chat.pojo.ChatMsg.class);
        Example.Criteria chatCriteria = chatExample.createCriteria();
        //代表未签收
        chatCriteria.andEqualTo("signFlag",0);
        chatCriteria.andEqualTo("acceptUserId",acceptUserId);

        List<com.mrj.chat.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);
        return result;
    }

    private void saveFriends(String sendUserId, String acceptUserId){

        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(sendUserId);
        myFriends.setMyUserId(acceptUserId);
        myFriendsMapper.insert(myFriends);


    }
    //保存的时候事务是required，查询的时候是supports
    private Users queryUserById(String userId){
        return usersMapper.selectByPrimaryKey(userId);
    }
}
