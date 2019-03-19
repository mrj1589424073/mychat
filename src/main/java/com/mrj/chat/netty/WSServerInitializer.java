package com.mrj.chat.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WSServerInitializer extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        //websocket基于http协议，所以要有http编解码器
        pipeline.addLast(new HttpServerCodec());
        //对写大数据流的支持
        pipeline.addLast(new ChunkedWriteHandler());
        //对httpMessage进行聚合，聚合成FullHttpRequest或者FullHttpResponse
        //几乎在所有的netty编程中都用到此handler
        pipeline.addLast(new HttpObjectAggregator(1024*64));
    //-------------------以上用于支持http协议-------------------
        //websocet服务器处理的协议，用于指定给客户端链接访问的路由：/ws
        //这个handler会帮我们处理一些繁重复杂的事情，比如握手动作handshaking（close，ping，pong）
        //ping+pong就是一个心跳。对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        //添加一个自定义handler
        pipeline.addLast(new ChartHandler());
        //增加心跳支持开始,
        //针对客户端,如果在一分钟没有向服务端发送读写心跳,主动断开
        //如果是读空闲或者写空闲,不处理
        pipeline.addLast(new IdleStateHandler(8,10,16));
        //自定义的空闲状态监测
        pipeline.addLast(new HeartBeatHandler());
        //增加心跳支持结束

    }
}
