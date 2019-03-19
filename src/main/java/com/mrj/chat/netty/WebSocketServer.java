package com.mrj.chat.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

@Component
public class WebSocketServer {
    //保障获得的线程是单例的
    private static class SingletionWSServer{
        static final WebSocketServer instance = new WebSocketServer();
    }

    public static WebSocketServer getInstance(){
        return SingletionWSServer.instance;
    }

    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    public WebSocketServer(){
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();
        server.group(mainGroup,subGroup).channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitializer());
    }

    public void srart(){
        this.future = server.bind(8088);
        System.err.println("netty websocket server 启动成功");
    }

    public static void main(String[] args){
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup subGroup = new NioEventLoopGroup();
        try {

            ServerBootstrap server = new ServerBootstrap();
            server.group(mainGroup,subGroup).channel(NioServerSocketChannel.class)
                    .childHandler(null);

            ChannelFuture sync = server.bind(8088).sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            mainGroup.shutdownGracefully();
            subGroup.shutdownGracefully();
        }

    }

}
