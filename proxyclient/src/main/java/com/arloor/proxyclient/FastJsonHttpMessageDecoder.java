package com.arloor.proxyclient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arloor.proxycommon.Handler.HttpMessageDecoder;
import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.channel.ChannelHandlerContext;

public class FastJsonHttpMessageDecoder extends HttpMessageDecoder {
    @Override
    public void processRequest(ChannelHandlerContext ctx, HttpRequest request) {
        request.reform();
        JSONObject object=JSONObject.parseObject(JSON.toJSONString(request));
        ctx.fireChannelRead(object);
    }
}

