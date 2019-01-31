package com.arloor.proxyserver.requestdecoder;

import com.arloor.proxycommon.Handler.HttpMessageDecoder;
import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpRequestDecoder extends HttpMessageDecoder {

    static Logger logger= LoggerFactory.getLogger(DefaultHttpRequestDecoder.class);
    @Override
    public void processRequest(ChannelHandlerContext ctx, HttpRequest request) {
        logger.info("处理请求："+request );
        ctx.fireChannelRead(request);
    }
}
