package com.arloor.proxyserver;


import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.Handler.ReadAllBytebufInboundHandler;
import com.arloor.proxycommon.Handler.length.MyLengthFieldBasedFrameDecoder;
import com.arloor.proxycommon.Handler.length.MyLengthFieldPrepender;
import com.arloor.proxycommon.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.crypto.handler.EncryptHandler;
import com.arloor.proxycommon.debug.PrintByteBufHandler;
import com.arloor.proxyserver.requestdecoder.DefaultHttpRequestDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerProxyBootStrap {

    private static Logger logger= LoggerFactory.getLogger(ServerProxyBootStrap.class);
    private static int port=8080;
    private static boolean crypto =true;

    public final static EventLoopGroup REMOTEWORKER =new NioEventLoopGroup(4);
    private final static  EventLoopGroup BOSS = new NioEventLoopGroup(4);
    private final static EventLoopGroup LOCALWORKER = new NioEventLoopGroup(1);

    public static void main(String[] args){
        ServerProxyBootStrap.crypto = Config.crypto();
        logger.info("当前代理是否进行加密： "+ServerProxyBootStrap.crypto);
        if(ServerProxyBootStrap.crypto){
            logger.info("所采用的加密实现： "+Config.cryptoType());
        }
        try {
            ServerProxyBootStrap.port =Integer.parseInt(Config.serverport());
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
            channel.pipeline().addLast(new ReadAllBytebufInboundHandler());
            //======================
            //length的粘包解决
            channel.pipeline().addLast(new MyLengthFieldBasedFrameDecoder());
            channel.pipeline().addLast(new MyLengthFieldPrepender());
            //================================

            if(crypto){
                channel.pipeline().addLast(new EncryptHandler());
                channel.pipeline().addLast(new DecryptHandler());
            }
//            //打印内容，debug用而已
//            channel.pipeline().addLast(new PrintByteBufHandler());
            channel.pipeline().addLast(new DefaultHttpRequestDecoder());
            channel.pipeline().addLast(new NewProxyConnectionHandler(channel));
        }
    }
}
