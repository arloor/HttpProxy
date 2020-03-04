package com.arloor.forwardproxy.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.ByteBuffer;

public class ReadReverseHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuffer) throws Exception {
        ByteBuf buf= PooledByteBufAllocator.DEFAULT.buffer();
//        for (int i = 0; i < byteBuffer.readableBytes(); i++) {
//        }{
//            buf.writeByte(~byteBuffer.readByte());
//        }
        byteBuffer.forEachByte((abyte)->{
            buf.writeByte(~abyte);
            return true;
        });
        channelHandlerContext.fireChannelRead(buf);
    }
}
