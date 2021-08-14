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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HttpsProxyServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger log = LoggerFactory.getLogger(HttpsProxyServerInitializer.class);
    private final Config.Ssl ssl;
    private SslContext sslCtx;

    private long lastLoadTime = 0;
    private static final long INTERVAL = 30 * 24 * 60 * 60 * 1000; //30天

    public HttpsProxyServerInitializer(Config.Ssl ssl) throws IOException, GeneralSecurityException {
        this.ssl = ssl;
        loadSslContextIfNeed();
    }

    private void loadSslContextIfNeed() throws IOException, GeneralSecurityException {
        long now = System.currentTimeMillis();
        if (now - lastLoadTime > INTERVAL || sslCtx == null) { //
            log.info("加载ssl证书 {} {}", ssl.getFullchain(), ssl.getPrivkey());
            this.sslCtx = SslContextFactory.getSSLContext(ssl.getFullchain(), ssl.getPrivkey());
            lastLoadTime = now;
        }
    }

    @Override
    public void initChannel(SocketChannel ch) throws IOException, GeneralSecurityException {
        ChannelPipeline p = ch.pipeline();
        p.addLast(GlobalTrafficMonitor.getInstance());
        loadSslContextIfNeed();
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
