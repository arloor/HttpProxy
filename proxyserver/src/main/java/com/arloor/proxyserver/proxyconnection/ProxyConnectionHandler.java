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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        logger.warn(ctx.channel()+" 可写性："+canWrite);
        //流量控制，不允许继续读
        remoteChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    public void channelRead(ChannelHandlerContext localCtx, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        logger.info("处理请求" + "[客户端:" + localChannel.remoteAddress() + "] " + request);
        if (!rejectRequest(request)) {
            if (remoteChannel == null) {
                if (request.getMethod() != null) {
                    establishConnectionAndSend(request, localCtx);
                } else {
                    //可以认为这是非法的不正常的请求，返回一个404响应，省得别人怀疑
                    logger.error("错误的第一次请求，没有指定host serverport，关闭此channel");
                    logger.error("错误content:\n" + new String(request.getRequestBody()));
                    //在这个时候就不要对响应加密了
//                    localChannel.pipeline().removeFirst();
                    ByteBuf byteBuf=Unpooled.buffer();
                    localChannel.writeAndFlush(byteBuf.writeBytes(HttpResponse.ERROR404())).addListener(future -> {
                        localChannel.close();
                    });

                }
            } else {
//                if (close||!remoteWritable.get()){
//                    logger.info("阻塞！已经不可写了，你还要写，你是禽兽吗！妈个鸡");
//                    Thread.sleep(50);
//                }
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
                logger.info("连接成功: " + request.getHost() + ":" + request.getPort() + (request.getMethod().equals(HttpMethod.CONNECT) ? "" : request.getPath()));
                localCtx.pipeline().addLast(Send2RemoteAdpterFactory.create(request.getMethod(), remoteChannel));
                if (request.getMethod().equals(HttpMethod.CONNECT)) {
                    ByteBuf byteBuf=Unpooled.buffer();
                    localChannel.writeAndFlush(byteBuf.writeBytes(HttpResponse.ESTABLISHED())).addListener(future1 -> {
                        if(future1.isSuccess()){
                            logger.info("success：通知隧道建立成功 "+localChannel.remoteAddress());
                        } else {
                            logger.warn("向" + localChannel.remoteAddress() + "写失败，异常信息如下：");
                            logger.warn(ExceptionUtil.getMessage(future1.cause()));
                        }
                    });
                }
                localCtx.fireChannelRead(request);
            } else {
                logger.error("连接失败: " + request.getHost() + ":" + request.getPort() + request.getPath());
                ByteBuf byteBuf=Unpooled.buffer();
                localChannel.writeAndFlush(byteBuf.writeBytes(HttpResponse.ERROR503())).addListener((localFuture -> {
                    localChannel.close();
                }));
            }
        });
    }


    /**
     * 当本channel被关闭时，及时关闭对应的另一个channel
     * 出现异常和正常关闭都会导致本channel被关闭
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        logger.info(""+ctx.channel().bytesBeforeWritable());
//        logger.info(""+ctx.channel().isWritable());
//        logger.info(""+localWritable.get());
        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.close().addListener(future -> {
                logger.info("browser关闭连接，因此关闭到webserver连接");
            });
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("出现异常：" + cause.getMessage() + "——" + ctx.channel().remoteAddress());
        logger.error(ExceptionUtil.getMessage(cause));
    }

    private class SendBack2ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private Logger logger = LoggerFactory.getLogger(SendBack2ClientHandler.class);

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            boolean canWrite = ctx.channel().isWritable();
            logger.warn(ctx.channel()+" 可写性："+canWrite);
            //流量控制，不允许继续读
            localChannel.config().setAutoRead(canWrite);
            super.channelWritabilityChanged(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
//           if (close||!localWritable.get()){
//               logger.info("阻塞！已经不可写了，你还要写，你是禽兽吗！妈个鸡");
//               Thread.sleep(50);
//           }
            localChannel.writeAndFlush(byteBuf.retain()).addListener(future -> {
                if(future.isSuccess()){
                    logger.info("返回响应 " + byteBuf.writerIndex() + "字节 " + channelHandlerContext.channel().remoteAddress());
                } else {
                    logger.warn("向" + channelHandlerContext.channel() + "写失败，异常信息如下：");
                    logger.warn(ExceptionUtil.getMessage(future.cause()));
                }

            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("出现异常：" + cause.getMessage() + "——" + ctx.channel());
            logger.error(ExceptionUtil.getMessage(cause));
        }

        /**
         * 当本channel被关闭时，及时关闭对应的另一个channel
         * 出现异常和正常关闭都会导致本channel被关闭
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (localChannel != null && localChannel.isActive()) {
                localChannel.close().addListener(future -> {
                    logger.info("webserver关闭连接，因此关闭到browser连接");
                });
            }
            super.channelInactive(ctx);
        }
    }

    /**
     * 不代理某些网站，原因懂的
     *
     * @param request
     * @return
     */
    private boolean rejectRequest(HttpRequest request) {
        if (request.getHost() == null) {
            return false;
        }
        for (String rejectHost : rejectHosts
        ) {
            if (request.getHost().contains(rejectHost)) {
                logger.info("不代理{}", request.getHost());
                ByteBuf error503=Unpooled.buffer();
                localChannel.writeAndFlush(error503.writeBytes(HttpResponse.ERROR503())).addListener((future -> {
                    localChannel.close();
                }));
                return true;
            }
        }
        return false;
    }
}
