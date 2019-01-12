package com.arloor.proxycommon.Handler;

import com.arloor.proxycommon.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class AppendDelimiterOutboundHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf=(ByteBuf)msg;
        buf.writeBytes(Config.delimiter().getBytes());
        super.write(ctx, msg, promise);
    }
}
