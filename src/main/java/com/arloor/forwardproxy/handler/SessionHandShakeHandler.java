package com.arloor.forwardproxy.handler;

import com.arloor.forwardproxy.session.Session;
import com.arloor.forwardproxy.util.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.timeout.IdleStateEvent;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;


public class SessionHandShakeHandler extends SimpleChannelInboundHandler<HttpObject> {
    public static final String NAME = "session";
    private static final Logger log = LoggerFactory.getLogger(SessionHandShakeHandler.class);
    private final Session session;

    public SessionHandShakeHandler(Map<String, String> auths, Span streamSpan, Set<String> whiteDomains) {
        this.session = new Session(auths, streamSpan, whiteDomains);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) {
        session.handle(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            log.info("close channel {} because of {}", ctx.channel().remoteAddress(), event.state());
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getClass().getSimpleName() + " " + cause.getMessage());
        ctx.close();
    }
}
