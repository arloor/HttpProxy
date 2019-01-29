package com.arloor.proxycommon.debug;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class PrintByteBufHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf=(ByteBuf)msg;
        byte[] bytes=new byte[buf.readableBytes()];
        buf.markReaderIndex();
        buf.readBytes(bytes);
        buf.resetReaderIndex();
        System.out.println(new String(bytes));

        super.channelRead(ctx, msg);
    }
}
