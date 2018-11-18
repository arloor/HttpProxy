package com.arloor.proxyserver.proxyconnection.send2Remotehandler;

import com.arloor.proxycommon.httpentity.HttpRequest;
import com.arloor.proxycommon.util.ExceptionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Send2RemoteAdapter  extends SimpleChannelInboundHandler<HttpRequest> {
    private static Logger logger= LoggerFactory.getLogger(Send2RemoteAdapter.class);
    protected SocketChannel remoteChannel;
    public Send2RemoteAdapter(SocketChannel remoteChannel) {
        this.remoteChannel=remoteChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("出现异常："+cause.getMessage()+"——"+ctx.channel());
        logger.error(ExceptionUtil.getMessage(cause));
    }
}
