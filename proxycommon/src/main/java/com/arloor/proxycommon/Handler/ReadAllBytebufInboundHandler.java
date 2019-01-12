package com.arloor.proxycommon.Handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ReadAllBytebufInboundHandler extends ChannelInboundHandlerAdapter {
    //用于暂时存放读到的Bytebuf直到channelReadComplete
    //在ChannelInactive时会release这个content
    ByteBuf content=PooledByteBufAllocator.DEFAULT.buffer();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf buf=(ByteBuf)msg;
            content.writeBytes(buf);
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if(content.writerIndex()!=0){
            //新建一个pooledBytebuf，传递给下一个handler的channelRead
            ByteBuf allBytebuf=PooledByteBufAllocator.DEFAULT.buffer();
            allBytebuf.writeBytes(content);
            content.clear();
            ctx.fireChannelRead(allBytebuf);
        }
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ReferenceCountUtil.release(content);
        super.channelInactive(ctx);
    }
}
