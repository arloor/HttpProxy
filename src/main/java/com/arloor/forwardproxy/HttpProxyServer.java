package com.arloor.forwardproxy;

import com.arloor.forwardproxy.util.OsHelper;
import com.arloor.forwardproxy.vo.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;


/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
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
        Config.Ssl ssl = config.ssl();
        Config.Http http = config.http();

        EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
        EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);


        try {
            if (ssl != null && http != null) {
                Channel sslChannel = startSSl(bossGroup, workerGroup, ssl);
                Channel httpChannel = startHttp(bossGroup, workerGroup, http);
                if (httpChannel != null) {
                    httpChannel.closeFuture().sync();
                }
                if (sslChannel != null) {
                    sslChannel.closeFuture().sync();
                }
            } else if (ssl != null) {
                Channel httpChannel = startSSl(bossGroup, workerGroup, ssl);
                if (httpChannel != null) {
                    httpChannel.closeFuture().sync();
                }
            } else if (http != null) {
                Channel sslChannel = startHttp(bossGroup, workerGroup, http);
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


    private static Channel startHttp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Config.Http http) {
        try {
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new HttpProxyServerInitializer(http));

            Channel httpChannel = b.bind(http.getPort()).sync().channel();
            log.info("http proxy@ port=" + http.getPort() + " auth=" + http.needAuth());
            return httpChannel;
        } catch (Exception e) {
            log.error("无法启动Http Proxy", e);
        }
        return null;
    }

    private static Channel startSSl(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Config.Ssl ssl) {
        try {
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new HttpsProxyServerInitializer(ssl));

            Channel sslChannel = b.bind(ssl.getPort()).sync().channel();
            log.info("https proxy@ port=" + ssl.getPort() + " auth=" + ssl.needAuth());
            return sslChannel;
        } catch (Exception e) {
            log.error("无法启动Https Proxy", e);
        }
        return null;
    }
}
