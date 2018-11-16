package com.arloor.proxyserver.requestdecoder;


import com.arloor.proxycommon.entity.HttpRequest;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        System.out.println("bodysize: "+(request.getRequestBody()==null?0:request.getRequestBody().length));
        System.out.println("================================================================================");
    }


    @Override
    void processRequest(ChannelHandlerContext ctx, HttpRequest request) {
        logger.info("处理请求 " +request);
        request.reform();
//        printRequestInfo(request);
        ctx.fireChannelRead(request);
    }
}
