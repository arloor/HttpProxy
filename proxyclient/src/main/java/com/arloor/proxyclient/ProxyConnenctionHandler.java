package com.arloor.proxyclient;

import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.filter.crypto.handler.CryptoHandler;
import com.arloor.proxycommon.filter.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.filter.crypto.handler.EncryptHandler;
import com.arloor.proxycommon.filter.crypto.utils.CryptoType;
import com.arloor.proxycommon.httpentity.HttpResponse;
import com.arloor.proxycommon.util.ExceptionUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyConnenctionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ProxyConnenctionHandler.class);
    private EventLoopGroup remoteLoopGroup = ClientProxyBootStrap.REMOTEWORKER;

    private SocketChannel localChannel;

    private SocketChannel remoteChannel;

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        logger.warn(ctx.channel() + " 可写性：" + canWrite);
        //流量控制，不允许继续读
        localChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    public ProxyConnenctionHandler(SocketChannel channel) {
        this.localChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext localCtx, Object msg) throws Exception {
        if (remoteChannel == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(remoteLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            remoteChannel = ch;
                            if (ClientProxyBootStrap.crypto) {
                                //如果不是字节取反，则增加分隔符
                                if (CryptoHandler.cryptoType != CryptoType.SIMPLE) {
                                    ch.pipeline().addLast(new DelimiterBasedFrameDecoder(65536, true, true, Unpooled.copiedBuffer(Config.delimiter().getBytes())));
                                    ch.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
                                        @Override
                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                            if (msg instanceof ByteBuf) {
                                                ByteBuf buf = (ByteBuf) msg;
                                                buf.writeBytes(Config.delimiter().getBytes());
                                            }
                                            super.write(ctx, msg, promise);
                                        }
                                    });
                                }
                                ch.pipeline().addLast(new EncryptHandler());
                                ch.pipeline().addLast(new DecryptHandler());
                            }
                            ch.pipeline().addLast(new SendBack2ClientHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect(ClientProxyBootStrap.serverHost, ClientProxyBootStrap.serverPort);
            future.addListener(ChannelFutureListener -> {
                if (future.isSuccess()) {
                    logger.info("连接成功: 到代理服务器");
                    //todo：向chennel写
                    remoteChannel.writeAndFlush(msg).addListener(future1 -> {
                        if (future1.isSuccess()) {
                            logger.info("发送请求 " + ((ByteBuf) msg).writerIndex() + "字节 " + remoteChannel.remoteAddress());
                        } else {
                            logger.warn("向" + remoteChannel.remoteAddress() + "写失败，异常信息如下：");
                            logger.warn(ExceptionUtil.getMessage(future1.cause()));
                        }
                    });
                } else {
                    logger.error("连接失败:  到代理服务器");
                    ByteBuf error503=Unpooled.buffer();
                    error503.writeBytes(HttpResponse.ERROR503());
                    localCtx.writeAndFlush(error503).addListener(future1 -> {
                        localChannel.close();
                    });

                }
            });
        } else {
            logger.info("发送请求 " + ((ByteBuf) msg).writerIndex() + "字节 " + remoteChannel.remoteAddress());
            remoteChannel.writeAndFlush(msg).addListener(future1 -> {
                if (future1.isSuccess()) {
                    logger.info("发送请求 " + ((ByteBuf) msg).writerIndex() + "字节 " + remoteChannel.remoteAddress());
                } else {
                    logger.warn("向" + remoteChannel.remoteAddress() + "写失败，异常信息如下：");
                    logger.warn(ExceptionUtil.getMessage(future1.cause()));
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(ExceptionUtil.getMessage(cause));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (remoteChannel != null && remoteChannel.isActive())
            remoteChannel.close().addListener((future -> {
                logger.info("浏览器关闭连接，因此关闭到代理服务器的连接");
            }));
        super.channelInactive(ctx);
    }

    private class SendBack2ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
            localChannel.writeAndFlush(byteBuf.retain()).addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("返回响应 " + byteBuf.writerIndex() + "字节 " + channelHandlerContext.channel().remoteAddress());
                } else {
                    logger.warn("向" + remoteChannel.remoteAddress() + "写失败，异常信息如下：");
                    logger.warn(ExceptionUtil.getMessage(future.cause()));
                }
            });
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            boolean canWrite = ctx.channel().isWritable();
            logger.warn(ctx.channel() + " 可写性：" + canWrite);
            //流量控制，不允许继续读
            localChannel.config().setAutoRead(canWrite);
            super.channelWritabilityChanged(ctx);
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (localChannel != null && localChannel.isActive())
                localChannel.close().addListener((future -> {
                    logger.info("代理服务器关闭连接，因此关闭到浏览器的连接");
                }));
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error(ExceptionUtil.getMessage(cause));
        }
    }
}
