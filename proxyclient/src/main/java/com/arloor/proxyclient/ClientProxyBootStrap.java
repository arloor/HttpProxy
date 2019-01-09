package com.arloor.proxyclient;

import com.arloor.proxycommon.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientProxyBootStrap {

    private static Logger logger= LoggerFactory.getLogger(ClientProxyBootStrap.class);
    private static  int clientPort =8081;
    public static  int serverPort =8081;
    public static  String serverHost ="127.0.0.1";
    public static boolean crypto=true;

    public final static EventLoopGroup REMOTEWORKER =new NioEventLoopGroup(4);
    private final static  EventLoopGroup BOSS = new NioEventLoopGroup(4);
    private final static EventLoopGroup LOCALWORKER = new NioEventLoopGroup(1);

    public static void main(String[] args){
        ClientProxyBootStrap.crypto = Config.crypto();
        logger.info("当前代理是否进行加密： "+ClientProxyBootStrap.crypto);
        if(ClientProxyBootStrap.crypto){
            logger.info("所采用的加密实现： "+Config.cryptoType());
        }
        String clientPort=Config.clientPort();
        try {
            ClientProxyBootStrap.clientPort =Integer.parseInt(clientPort);
        }catch (Exception e){
            logger.error("解析client.port配置失败，设置为默认端口："+ClientProxyBootStrap.clientPort);
        }
        ClientProxyBootStrap.serverHost =Config.serverhost();
        String serverport=Config.serverport();
        try {
            ClientProxyBootStrap.serverPort =Integer.parseInt(serverport);
        }catch (Exception e){
            logger.error("server.port配置失败，设置为默认端口："+ClientProxyBootStrap.serverPort);
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(BOSS, LOCALWORKER)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(MyChannelInitializer.instance);
            ChannelFuture future = bootstrap.bind(ClientProxyBootStrap.clientPort).sync();
            logger.info("开启代理 端口:"+ClientProxyBootStrap.clientPort);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            BOSS.shutdownGracefully();
            LOCALWORKER.shutdownGracefully();
            REMOTEWORKER.shutdownGracefully();
        }
    }

    private static class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

        private static MyChannelInitializer instance=new MyChannelInitializer();

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline().addLast(new ProxyConnenctionHandler(channel));
        }
    }
}
