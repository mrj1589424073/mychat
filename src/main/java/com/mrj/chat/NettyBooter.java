package com.mrj.chat;

import com.mrj.chat.netty.WebSocketServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        //如果上下文对象是空的，说明服务器没有启动
        if(contextRefreshedEvent.getApplicationContext().getParent() == null){
            try {
                WebSocketServer.getInstance().srart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
