package com.arloor.proxyserver;


import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.Handler.AppendDelimiterOutboundHandler;
import com.arloor.proxycommon.Handler.ReadAllBytebufInboundHandler;
import com.arloor.proxycommon.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.crypto.handler.EncryptHandler;
import com.arloor.proxyserver.requestdecoder.Byte2JSONObjectDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
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
            channel.pipeline().addLast(new AuthVerifyInboundhandler());
            channel.pipeline().addLast(new AppendDelimiterOutboundHandler());
            channel.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,true,true, Unpooled.wrappedBuffer(Config.delimiter().getBytes())));
            if(crypto){
                channel.pipeline().addLast(new EncryptHandler());
                channel.pipeline().addLast(new DecryptHandler());
            }
            channel.pipeline().addLast(new Byte2JSONObjectDecoder());
//            channel.pipeline().addLast(new DefaultHttpMessageDecoderAdapter());
            channel.pipeline().addLast(new NewProxyConnectionHandler(channel));
        }
    }
}
