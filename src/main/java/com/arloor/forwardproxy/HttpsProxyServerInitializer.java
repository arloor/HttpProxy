package com.arloor.forwardproxy;

import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.ssl.SslContextFactory;
import com.arloor.forwardproxy.vo.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HttpsProxyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Config.Ssl ssl;

    private final SslContext sslCtx;

    public HttpsProxyServerInitializer(Config.Ssl ssl) throws IOException, GeneralSecurityException {
        this.ssl = ssl;
        this.sslCtx=SslContextFactory.getSSLContext(ssl.getFullchain(),ssl.getPrivkey());
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(GlobalTrafficMonitor.getInstance());
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpServerExpectContinueHandler());
//        p.addLast(new LoggingHandler(LogLevel.INFO));
        p.addLast(new HttpProxyConnectHandler(ssl.getAuthMap()));

    }
}
