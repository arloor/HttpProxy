package com.arloor.proxyserver;


import com.arloor.proxycommon.filter.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.filter.crypto.handler.EncryptHandler;
import com.arloor.proxyserver.requestdecoder.DefaultHttpMessageDecoderAdapter;
import com.arloor.proxyserver.proxyconnection.ProxyConnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ServerProxyBootStrap {

    private static Logger logger= LoggerFactory.getLogger(ServerProxyBootStrap.class);
    private static int port=8080;
    private static boolean crypto =true;

    public final static EventLoopGroup REMOTEWORKER =new NioEventLoopGroup(4);
    private final static  EventLoopGroup BOSS = new NioEventLoopGroup(4);
    private final static EventLoopGroup LOCALWORKER = new NioEventLoopGroup(1);

    public static void main(String[] args){

        Properties prop = new Properties();
        InputStream in = ServerProxyBootStrap.class.getResourceAsStream("/proxy.properties");
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String crypto=prop.getProperty("crypto","false");

        if(crypto.equals("false")){
            ServerProxyBootStrap.crypto =false;
        }
        logger.info("当前代理是否进行加密： "+ServerProxyBootStrap.crypto);

        String port=prop.getProperty("server.port", "8080");
        try {
            ServerProxyBootStrap.port =Integer.parseInt(port);
        }catch (Exception e){
            logger.error("解析server.port配置失败");
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(BOSS, LOCALWORKER)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(MyChannelInitializer.instance);
            ChannelFuture future = bootstrap.bind(ServerProxyBootStrap.port ).sync();
            logger.info("开启代理 端口:"+ServerProxyBootStrap.port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            BOSS.shutdownGracefully();
            LOCALWORKER.shutdownGracefully();
            REMOTEWORKER.shutdownGracefully();
        }
    }

    private static class MyChannelInitializer extends ChannelInitializer<io.netty.channel.socket.SocketChannel> {

        private static MyChannelInitializer instance=new MyChannelInitializer();

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            if(crypto){
                channel.pipeline().addLast(new EncryptHandler());
                channel.pipeline().addLast(new DecryptHandler());
            }
            channel.pipeline().addLast(new DefaultHttpMessageDecoderAdapter());
            channel.pipeline().addLast(new ProxyConnectionHandler(channel));
        }
    }
}
