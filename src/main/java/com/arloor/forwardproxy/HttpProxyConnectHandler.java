package com.arloor.forwardproxy;

import com.arloor.forwardproxy.util.OsHelper;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.Config;
import com.arloor.forwardproxy.web.Dispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
import java.util.ArrayList;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;

public class HttpProxyConnectHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger log = LoggerFactory.getLogger(HttpProxyConnectHandler.class);
    private final Map<String, String> auths;

    public HttpProxyConnectHandler(Map<String, String> auths) {
        this.auths = auths;
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
            request = (HttpRequest) msg;
            setHostPort(ctx);
        } else {
            //SimpleChannelInboundHandler会将HttpContent中的bytebuf Release，但是这个还会转给relayHandler，所以需要在这里预先retain
            ((HttpContent) msg).content().retain();
            contents.add((HttpContent) msg);
            //一个完整的Http请求被收到，开始处理该请求
            if (msg instanceof LastHttpContent) {
                String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
                // bugfix:当且仅当为connect请求时，暂停读，防止跟随的内容被忽略
//                if (request.method().equals(HttpMethod.CONNECT)) {
//                    ctx.channel().config().setAutoRead(false);
//                }
                // 1. 如果url以 / 开头，则认为是直接请求，而不是代理请求
                if (request.uri().startsWith("/")) {
                    Dispatcher.handle(request, ctx);
                    // 这里需要将content全部release
                    contents.forEach(ReferenceCountUtil::release);
                    return;
                }

                //2. 检验auth
                String basicAuth = request.headers().get("Proxy-Authorization");
                String userName = getUserName(basicAuth, auths);
                if (auths != null && auths.size() != 0) {
                    if (basicAuth == null || !auths.containsKey(basicAuth)) {
                        log.warn(clientHostname + " " + request.method() + " " + request.uri() + "  {" + host + "} wrong_auth:{" + basicAuth + "}");
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
                log.info("{}@{} ==> {} {} {}", userName, clientHostname, request.method(), request.uri(), !request.uri().equals(request.headers().get("Host")) ? "Host=" + request.headers().get("Host") : "");
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
                        .channel(OsHelper.socketChannelClazz())
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

    private String getUserName(String basicAuth, Map<String, String> auths) {
        String userName = "nouser";
        if (basicAuth != null && basicAuth.length() != 0) {
            String raw = auths.get(basicAuth);
            if (raw != null && raw.length() != 0) {
                userName = raw.split(":")[0];
            }
        }
        return userName;
    }

    /**
     * 从httprequest中寻找host和port
     * 由于不同的httpclient实现不一样，可能会有不兼容
     * 已知不兼容：
     * idea2019.3设置的http proxy: 传的Host请求头没有带上端口，因此需要以request.uri()为准 CONNECT www.google.com:443 Host=www.google.com
     * ubuntu的apt设置的代理，request.uri()为代理的地址，因此需要以Host请求头为准 CONNECT mirrors.tuna.tsinghua.edu.cn:443 Host=localhost:3128
     * 很坑。。
     *
     * @param ctx
     */
    private void setHostPort(ChannelHandlerContext ctx) {
        String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
        port = Integer.parseInt(portStr);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
