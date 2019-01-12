package com.arloor.proxyclient;

import com.alibaba.fastjson.JSONObject;
import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.Handler.AppendDelimiterOutboundHandler;
import com.arloor.proxycommon.Handler.ReadAllBytebufInboundHandler;
import com.arloor.proxycommon.crypto.handler.CryptoHandler;
import com.arloor.proxycommon.crypto.handler.DecryptHandler;
import com.arloor.proxycommon.crypto.handler.EncryptHandler;
import com.arloor.proxycommon.crypto.utils.CryptoType;
import com.arloor.proxycommon.httpentity.HttpMethod;
import com.arloor.proxycommon.httpentity.HttpResponse;
import com.arloor.proxycommon.util.ExceptionUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;


public class ProxyConnenctionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ProxyConnenctionHandler.class);
    private EventLoopGroup remoteLoopGroup = ClientProxyBootStrap.REMOTEWORKER;
    private SocketChannel localChannel;
    private SocketChannel remoteChannel;
    private String host = null;
    private int port=80;
    private boolean isTunnel = false;

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
                        ch.pipeline().addLast(new AppendDelimiterOutboundHandler());
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,true,true, Unpooled.wrappedBuffer(Config.delimiter().getBytes())));
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
        if(msg instanceof JSONObject){
            JSONObject object=(JSONObject)msg;
            //设置一些信息
            if(host==null&&object.containsKey("host")){
                host=object.getString("host");
                port=object.getInteger("port");
                if(object.containsKey("method")){
                    isTunnel=HttpMethod.CONNECT.toString().equals(object.getString("method"));
                }
            }
            //检查这个请求是否有效
            if(host!=null){
                //有效则向代理服务器发送
                //将jsonString转成PooledBytebuf。记得release
                ByteBuf buf= PooledByteBufAllocator.DEFAULT.buffer();
                buf.writeBytes(object.toJSONString().getBytes(UTF_8));
                remoteChannel.writeAndFlush(buf).addListener(future -> {
                    if(future.isSuccess()){
                        logger.info("向代理服务器发送请求成功。host :"+host);
                    }else {
                        logger.info(ExceptionUtil.getMessage(future.cause()));
                    }
                    //这里竟然不需要release
//                    ReferenceCountUtil.release(buf);
                });
            }else{
                ByteBuf buf=PooledByteBufAllocator.DEFAULT.buffer();
                localChannel.writeAndFlush(buf.writeBytes(HttpResponse.ERROR503())).addListener(future -> {
                    logger.warn("错误的第一次请求：没有指定host。关闭channel");
                    ReferenceCountUtil.release(buf);
                });
            }
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
