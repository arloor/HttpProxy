package com.arloor.forwardproxy;

import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.vo.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HttpProxyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Config.Http http;

    public HttpProxyServerInitializer(Config.Http http) throws IOException, GeneralSecurityException {
        this.http = http;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(GlobalTrafficMonitor.getInstance());
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new HttpProxyConnectHandler(http.getAuthMap()));
    }
}
