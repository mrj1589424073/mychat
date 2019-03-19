package com.mrj.chat.netty;

import com.mrj.chat.SpringUtil;
import com.mrj.chat.enums.MsgActionEnum;
import com.mrj.chat.service.UserService;
import com.mrj.chat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
* 处理消息的handler
* TextWebSocketFrame：在netty中，是用于为Websocket专门处理文本的对象
* frame是消息的载体
* */
public class ChartHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    //用于记录和管理客户端所有的channel,得到一个管理的实例
    public  static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
   //1.获取客户端传输过来的消息
        String content = msg.text();
        System.out.println("接收到的数据是："+content);
       //获取当前channel
        Channel currentChannel = ctx.channel();
        DataContent dataContent = JsonUtils.jsonToPojo(content,DataContent.class);
        Integer action = dataContent.getAction();
        //2.判断消息的类型,根据不同的类型处理不同的业务
        if (action == MsgActionEnum.CONNECT.type){
            // 2.1 当websocket第一次open的时候,初始化channel,把用的channel和userId相关联
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId,currentChannel);

            //测试
            for (Channel c:users) {
                System.out.println(c.id().asShortText());
            }
            UserChannelRel.output();
        }else if (action == MsgActionEnum.CHAT.type){
            //2.2 聊天类型的消息,把聊天记录保存到数据库同时标记消息签收状态[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String receiverId = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();

            //保存消息到数据库并标记为未签收,通过spring去获取上下文对象来获取
            UserService userService =(UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            //这里是要返回前端的
            chatMsg.setMsgId(msgId);

            System.out.println(chatMsg);
            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);
            System.out.println("返回的数据是"+dataContentMsg);
            //发送消息
            //从全局用户channel关系中获取接收方的channel,这个时候要手机两端测试.只有一台手机就会显示recivedId为空
            Channel reciveChannel = UserChannelRel.get(receiverId);
            if(reciveChannel == null){
                //channel为空代表用户离线.推送消息(小米推送等第三方)
            }else {
                //当reciveChannel不为空的时候,从ChannelGroup中去查找对应的channel是否存在
                //只有在ChannelGrou[中存在才能进行消息发送,两边都要判断
                Channel findChannel = users.find(reciveChannel.id());
                if(findChannel!= null){
                    //用户在线,将发送的消息转化为字符串,发到前端
                    reciveChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContentMsg)));
                }else{
                    //用户离线.推送消息
                }
            }

        }else if (action == MsgActionEnum.SIGNED.type){
            //2.3签收消息类型.对具体的消息进行签收,修改数据库中消息的签收状态[已经签收]
              //用户读到和签收不同,签收是指发送成功
            UserService userService =(UserService) SpringUtil.getBean("userServiceImpl");
            //用于接收msgid.可以单个,可以多个.逗号间隔
            String msgIdStr = dataContent.getExtend();
            if(msgIdStr != null) {
                String msgIds[] = msgIdStr.split(",");
                //将各个Id放到集合中去
                List<String> msgIdList = new ArrayList<>();
                for (String mid : msgIds) {
                    if (StringUtils.isBlank(mid)) {
                        msgIdList.add(mid);
                    }
                }
                System.out.println(msgIdList.toString());
                if (msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
                    //批量签收
                    userService.updateMsgSigned(msgIdList);
                }
            }
        }else if(action == MsgActionEnum.KEEPALIVE.type){
            //2.4心跳类型的消息
            System.out.println("收到来自channel为:["+currentChannel+"]的心跳");
        }

        for (Channel channel : users) {
            channel.writeAndFlush(new TextWebSocketFrame(content));
        }
        //下边这个方法和上面的for循环一致
        //users.writeAndFlush(new TextWebSocketFrame("[服务器在]"+ LocalDateTime.now()+"接收到消息："+content));
    }

    //当客户端连接服务端以后（打开连接）
    //获取客户端的channel，并且放到channelgroup去管理
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //当触发handerRemoved。ChannelGroup会自动移除对应客户端的channel
        users.remove(ctx.channel());
        System.out.println("客户端断开，channel对应的长Id是："+ctx.channel().id().asLongText());
        System.out.println("客户端断开 ，channel对应的短Id是："+ctx.channel().id().asShortText());
    }


    //异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       cause.printStackTrace();
       //发生异常以后关闭channel,随后从channelgroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());

    }
}
