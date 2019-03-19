package com.mrj.chat.mapper;

import com.mrj.chat.pojo.FriendsRequest;
import com.mrj.chat.pojo.Users;
import com.mrj.chat.pojo.vo.FriendRequestVo;
import com.mrj.chat.pojo.vo.MyFriendsVo;
import com.mrj.chat.utils.MyMapper;

import java.util.List;

public interface UsersMapperCustom extends MyMapper<Users> {
    public List<FriendRequestVo> queryFriendRequestList(String acceptUserId);
    public List<MyFriendsVo> queryMyFriends(String userId);
    public void batchUpdateMsgSigned(List<String> msgIdList);

}