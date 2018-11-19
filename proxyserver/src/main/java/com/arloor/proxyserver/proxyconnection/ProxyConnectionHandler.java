package com.arloor.proxyserver.proxyconnection;


import com.arloor.proxycommon.httpentity.HttpMethod;
import com.arloor.proxycommon.httpentity.HttpRequest;
import com.arloor.proxycommon.httpentity.HttpResponse;
import com.arloor.proxycommon.util.ExceptionUtil;
import com.arloor.proxyserver.ServerProxyBootStrap;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.factory.Send2RemoteAdpterFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProxyConnectionHandler extends ChannelInboundHandlerAdapter {
    private EventLoopGroup remoteLoopGroup = ServerProxyBootStrap.REMOTEWORKER;

    private SocketChannel localChannel = null;

    private SocketChannel remoteChannel = null;

    private static Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);

    private List<String> rejectHosts = new ArrayList<>();

    public ProxyConnectionHandler(SocketChannel channel) {
        this();
        localChannel = channel;
    }

    private ProxyConnectionHandler() {
//        rejectHosts.add("google");
//        rejectHosts.add("youtube");
//        rejectHosts.add("facebook");
    }

    public void channelRead(ChannelHandlerContext localCtx, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        if (!rejectRequest(request)) {
            if(remoteChannel==null){
                if (request.getMethod() != null) {
                    establishConnectionAndSend(request, localCtx);
                }else {
                    //可以认为这是非法的不正常的请求，返回一个404响应，省得别人怀疑
                    logger.error("错误的第一次请求，没有指定host port，关闭此channel");
                    //在这个时候就不要对响应加密了
                    localChannel.pipeline().removeFirst();
                    localChannel.writeAndFlush(Unpooled.wrappedBuffer(HttpResponse.ERROR404())).addListener(future -> {
                        localChannel.close();
                    });

                }
            }else {
                localCtx.fireChannelRead(request);
            }
        }
    }



    private void establishConnectionAndSend(HttpRequest request, ChannelHandlerContext localCtx) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(remoteLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        remoteChannel = ch;
                        ch.pipeline().addLast(new SendBack2ClientHandler());
                    }
                });
        ChannelFuture future = bootstrap.connect(request.getHost(), request.getPort());
        future.addListener(ChannelFutureListener -> {
            if (future.isSuccess()) {
                logger.info("连接成功: " + request.getHost() + ":" + request.getPort() + (request.getMethod().equals(HttpMethod.CONNECT)?"":request.getPath()));
                localCtx.pipeline().addLast(Send2RemoteAdpterFactory.create(request.getMethod(), remoteChannel));
                if (request.getMethod().equals(HttpMethod.CONNECT)) {
                    localChannel.writeAndFlush(Unpooled.wrappedBuffer(HttpResponse.ESTABLISHED()));
                }
                localCtx.fireChannelRead(request);
            } else {
                logger.error("连接失败: " + request.getHost() + ":" + request.getPort() + request.getPath());
                localChannel.writeAndFlush(Unpooled.wrappedBuffer(HttpResponse.ERROR503())).addListener((localFuture -> {
                    localChannel.close();
                }));
            }
        });
    }


    /**
     * 当本channel被关闭时，及时关闭对应的另一个channel
     * 出现异常和正常关闭都会导致本channel被关闭
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(remoteChannel!=null&&remoteChannel.isActive()){
            remoteChannel.close().addListener(future -> {
                logger.info("browser关闭连接，因此关闭到webserver连接");
            });
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("出现异常："+cause.getMessage()+"——"+ctx.channel().remoteAddress());
        logger.error(ExceptionUtil.getMessage(cause));
    }

    private class SendBack2ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private Logger logger=LoggerFactory.getLogger(SendBack2ClientHandler.class);

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
            localChannel.writeAndFlush(byteBuf.retain()).addListener(ChannelFutureListener -> {
                logger.debug("返回响应" + channelHandlerContext.channel().remoteAddress());
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("出现异常："+cause.getMessage()+"——"+ctx.channel());
            logger.error(ExceptionUtil.getMessage(cause));
        }

        /**
         * 当本channel被关闭时，及时关闭对应的另一个channel
         * 出现异常和正常关闭都会导致本channel被关闭
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
           if(localChannel!=null&&localChannel.isActive()){
               localChannel.close().addListener(future -> {
                   logger.info("webserver关闭连接，因此关闭到browser连接");
               });
           }
            super.channelInactive(ctx);
        }
    }

    /**
     * 不代理某些网站，原因懂的
     * @param request
     * @return
     */
    private boolean rejectRequest(HttpRequest request) {
        if(request.getHost()==null){
            return false;
        }
        for (String rejectHost : rejectHosts
        ) {
            if (request.getHost().contains(rejectHost)) {
                logger.info("不代理{}", request.getHost());
                localChannel.writeAndFlush(Unpooled.wrappedBuffer(HttpResponse.ERROR503())).addListener((future -> {
                    localChannel.close();
                }));
                return true;
            }
        }
        return false;
    }
}
