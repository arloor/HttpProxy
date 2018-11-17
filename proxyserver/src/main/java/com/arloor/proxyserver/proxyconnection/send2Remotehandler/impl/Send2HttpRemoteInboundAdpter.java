package com.arloor.proxyserver.proxyconnection.send2Remotehandler.impl;

import com.arloor.proxycommon.httpentity.HttpRequest;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.Send2RemoteAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Send2HttpRemoteInboundAdpter extends Send2RemoteAdapter {
    private static Logger logger= LoggerFactory.getLogger(Send2HttpRemoteInboundAdpter.class);

    public Send2HttpRemoteInboundAdpter(SocketChannel remoteChannel) {
        super(remoteChannel);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpRequest request) throws Exception {

        remoteChannel.writeAndFlush(parseRequest2Bytes(request)).addListener(ChannelFutureListener->{
            logger.debug("向remote："+remoteChannel.remoteAddress()+"写");
        }).await();

    }

    ByteBuf parseRequest2Bytes(HttpRequest request){
        StringBuffer sb=new StringBuffer();
        if(request.getRequestLine()!=null){
            sb.append(request.getRequestLine());
            sb.append("\r\n");
        }
        List<HttpRequest.HttpRequestHeader> headers=request.getHeaders();
        if(headers!=null){
            for (HttpRequest.HttpRequestHeader header:request.getHeaders()
            ) {
                sb.append(header.getKey());
                sb.append(": ");
                sb.append(header.getValue());
                sb.append("\r\n");
            }
            sb.append("\r\n");
        }else{
//            System.out.print("");
        }
        ByteBuf result=Unpooled.buffer();
        result.writeBytes(sb.toString().getBytes());
        if(request.getRequestBody()!=null){
            result.writeBytes(request.getRequestBody());

        }
        return result;
    }
}
