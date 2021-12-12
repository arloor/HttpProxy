package com.arloor.forwardproxy;

import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.trace.TraceConstant;
import com.arloor.forwardproxy.trace.Tracer;
import com.arloor.forwardproxy.vo.HttpConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HttpProxyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final HttpConfig httpConfig;

    public HttpProxyServerInitializer(HttpConfig httpConfig) throws IOException, GeneralSecurityException {
        this.httpConfig = httpConfig;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(GlobalTrafficMonitor.getInstance());
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpServerExpectContinueHandler());
        Span streamSpan = Tracer.spanBuilder(TraceConstant.stream.name())
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(TraceConstant.client.name(), ch.remoteAddress().getHostName())
                .startSpan();
        p.addLast(new HttpProxyConnectHandler(httpConfig.getAuthMap(), streamSpan));
        p.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);
                streamSpan.end();
            }
        });
    }
}
