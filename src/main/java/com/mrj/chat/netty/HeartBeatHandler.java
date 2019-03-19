package com.mrj.chat.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/*Idle空闲,没有发生相关请求
* 继承ChannelInboundHandlerAdapter,从而不需要实现channelRead0方法
* 用于检测channel的心跳handler
* */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
       //判断evt是否是IdleStateEvent(用于触发用户事件,包括读空闲/写空闲/读写空闲)
        if(evt instanceof IdleStateEvent){
           IdleStateEvent event = (IdleStateEvent)evt;
           //如果是读空闲
           if(event.state() == IdleState.READER_IDLE){
               System.out.println("进入读空闲");
           }else if (event.state() == IdleState.WRITER_IDLE){
               System.out.println("进入写空闲");
           }else if(event.state() == IdleState.ALL_IDLE){
                System.out.println("进入读写空闲");
                System.out.println("channel关闭前,users的数量是"+ChartHandler.users.size());
                Channel channel = ctx.channel();
                //关闭无用的channel.以防资源浪费
                channel.close();
                int size = ChartHandler.users.size();
                System.out.println("channel关闭后.users的数量是"+size);
            }
        }
    }
}
