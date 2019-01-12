package com.arloor.proxyserver.proxyconnection.send2Remotehandler.impl;


import com.arloor.proxycommon.httpentity.HttpRequest;
import com.arloor.proxycommon.util.ExceptionUtil;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.Send2RemoteAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Send2HttpsRemoteInboundAdpter extends Send2RemoteAdapter {
    private static Logger logger= LoggerFactory.getLogger(Send2HttpRemoteInboundAdpter.class);

    public Send2HttpsRemoteInboundAdpter(SocketChannel remoteChannel) {
        super(remoteChannel);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpRequest request) throws Exception {
        if(request.getRequestBody()!=null&&request.getRequestBody().length!=0)
        remoteChannel.writeAndFlush(parseRequest2Bytes(request)).addListener(channelFuture->logger.debug("向remote："+remoteChannel+"写"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(ExceptionUtil.getMessage(cause));
    }

    ByteBuf parseRequest2Bytes(HttpRequest request){
        ByteBuf result= Unpooled.buffer();
        result.writeBytes(request.getRequestBody());
        return result;
    }
}
