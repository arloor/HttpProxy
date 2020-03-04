package com.arloor.forwardproxy.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class WriteReverseHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf= PooledByteBufAllocator.DEFAULT.buffer();
//        for (int i = 0; i < byteBuffer.readableBytes(); i++) {
//        }{
//            buf.writeByte(~byteBuffer.readByte());
//        }
        ((ByteBuf)msg).forEachByte((abyte)->{
            buf.writeByte(~abyte);
            return true;
        });
        ReferenceCountUtil.release(msg);
        super.write(ctx,buf,promise);
    }
}
