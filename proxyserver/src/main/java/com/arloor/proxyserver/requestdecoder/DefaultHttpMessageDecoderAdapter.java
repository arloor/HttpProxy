package com.arloor.proxyserver.requestdecoder;


import com.arloor.proxycommon.httpentity.HttpRequest;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultHttpMessageDecoderAdapter extends HttpMessageDecoder  {
    private static Logger logger= LoggerFactory.getLogger(DefaultHttpMessageDecoderAdapter.class);

    private void printRequestInfo(HttpRequest request){
        System.out.println("================================================================================");
        System.out.println(request.getRequestLine());
        if(request.getHeaders()!=null){
        for (HttpRequest.HttpRequestHeader header :request.getHeaders()
        ) {
            System.out.println(header.getKey()+": "+header.getValue());
        }}
        System.out.println();
        System.out.println((request.getRequestBody()==null?null:new String(request.getRequestBody(),UTF_8)));
        System.out.println("================================================================================");
    }


    @Override
    void processRequest(ChannelHandlerContext ctx, HttpRequest request) {
        request.reform();
        printRequestInfo(request);
        ctx.fireChannelRead(request);
    }
}
