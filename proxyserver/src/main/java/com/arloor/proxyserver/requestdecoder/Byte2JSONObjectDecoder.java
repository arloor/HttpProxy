package com.arloor.proxyserver.requestdecoder;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Byte2JSONObjectDecoder extends SimpleChannelInboundHandler<ByteBuf> {
    private static Logger logger= LoggerFactory.getLogger(Byte2JSONObjectDecoder.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String jsonString=new String(bytes,UTF_8);
        logger.debug("请求如下： "+jsonString);
        try {
            JSONObject requestJsonObject=JSONObject.parseObject(jsonString);
            ctx.fireChannelRead(requestJsonObject);
        }catch (JSONException e){
            System.out.println("error parse: "+jsonString);
            throw e;
        }
    }
}
