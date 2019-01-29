package com.arloor.proxyclient;

import com.arloor.proxycommon.Handler.ReadAllBytebufInboundHandler;
import com.arloor.proxycommon.Handler.length.MyLengthFieldBasedFrameDecoder;
import com.arloor.proxycommon.Handler.length.MyLengthFieldPrepender;
import com.arloor.proxycommon.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.crypto.handler.EncryptHandler;
import com.arloor.proxycommon.httpentity.HttpResponse;
import com.arloor.proxycommon.util.ExceptionUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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

    /**
     * 在ChannelRegistered之后，连接代理服务器
     * 如果成功连接，则允许autoRead。
     * 如果失败，则返回503
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(remoteLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        remoteChannel = ch;
                        ch.pipeline().addLast(new ReadAllBytebufInboundHandler());
                        //======================
                        //length的粘包解决
                        ch.pipeline().addLast(new MyLengthFieldBasedFrameDecoder());
                        ch.pipeline().addLast(new MyLengthFieldPrepender());
                        //=================================
                        if (ClientProxyBootStrap.crypto) {
                            ch.pipeline().addLast(new EncryptHandler());
                            ch.pipeline().addLast(new DecryptHandler());
                        }
                        ch.pipeline().addLast(new SendBack2ClientHandler());
                    }
                });
        bootstrap.connect(ClientProxyBootStrap.serverHost, ClientProxyBootStrap.serverPort).addListener(future -> {
            if (future.isSuccess()) {
                logger.info("连接成功: 到代理服务器,允许读浏览器请求");
                ctx.channel().config().setAutoRead(true);
            } else {
                logger.error("连接失败:  到代理服务器");
                ByteBuf error503 = Unpooled.buffer();
                error503.writeBytes(HttpResponse.ERROR503());
                ctx.writeAndFlush(error503).addListener(future1 -> {
                    ctx.channel().close();
                });
            }
        });
        super.channelRegistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext localCtx, Object msg) throws Exception {
        remoteChannel.writeAndFlush(msg).addListener(future -> {
            if (future.isSuccess()) {
                logger.info("向代理服务器发送请求成功。");
            } else {
                logger.info("向代理服务器发送请求失败。异常如下：");
                logger.info(ExceptionUtil.getMessage(future.cause()));
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(ExceptionUtil.getMessage(cause));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (remoteChannel != null && remoteChannel.isActive())
            remoteChannel.writeAndFlush(PooledByteBufAllocator.DEFAULT.buffer()).addListener(future -> {
                remoteChannel.close().addListener((future1 -> {
                    logger.info("返回 0字节：浏览器关闭连接，因此关闭到代理服务器的连接");
                }));
            });
        super.channelInactive(ctx);
    }


    private class SendBack2ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
            if (byteBuf.readableBytes() > 0)//会返回0字节，这个就不返回了 返回0字节的原因是，关闭连接的事件前，会写0字节
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
                localChannel.writeAndFlush(PooledByteBufAllocator.DEFAULT.buffer()).addListener(future -> {
                    localChannel.close().addListener(future1 -> {
                        logger.info("返回0字节：代理服务器关闭连接，因此关闭到browser连接");
                    });
                });
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error(ExceptionUtil.getMessage(cause));
        }
    }
}
