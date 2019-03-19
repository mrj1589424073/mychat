package com.mrj.chat.netty;

import io.netty.channel.Channel;

import java.util.HashMap;

/*
* 用户Id和channel的关联关系处理
* */
public class UserChannelRel {

    private static HashMap<String, Channel> manager = new HashMap<>();

    public static void put(String sendId,Channel channel){
        manager.put(sendId,channel);
    }

    public static Channel get(String senderId){
      return manager.get(senderId);
    }

    public static void output(){
        for (HashMap.Entry<String,Channel> entry: manager.entrySet()){
            System.out.println("UserId:"+entry.getKey()+",ChannelId:"
                    +entry.getValue().id().asLongText());
        }
    }
}
