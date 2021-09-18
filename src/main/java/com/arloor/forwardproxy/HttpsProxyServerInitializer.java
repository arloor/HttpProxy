package com.arloor.forwardproxy;

import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.ssl.SslContextFactory;
import com.arloor.forwardproxy.vo.SslConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpsProxyServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger log = LoggerFactory.getLogger(HttpsProxyServerInitializer.class);

    private final SslConfig sslConfig;

    private SslContext sslCtx;

    public HttpsProxyServerInitializer(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
        loadSslContext();
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
        p.addLast(new HttpProxyConnectHandler(sslConfig.getAuthMap()));

    }

    public void loadSslContext() {
        try {
            this.sslCtx = SslContextFactory.getSSLContext(sslConfig.getFullchain(), sslConfig.getPrivkey());
        } catch (Throwable e) {
            log.error("init ssl context error!", e);
        }
    }
}
