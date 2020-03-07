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

import io.netty.bootstrap.Bootstrap;
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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class HttpProxyConnectHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger log= LoggerFactory.getLogger(HttpProxyConnectHandler.class);
    private static final String auth=System.getProperty("auth");
    private static final String basicAuth=(auth==null?null:"Basic "+ Base64.getEncoder().encodeToString(auth.getBytes()));

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
            String clientHostname=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
            //获取Host和port
            String hostAndPortStr = req.headers().get("Host");
            String[] hostPortArray = hostAndPortStr.split(":");
            host = hostPortArray[0];
            String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
            port = Integer.parseInt(portStr);
            log.info(clientHostname+" "+req.method() + " " + req.uri() +"  {"+host+"}");
        } else {
            //SimpleChannelInboundHandler会将HttpContent中的bytebuf Release，但是这个还会转给relayHandler，所以需要在这里预先retain
            ((HttpContent) msg).content().retain();
            contents.add((HttpContent) msg);
            //一个完整的Http请求被收到，开始处理该请求
            if (msg instanceof LastHttpContent) {
                // 1. 如果url不是以http开头，则认为是直接请求，而不是代理请求
                if(request.uri().startsWith("/")){
                    String hostName ="";
                    SocketAddress socketAddress = ctx.channel().remoteAddress();
                    if(socketAddress instanceof InetSocketAddress){
                        hostName = ((InetSocketAddress) socketAddress).getHostName();
                    }

                    final FullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(hostName.getBytes()));
                    response.headers().set("Server", "??????");
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    // 这里需要将content全部release
                    contents.forEach(ReferenceCountUtil::release);
                    return;
                }

                //2. 检验auth
                if(basicAuth!=null){
                    String requestBasicAuth = request.headers().get("Proxy-Authorization");
//                    request.headers().forEach(System.out::println);
                    if(requestBasicAuth==null||!requestBasicAuth.equals(basicAuth)){
                        String clientHostname=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
                        log.warn(clientHostname+" "+request.method() + " " + request.uri() +"  {"+host+"} {"+requestBasicAuth+"}");
                        // 这里需要将content全部release
                        contents.forEach(ReferenceCountUtil::release);
                        ctx.channel().writeAndFlush(
                                new DefaultHttpResponse(request.protocolVersion(), PROXY_AUTHENTICATION_REQUIRED)
                        );
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
                                                new DefaultHttpResponse(request.protocolVersion(), OK));

                                        responseFuture.addListener(new ChannelFutureListener() {
                                            @Override
                                            public void operationComplete(ChannelFuture channelFuture) {
                                                ctx.pipeline().remove(HttpRequestDecoder.class);
                                                ctx.pipeline().remove(HttpResponseEncoder.class);
                                                ctx.pipeline().remove(HttpServerExpectContinueHandler.class);
                                                ctx.pipeline().remove(HttpProxyConnectHandler.class);
                                                outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                                ctx.pipeline().addLast(new RelayHandler(outboundChannel));
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
                                        ctx.pipeline().remove(HttpProxyConnectHandler.this);
                                        ctx.pipeline().remove(HttpResponseEncoder.class);
                                        outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                                        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                        RelayHandler clientEndtoRemoteHandler = new RelayHandler(outboundChannel);
                                        ctx.pipeline().addLast(clientEndtoRemoteHandler);

                                        request.headers().remove("Proxy-Authorization");
                                        String proxyConnection = request.headers().get("Proxy-Connection");
                                        if (Objects.nonNull(proxyConnection)) {
                                            request.headers().set("Connection", proxyConnection);
                                            request.headers().remove("Proxy-Connection");
                                        }
                                        try {
                                            String url = request.uri().split(host)[1];
                                            if (url.startsWith(":" + port)) {
                                                url = url.replace(":" + port, "");
                                            }
                                            request.setUri(url);
                                        } catch (Exception e) {
                                            System.err.println("无法获取url：" + request.uri() + " " + host);
                                        }


                                        //出于未知的原因，不知道为什么fireChannelread不行
                                        clientEndtoRemoteHandler.channelRead(ctx, request);
                                        contents.forEach(content -> {
                                            try {
                                                clientEndtoRemoteHandler.channelRead(ctx, content);
                                            } catch (Exception e) {
                                                System.err.println("????????????");
                                                e.printStackTrace();
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
                        .channel(NioSocketChannel.class)
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
                            //返回500，并关闭连接
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
        cause.printStackTrace();
        ctx.close();
    }
}
