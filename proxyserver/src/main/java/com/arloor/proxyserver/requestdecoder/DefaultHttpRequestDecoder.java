package com.arloor.proxyserver.requestdecoder;

import com.arloor.proxycommon.Handler.HttpMessageDecoder;
import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.channel.ChannelHandlerContext;

public class DefaultHttpRequestDecoder extends HttpMessageDecoder {
    @Override
    public void processRequest(ChannelHandlerContext ctx, HttpRequest request) {
        ctx.fireChannelRead(request);
    }
}
