/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.arloor.forwardproxy;

import com.arloor.forwardproxy.vo.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.Net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class HttpProxyConnectHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static byte[] favicon=null;

    static {
        try (InputStream stream = HttpProxyServer.class.getClassLoader().getResourceAsStream("favicon.ico")){
            byte b[] = new byte[6518];
            int len = 0;
            int temp = 0; //全部读取的内容都使用temp接收
            while ((temp = stream.read()) != -1) { //当没有读取完时，继续读取
                b[len] = (byte) temp;
                len++;
            } stream .close();
            favicon=Arrays.copyOf(b,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(HttpProxyConnectHandler.class);
    private final String basicAuth;

    public HttpProxyConnectHandler(String basicAuth) {
        this.basicAuth = basicAuth;
    }

    private final Bootstrap b = new Bootstrap();

    private String host;
    private int port;
    private HttpRequest request;
    private ArrayList<HttpContent> contents = new ArrayList<>();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) {


        if (msg instanceof HttpRequest) {
            final HttpRequest req = (HttpRequest) msg;
            request = req;
            String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
            //获取Host和port
            String hostAndPortStr = req.headers().get("Host");
            if (hostAndPortStr == null) {
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
            String[] hostPortArray = hostAndPortStr.split(":");
            host = hostPortArray[0];
            String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
            port = Integer.parseInt(portStr);
//          压测时不打印日志，防止IOwait占用cpu时间
            log.info(clientHostname + " " + req.method() + " " + req.uri() + "  {" + host + "}");
        } else {
            //SimpleChannelInboundHandler会将HttpContent中的bytebuf Release，但是这个还会转给relayHandler，所以需要在这里预先retain
            ((HttpContent) msg).content().retain();
            contents.add((HttpContent) msg);
            //一个完整的Http请求被收到，开始处理该请求
            if (msg instanceof LastHttpContent) {
                // bugfix:当且仅当为connect请求时，暂停读，防止跟随的内容被忽略
//                if (request.method().equals(HttpMethod.CONNECT)) {
//                    ctx.channel().config().setAutoRead(false);
//                }
                // 1. 如果url以 / 开头，则认为是直接请求，而不是代理请求
                if (request.uri().startsWith("/")) {
                    String hostName = "";
                    SocketAddress socketAddress = ctx.channel().remoteAddress();
                    if (socketAddress instanceof InetSocketAddress) {
                        hostName = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
                    }

                    if(request.uri().equals("/favicon.ico")){
                        ByteBuf buffer = ctx.alloc().buffer();
                        buffer.writeBytes(favicon);
                        final FullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                        response.headers().set("Server", "netty");
                        response.headers().set("Content-Length", favicon.length);
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }else if (OsHelper.os.equals(OsHelper.OS.Unix)){
                        String html = NetStats.html();
                        ByteBuf buffer = ctx.alloc().buffer();
                        buffer.writeBytes(html.getBytes());
                        final FullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                        response.headers().set("Server", "netty");
                        response.headers().set("Content-Length", html.getBytes().length);
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }else {
                        final FullHttpResponse response = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(hostName.getBytes()));
                        response.headers().set("Server", "netty");
                        response.headers().set("Content-Length", hostName.getBytes().length);
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }
                    // 这里需要将content全部release
                    contents.forEach(ReferenceCountUtil::release);
                    return;
                }

                //2. 检验auth
                if (basicAuth != null) {
                    String requestBasicAuth = request.headers().get("Proxy-Authorization");
//                    request.headers().forEach(System.out::println);
                    if (requestBasicAuth == null || !requestBasicAuth.equals(basicAuth)) {
                        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostName();
                        log.warn(clientHostname + " " + request.method() + " " + request.uri() + "  {" + host + "} wrong_auth:{" + requestBasicAuth + "}");
                        // 这里需要将content全部release
                        contents.forEach(ReferenceCountUtil::release);
                        DefaultHttpResponse responseAuthRequired;
                        if (Config.ask4Authcate && !request.method().equals(HttpMethod.OPTIONS) && !request.method().equals(HttpMethod.HEAD)) {
                            responseAuthRequired = new DefaultHttpResponse(request.protocolVersion(), PROXY_AUTHENTICATION_REQUIRED);
                            responseAuthRequired.headers().add("Proxy-Authenticate", "Basic realm=\"netty forwardproxy\"");
                        } else {
                            responseAuthRequired = new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR);
                        }
                        ctx.channel().writeAndFlush(responseAuthRequired);
                        SocksServerUtils.closeOnFlush(ctx.channel());
                        return;
                    }
                }

                //3. 这里进入代理请求处理，分为两种：CONNECT方法和其他HTTP方法
                Promise<Channel> promise = ctx.executor().newPromise();
                if (request.method().equals(HttpMethod.CONNECT)) {
                    promise.addListener(
                            new FutureListener<Channel>() {
                                @Override
                                public void operationComplete(final Future<Channel> future) throws Exception {
                                    final Channel outboundChannel = future.getNow();
                                    if (future.isSuccess()) {
                                        ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                                                new DefaultHttpResponse(request.protocolVersion(), new HttpResponseStatus(200, "Connection Established")));
                                        responseFuture.addListener(new ChannelFutureListener() {
                                            @Override
                                            public void operationComplete(ChannelFuture channelFuture) {
                                                if (channelFuture.isSuccess()) {
                                                    ctx.pipeline().remove(HttpRequestDecoder.class);
                                                    ctx.pipeline().remove(HttpResponseEncoder.class);
                                                    ctx.pipeline().remove(HttpServerExpectContinueHandler.class);
                                                    ctx.pipeline().remove(HttpProxyConnectHandler.class);
                                                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
//                                                    ctx.channel().config().setAutoRead(true);
                                                } else {
                                                    log.info("reply tunnel established Failed: " + ctx.channel().remoteAddress() + " " + request.method() + " " + request.uri());
                                                    SocksServerUtils.closeOnFlush(ctx.channel());
                                                    SocksServerUtils.closeOnFlush(outboundChannel);
                                                }
                                            }
                                        });
                                    } else {
                                        ctx.channel().writeAndFlush(
                                                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                                        );
                                        SocksServerUtils.closeOnFlush(ctx.channel());
                                    }
                                }
                            });
                } else {
                    promise.addListener(
                            new FutureListener<Channel>() {
                                @Override
                                public void operationComplete(final Future<Channel> future) throws Exception {
                                    final Channel outboundChannel = future.getNow();
                                    if (future.isSuccess()) {
                                        // 这里有几率抛出NoSuchElementException，原因是连接target host完成时，客户端已经关闭连接。
                                        // 考虑到是比较小的几率，不catch。注：该异常没有啥影响。
                                        ctx.pipeline().remove(HttpProxyConnectHandler.this);
                                        ctx.pipeline().remove(HttpResponseEncoder.class);
                                        outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                                        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                        RelayHandler clientEndtoRemoteHandler = new RelayHandler(outboundChannel);
                                        ctx.pipeline().addLast(clientEndtoRemoteHandler);
//                                        ctx.channel().config().setAutoRead(true);

                                        //出于未知的原因，不知道为什么fireChannelread不行
                                        clientEndtoRemoteHandler.channelRead(ctx, request);
                                        contents.forEach(content -> {
                                            try {
                                                clientEndtoRemoteHandler.channelRead(ctx, content);
                                            } catch (Exception e) {
                                                log.error("处理非CONNECT方法的代理请求失败！", e);
                                            }
                                        });
                                    } else {
                                        ctx.channel().writeAndFlush(
                                                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                                        );
                                        SocksServerUtils.closeOnFlush(ctx.channel());
                                    }
                                }
                            });
                }


                // 4.连接目标网站
                final Channel inboundChannel = ctx.channel();
                b.group(inboundChannel.eventLoop())
                        .channel(OsHelper.os.socketChannelClazz)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .handler(new DirectClientHandler(promise));

                b.connect(host, port).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            // Connection established use handler provided results
                        } else {
                            // Close the connection if the connection attempt has failed.
                            ctx.channel().writeAndFlush(
                                    new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                            );
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
