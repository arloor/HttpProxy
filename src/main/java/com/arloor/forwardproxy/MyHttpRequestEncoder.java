package com.arloor.forwardproxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequestEncoder;

import java.util.List;
import java.util.stream.Collectors;

public class MyHttpRequestEncoder extends HttpRequestEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        super.encode(ctx, msg, out);
        System.out.println(out);
        out=out.stream().filter(cell->false).collect(Collectors.toList());
    }
}
