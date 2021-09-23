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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        put("/ip", Dispatcher::ip);
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
        response.headers().set("Cache-Control", "max-age=86400");
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
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(HttpProxyServer.class.getClassLoader().getResourceAsStream("favicon.ico")))) {
            favicon = readAll(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("缺少favicon.ico");
        }

        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(HttpProxyServer.class.getClassLoader().getResourceAsStream("echarts.min.js")))) {
            echarts_min_js = readAll(stream);
        } catch (Throwable e) {
            log.error("加载echart.min.js失败");
        }
    }

    public static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
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
        String path = getPath(request);
        final byte[] bytes = ResourceReader.readFile(path);
        if (bytes == null) {
            r404(ctx);
        } else {
            String contentType = getContentType(path);
            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeBytes(bytes);
            final FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
            response.headers().set("Server", "nginx/1.11");
            response.headers().set("Content-Length", bytes.length);
            response.headers().set("Cache-Control", "max-age=1800");
            response.headers().set("Content-Type", contentType + "; charset=utf-8");
            ctx.writeAndFlush(response);
        }
    }

    // 文件后缀与contentType映射见 https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
    private static String getContentType(String path) {
        final int i = path.lastIndexOf(".");
        if (i == -1) return "text/text";
        String end = path.substring(i);
        return switch (end) {
            case ".aac" -> "audio/aac";
            case ".abw" -> "application/x-abiword";
            case ".arc" -> "application/x-freearc";
            case ".avi" -> "video/x-msvideo";
            case ".azw" -> "application/vnd.amazon.ebook";
            case ".bin" -> "application/octet-stream";
            case ".bmp" -> "image/bmp";
            case ".bz" -> "application/x-bzip";
            case ".bz2" -> "application/x-bzip2";
            case ".csh" -> "application/x-csh";
            case ".css" -> "text/css";
            case ".csv" -> "text/csv";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".eot" -> "application/vnd.ms-fontobject";
            case ".epub" -> "application/epub+zip";
            case ".gif" -> "image/gif";
            case ".htm" -> "text/html";
            case ".html" -> "text/html";
            case ".ico" -> "image/vnd.microsoft.icon";
            case ".ics" -> "text/calendar";
            case ".jar" -> "application/java-archive";
            case ".jpeg" -> "image/jpeg";
            case ".jpg" -> "image/jpeg";
            case ".js" -> "text/javascript";
            case ".json" -> "application/json";
            case ".jsonld" -> "application/ld+json";
            case ".mid" -> "audio/midi";
            case ".midi" -> "audio/midi";
            case ".mjs" -> "text/javascript";
            case ".mp3" -> "audio/mpeg";
            case ".mpeg" -> "video/mpeg";
            case ".mpkg" -> "application/vnd.apple.installer+xml";
            case ".odp" -> "application/vnd.oasis.opendocument.presentation";
            case ".ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            case ".odt" -> "application/vnd.oasis.opendocument.text";
            case ".oga" -> "audio/ogg";
            case ".ogv" -> "video/ogg";
            case ".ogx" -> "application/ogg";
            case ".otf" -> "font/otf";
            case ".png" -> "image/png";
            case ".pdf" -> "application/pdf";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".rar" -> "application/x-rar-compressed";
            case ".rtf" -> "application/rtf";
            case ".sh" -> "application/x-sh";
            case ".svg" -> "image/svg+xml";
            case ".swf" -> "application/x-shockwave-flash";
            case ".tar" -> "application/x-tar";
            case ".tif" -> "image/tiff";
            case ".tiff" -> "image/tiff";
            case ".ttf" -> "font/ttf";
            case ".txt" -> "text/plain";
            case ".vsd" -> "application/vnd.visio";
            case ".wav" -> "audio/wav";
            case ".weba" -> "audio/webm";
            case ".webm" -> "video/webm";
            case ".webp" -> "image/webp";
            case ".woff" -> "font/woff";
            case ".woff2" -> "font/woff2";
            case ".xhtml" -> "application/xhtml+xml";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".xml" -> "application/xml";
            case ".xul" -> "application/vnd.mozilla.xul+xml";
            case ".zip" -> "application/zip";
            case ".3gp" -> "video/3gpp";
            case ".3g2" -> "video/3gpp2";
            case ".7z" -> "application/x-7z-compressed";
            default -> "text/text";
        };

    }

    private static String getPath(HttpRequest request) {
        String uri = request.uri();
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        if (uri.endsWith("/")) {
            uri += "index.html";
        }
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        return uri;
    }

    private static void r404(ChannelHandlerContext ctx) {
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

    private static void ip(HttpRequest request, ChannelHandlerContext ctx) {
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
        String html = GlobalTrafficMonitor.html(true);
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
        response.headers().set("Cache-Control", "max-age=86400");
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
