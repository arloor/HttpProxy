package com.arloor.forwardproxy;

import com.arloor.forwardproxy.handler.HttpProxyServerInitializer;
import com.arloor.forwardproxy.handler.HttpsProxyServerInitializer;
import com.arloor.forwardproxy.util.OsUtils;
import com.arloor.forwardproxy.vo.Config;
import com.arloor.forwardproxy.vo.HttpConfig;
import com.arloor.forwardproxy.vo.SslConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class HttpProxyServer {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyServer.class);

    public static void main(String[] args) {
        String propertiesPath = null;
        if (args.length == 2 && args[0].equals("-c")) {
            propertiesPath = args[1];
        }
        Properties properties = parseProperties(propertiesPath);

        Config config = Config.parse(properties);
        log.info("主动要求验证：" + Config.ask4Authcate);
        SslConfig sslConfig = config.ssl();
        HttpConfig httpConfig = config.http();

        EventLoopGroup bossGroup = OsUtils.buildEventLoopGroup(1);
        EventLoopGroup workerGroup = OsUtils.buildEventLoopGroup(0);
        try {
            if (sslConfig != null && httpConfig != null) {
                Channel sslChannel = startSSl(bossGroup, workerGroup, sslConfig);
                Channel httpChannel = startHttp(bossGroup, workerGroup, httpConfig);
                if (httpChannel != null) {
                    httpChannel.closeFuture().sync();
                }
                if (sslChannel != null) {
                    sslChannel.closeFuture().sync();
                }
            } else if (sslConfig != null) {
                Channel httpChannel = startSSl(bossGroup, workerGroup, sslConfig);
                if (httpChannel != null) {
                    httpChannel.closeFuture().sync();
                }
            } else if (httpConfig != null) {
                Channel sslChannel = startHttp(bossGroup, workerGroup, httpConfig);
                if (sslChannel != null) {
                    sslChannel.closeFuture().sync();
                }
            }
        } catch (InterruptedException e) {
            log.error("interrupt!", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static Properties parseProperties(String propertiesPath) {
        Properties properties = new Properties();
        try {
            if (propertiesPath != null) {
                properties.load(new FileReader(new File(propertiesPath)));
            } else {
                properties.load(HttpProxyServer.class.getClassLoader().getResourceAsStream("proxy.properties"));
            }
        } catch (Exception e) {
            log.error("loadProperties Error!", e);
        }
        return properties;
    }


    public static Channel startHttp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, HttpConfig httpConfig) {
        try {
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsUtils.serverSocketChannelClazz())
                    .childHandler(new HttpProxyServerInitializer(httpConfig));

            Channel httpChannel = b.bind(httpConfig.getPort()).sync().channel();
            log.info("http proxy@ port=" + httpConfig.getPort() + " auth=" + httpConfig.needAuth());
            return httpChannel;
        } catch (Exception e) {
            log.error("无法启动Http Proxy", e);
        }
        return null;
    }

    public static Channel startSSl(EventLoopGroup bossGroup, EventLoopGroup workerGroup, SslConfig sslConfig) {
        try {
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            HttpsProxyServerInitializer initializer = new HttpsProxyServerInitializer(sslConfig);
            b.group(bossGroup, workerGroup)
                    .channel(OsUtils.serverSocketChannelClazz())
                    .childHandler(initializer);

            Channel sslChannel = b.bind(sslConfig.getPort()).sync().channel();
            // 每天更新一次ssl证书
            sslChannel.eventLoop().scheduleAtFixedRate(() -> {
                log.info("定时重加载ssl证书！");
                initializer.loadSslContext();
            }, 1, 1, TimeUnit.DAYS);
            log.info("https proxy@ port=" + sslConfig.getPort() + " auth=" + sslConfig.needAuth());
            return sslChannel;
        } catch (Exception e) {
            log.error("无法启动Https Proxy", e);
        }
        return null;
    }
}
