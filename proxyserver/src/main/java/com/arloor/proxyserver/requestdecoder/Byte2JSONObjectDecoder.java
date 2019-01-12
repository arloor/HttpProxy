package com.arloor.proxyserver.requestdecoder;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.json.JsonObjectDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Byte2JSONObjectDecoder extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String jsonString=new String(bytes,UTF_8);
        System.out.println(jsonString);
        try {
            JSONObject requestJsonObject=JSONObject.parseObject(jsonString);
            ctx.fireChannelRead(requestJsonObject);
        }catch (JSONException e){
            System.out.println("error: "+jsonString);
            throw e;
        }
    }
}
