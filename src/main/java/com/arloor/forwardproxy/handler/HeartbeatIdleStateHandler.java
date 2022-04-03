package com.arloor.forwardproxy.handler;

import com.arloor.forwardproxy.util.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HeartbeatIdleStateHandler extends IdleStateHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatIdleStateHandler.class);

    public HeartbeatIdleStateHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            log.info("close channel {} because of {}", ctx.channel().remoteAddress(), event.state());
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
    }
}
