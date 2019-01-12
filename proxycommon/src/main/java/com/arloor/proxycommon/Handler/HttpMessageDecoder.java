package com.arloor.proxycommon.Handler;

;
import com.arloor.proxycommon.httpentity.HttpRequest;
import com.arloor.proxycommon.util.ExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.List;

/**
 * 解码http请求，生成HttpRequest对象。
 */
public abstract class HttpMessageDecoder extends ChannelInboundHandlerAdapter {
    private static Logger logger= LoggerFactory.getLogger(HttpMessageDecoder.class);

    private HttpRequest decodeRequest(ByteBuf msg) {
        byte[] buff = new byte[msg.readableBytes()];
        msg.readBytes(buff);
        if(buff.length<=10){
            HttpRequest request = new HttpRequest();
            request.setRequestBody(buff);
            return request;
        }

        String startStr=new String(buff,0,8);
        if(
                !startStr.startsWith("CONNECT ")&&
                        !startStr.startsWith("POST ")&&
                        !startStr.startsWith("GET ")&&
                        !startStr.startsWith("PUT ")&&
                        !startStr.startsWith("HEAD ")&&
                        !startStr.startsWith("DELETE ")&&
                        !startStr.startsWith("OPTIONS ")&&
                        !startStr.startsWith("TRACE ")
        ) {
            HttpRequest request = new HttpRequest();
            request.setRequestBody(buff);
            return request;
        }

        String msgStr = new String(buff);
        String requsetLine = parseRequestLine(msgStr);
        if(requsetLine==null){
            HttpRequest request = new HttpRequest();
            request.setRequestBody(buff);
            return request;
        }

        List<HttpRequest.HttpRequestHeader> headers = parseRequestHeaders(msgStr);
        int bodyStartIndex = 0;
        int tempIndex = msgStr.indexOf("\r\n\r\n");
        if (tempIndex != -1) {
            bodyStartIndex = tempIndex + 4;
        }
        byte[] requestBody = null;
        if (bodyStartIndex != msgStr.length()) {
            requestBody = parseRequestBody(buff, bodyStartIndex);
        }

        HttpRequest request = new HttpRequest();
        request.setRequestLine(requsetLine);
        request.setHeaders(headers);
        request.setRequestBody(requestBody);
        return request;
    }

    private String parseRequestLine(String msg) {
        int tempIndex = msg.indexOf("\r\n");
        if(tempIndex==-1){
            return null;
        }else {
            String tempStr=msg.substring(0,tempIndex );
           String[] split=tempStr.split(" ");
           if(split.length!=3||!split[2].startsWith("HTTP/")){
               return null;
           }else return tempStr;
        }
        //不使用以下正则表达式，因为在请求体过长的情况下，会出现stackoverflow。。。
        //可以说是很牛逼了。。
//        Pattern pattern=Pattern.compile("(CONNECT|GET|POST|PUT|HEAD|DELETE|OPTIONS|TRACE) (.|\\.)* HTTP/\\d{1}\\.\\d{1}");
//        Matcher m=pattern.matcher(msg);
//        if(m.find()){
//            return m.group(0);
//        }else return null;
    }

    private List<HttpRequest.HttpRequestHeader> parseRequestHeaders(String msg) {
        int tempIndex = msg.indexOf("\r\n");
        int headersStartIndex = 0;
        int headersEndIndex = msg.indexOf("\r\n\r\n");
        if (tempIndex != -1&&headersEndIndex!=-1) {
            headersStartIndex = tempIndex + 2;
        } else {
            return null;
        }
        List<HttpRequest.HttpRequestHeader> httpRequestHeaders=new ArrayList<>();
        try {
            String headersStr = msg.substring(headersStartIndex, headersEndIndex);
            String[] headersSplit = headersStr.split("\r\n");
            for (String headerEntry : headersSplit
            ) {
                String[] lineSplit = headerEntry.split(": ");
                HttpRequest.HttpRequestHeader header=new HttpRequest.HttpRequestHeader();
                header.setKey(lineSplit[0]);

                header.setValue(lineSplit.length == 2 ? lineSplit[1] : "");
                httpRequestHeaders.add(header);
            }
        }catch (Exception e){
            logger.error(ExceptionUtil.getMessage(e));
        }
        return httpRequestHeaders;
    }

    private byte[] parseRequestBody(byte[] buff, int bodyStartIndex) {
        int size = buff.length - bodyStartIndex;
        byte[] body = new byte[size];
        System.arraycopy(buff, bodyStartIndex, body, 0, size);
        return body;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf content=(ByteBuf)msg;
        if (content.writerIndex() != 0) {
            HttpRequest request = decodeRequest(content);
            processRequest(ctx, request);
        }
        ReferenceCountUtil.release(content);
    }

    abstract public void processRequest(ChannelHandlerContext ctx ,HttpRequest request);
}
