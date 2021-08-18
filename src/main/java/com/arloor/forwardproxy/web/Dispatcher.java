package com.arloor.forwardproxy.web;

import com.arloor.forwardproxy.HttpProxyServer;
import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.monitor.MonitorService;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.core.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public class Dispatcher {
    private static final Logger log = LoggerFactory.getLogger("web");
    private static byte[] favicon = new byte[0];
    private static byte[] echarts_min_js = new byte[0];
    private static final MonitorService MONITOR_SERVICE = MonitorService.getInstance();
    private static Map<String, BiConsumer<HttpRequest, ChannelHandlerContext>> handler = new HashMap<String, BiConsumer<HttpRequest, ChannelHandlerContext>>() {{
        put("/favicon.ico", Dispatcher::favicon);
        put("/", Dispatcher::index);
        put("/net", Dispatcher::net);
        put("/metrics", Dispatcher::metrics);
        put("/echarts.min.js", Dispatcher::echarts);
    }};

    private static void echarts(HttpRequest request, ChannelHandlerContext ctx) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(echarts_min_js);
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", echarts_min_js.length);
        if (needClose(request)) {
            response.headers().set(CONNECTION, CLOSE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }

    }

    private static final Map<String, Long> counters = new ConcurrentHashMap<>();

    private static void metrics(HttpRequest httpRequest, ChannelHandlerContext ctx) {
        String html = MONITOR_SERVICE.metrics();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(html.getBytes());
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes().length);
        response.headers().set("Content-Type", "text/text; charset=utf-8");
        ctx.writeAndFlush(response);
    }

    private static boolean needClose(HttpRequest httpRequest) {
        Long counter = counters.computeIfAbsent(httpRequest.uri(), (key) -> 0L);
        counter++;
        if (counter > 10) {
            counter = 0L;
            counters.put(httpRequest.uri(), counter);
            return true;
        } else {
            counters.put(httpRequest.uri(), counter);
            return false;
        }
    }

    static {
        try (InputStream stream = HttpProxyServer.class.getClassLoader().getResourceAsStream("favicon.ico")) {
            String str = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            favicon = str.getBytes(StandardCharsets.UTF_8);
        } catch (Throwable e) {
            log.error("加载favicon失败");
        }

        try (InputStream stream = HttpProxyServer.class.getClassLoader().getResourceAsStream("echarts.min.js")) {
            String str = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            echarts_min_js = str.getBytes(StandardCharsets.UTF_8);
        } catch (Throwable e) {
            log.error("加载echart.min.js失败");
        }
    }

    public static void handle(HttpRequest request, ChannelHandlerContext ctx) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        boolean fromLocalAddress = ((InetSocketAddress) socketAddress).getAddress().isSiteLocalAddress();
        boolean fromLocalHost = ((InetSocketAddress) socketAddress).getAddress().isLoopbackAddress();
        if (fromLocalAddress || fromLocalHost || !Config.ask4Authcate) { //来自局域网或本机，或者无被探测到风险
            log(request, ctx);
            handler.getOrDefault(request.uri(), Dispatcher::other).accept(request, ctx);
        } else {
            refuse(request, ctx);
        }
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

    private static void refuse(HttpRequest request, ChannelHandlerContext ctx) {
        String hostAndPortStr = request.headers().get("Host");
        if (hostAndPortStr == null) {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("refuse!! {} {} {} {}", clientHostname, request.method(), request.uri(), String.format("{%s:%s}", host, port));
        ctx.close();
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
        buffer.writeBytes(html.getBytes(StandardCharsets.UTF_8));
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes(StandardCharsets.UTF_8).length);
        response.headers().set("Content-Type", "text/html; charset=utf-8");
        if (needClose(request)) {
            response.headers().set(CONNECTION, CLOSE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }


    private static void favicon(HttpRequest request, ChannelHandlerContext ctx) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(favicon);
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", favicon.length);
        if (needClose(request)) {
            response.headers().set(CONNECTION, CLOSE);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
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
