package com.arloor.forwardproxy.web;

import com.arloor.forwardproxy.HttpProxyServer;
import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.util.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public class Dispatcher {
    private static final Logger log = LoggerFactory.getLogger("web");
    private static byte[] favicon = new byte[0];
    private static Map<String, BiConsumer<HttpRequest, ChannelHandlerContext>> handler = new HashMap<String, BiConsumer<HttpRequest, ChannelHandlerContext>>() {{
        put("/favicon.ico", Dispatcher::favicon);
        put("/", Dispatcher::index);
        put("/net", Dispatcher::net);
        put("/metrics", Dispatcher::metrics);
    }};

    private static void metrics(HttpRequest httpRequest, ChannelHandlerContext ctx) {
        String html = "# HELP testA_total aa\n" +
                "# TYPE testA_total counter\n" +
                "testA_total{a=\"a\",b=\"b\",} 2203.0\n" +
                "# HELP testA_created aa\n" +
                "# TYPE testA_created gauge\n" +
                "testA_created{a=\"a\",b=\"b\",} 1.61175989861E9";
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(html.getBytes());
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    static {
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(HttpProxyServer.class.getClassLoader().getResourceAsStream("favicon.ico")))) {
            byte[] bytes = new byte[stream.available()];
            int read = stream.read(bytes);
            favicon = bytes;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("缺少favicon.ico");
        }
    }

    public static void handle(HttpRequest request, ChannelHandlerContext ctx) {
        log(request, ctx);
        handler.getOrDefault(request.uri(), Dispatcher::other).accept(request, ctx);
    }

    private static void other(HttpRequest request, ChannelHandlerContext ctx) {
        String notFound = "404 not found";
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(notFound.getBytes());
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, NOT_FOUND, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", notFound.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void index(HttpRequest request, ChannelHandlerContext ctx) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(clientHostname.getBytes());
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", clientHostname.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void net(HttpRequest request, ChannelHandlerContext ctx) {
        String html = GlobalTrafficMonitor.html();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(html.getBytes());
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private static void favicon(HttpRequest request, ChannelHandlerContext ctx) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(favicon);
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", favicon.length);
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private static final void log(HttpRequest request, ChannelHandlerContext ctx) {
        //获取Host和port
        String hostAndPortStr = request.headers().get("Host");
        if (hostAndPortStr == null) {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("{} {} {} {}", clientHostname, request.method(), request.uri(), String.format("{%s:%s}", host, port));
    }
}
