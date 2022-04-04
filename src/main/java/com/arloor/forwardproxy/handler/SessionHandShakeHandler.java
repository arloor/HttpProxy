package com.arloor.forwardproxy.handler;

import com.arloor.forwardproxy.session.Session;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;


public class SessionHandShakeHandler extends SimpleChannelInboundHandler<HttpObject> {
    public static final String NAME = "session";
    private static final Logger log = LoggerFactory.getLogger(SessionHandShakeHandler.class);
    private final Session session;

    public SessionHandShakeHandler(Map<String, String> auths, Span streamSpan) {
        this.session = new Session(auths, streamSpan);
    }

//    @Override
//    public void channelReadComplete(ChannelHandlerContext ctx) {
//        ctx.flush();
//    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) {
        session.handle(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
