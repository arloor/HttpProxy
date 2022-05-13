package com.arloor.forwardproxy.session;

import com.arloor.forwardproxy.handler.RelayHandler;
import com.arloor.forwardproxy.handler.SessionHandShakeHandler;
import com.arloor.forwardproxy.trace.TraceConstant;
import com.arloor.forwardproxy.trace.Tracer;
import com.arloor.forwardproxy.util.OsUtils;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.Config;
import com.arloor.forwardproxy.web.Dispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;

public enum Status {
    HTTP_REQUEST {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            if (msg instanceof HttpRequest request) {
                session.setRequest(request);
                String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
                String[] hostPortArray = hostAndPortStr.split(":");
                String host = hostPortArray[0];
                session.setHost(host);
                String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
                session.setPort(Integer.parseInt(portStr));
                session.setAttribute(TraceConstant.host.name(), host);
                session.setStatus(LAST_HTTP_CONTENT);
            }
        }
    }, LAST_HTTP_CONTENT {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            //SimpleChannelInboundHandler会将HttpContent中的bytebuf Release，但是这个还会转给relayHandler，所以需要在这里预先retain
            ((HttpContent) msg).content().retain();
            session.addContent((HttpContent) msg);
            if (msg instanceof LastHttpContent) {
                // 1. 如果url以 / 开头，则认为是直接请求，而不是代理请求
                if (session.getRequest().uri().startsWith("/")) {
                    session.setStatus(WEB);
                    session.handle(channelContext, msg);
                } else {
                    session.setStatus(CheckAuth);
                    session.handle(channelContext, msg);
                }
            }
        }
    }, WEB {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            session.setAttribute(TraceConstant.host.name(), "localhost");
            Span dispatch = Tracer.spanBuilder(TraceConstant.web.name())
                    .setAttribute(TraceConstant.url.name(), String.valueOf(session.getRequest().uri()))
                    .setParent(io.opentelemetry.context.Context.current().with(session.getStreamSpan()))
                    .startSpan();
            try (Scope scope = dispatch.makeCurrent()) {
                boolean ifNeedClose = session.incrementCountAndIfNeedClose();
                Dispatcher.handle(session.getRequest(), channelContext, ifNeedClose);
                // 这里需要将content全部release
                session.getContents().forEach(ReferenceCountUtil::release);
            } finally {
                dispatch.end();
            }
            session.setStatus(HTTP_REQUEST);
        }
    }, CheckAuth {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            String clientHostname = ((InetSocketAddress) channelContext.channel().remoteAddress()).getAddress().getHostAddress();
            //2. 检验auth
            HttpRequest request = session.getRequest();
            String basicAuth = request.headers().get("Proxy-Authorization");
            String userName = "nouser";
            Map<String, String> auths = session.getAuths();
            if (basicAuth != null && basicAuth.length() != 0) {
                String raw = auths.get(basicAuth);
                if (raw != null && raw.length() != 0) {
                    userName = raw.split(":")[0];
                }
            }

            if (!session.checkAuth(basicAuth)) {
                log.warn(clientHostname + " " + request.method() + " " + request.uri() + "  {" + session.getHost() + "} wrong_auth:{" + basicAuth + "}");
                // 这里需要将content全部release
                session.getContents().forEach(ReferenceCountUtil::release);
                DefaultHttpResponse responseAuthRequired;
                if (Config.ask4Authcate && !session.isCheckAuthByWhiteDomain() && !request.method().equals(HttpMethod.OPTIONS) && !request.method().equals(HttpMethod.HEAD)) {
                    responseAuthRequired = new DefaultHttpResponse(request.protocolVersion(), PROXY_AUTHENTICATION_REQUIRED);
                    responseAuthRequired.headers().add("Proxy-Authenticate", "Basic realm=\"netty forwardproxy\"");
                } else {
                    responseAuthRequired = new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR);
                }
                channelContext.channel().writeAndFlush(responseAuthRequired);
                SocksServerUtils.closeOnFlush(channelContext.channel());
                Tracer.spanBuilder(TraceConstant.wrong_auth.name())
                        .setAttribute(TraceConstant.auth.name(), String.valueOf(basicAuth))
                        .setParent(io.opentelemetry.context.Context.current().with(session.getStreamSpan()))
                        .startSpan()
                        .end();
                session.setStatus(HTTP_REQUEST);
                return;
            }

            //3. 这里进入代理请求处理，分为两种：CONNECT方法和其他HTTP方法
            log.info("{}@{} ==> {} {} {}", userName, clientHostname, request.method(), request.uri(), !request.uri().equals(request.headers().get("Host")) ? "Host=" + request.headers().get("Host") : "");
            if (request.method().equals(HttpMethod.CONNECT)) {
                session.setStatus(TUNNEL);
            } else {
                session.setStatus(GETPOST);
            }
            session.handle(channelContext, msg);
        }
    }, TUNNEL {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            HttpRequest request = session.getRequest();
            final Channel inboundChannel = channelContext.channel();
            Bootstrap b = session.getBootStrap();
            b.group(inboundChannel.eventLoop())
                    .channel(OsUtils.socketChannelClazz())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new RelayHandler(channelContext.channel(), session.getHost()));
            b.connect(session.getHost(), session.getPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        final Channel outboundChannel = future.channel();
                        String targetAddr = ((InetSocketAddress) outboundChannel.remoteAddress()).getAddress().getHostAddress();
                        session.setAttribute(TraceConstant.target.name(), targetAddr);
                        // Connection established use handler provided results
                        ChannelFuture responseFuture = channelContext.channel().writeAndFlush(
                                new DefaultHttpResponse(request.protocolVersion(), new HttpResponseStatus(200, "Connection Established")));
                        responseFuture.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture channelFuture) {
                                if (channelFuture.isSuccess()) {
                                    channelContext.pipeline().remove(IdleStateHandler.class);
                                    channelContext.pipeline().remove(HttpRequestDecoder.class);
                                    channelContext.pipeline().remove(HttpResponseEncoder.class);
                                    channelContext.pipeline().remove(HttpServerExpectContinueHandler.class);
                                    channelContext.pipeline().remove(SessionHandShakeHandler.class);
                                    channelContext.pipeline().addLast(new RelayHandler(outboundChannel, session.getHost()));
                                } else {
                                    log.info("reply tunnel established Failed: " + channelContext.channel().remoteAddress() + " " + request.method() + " " + request.uri());
                                    SocksServerUtils.closeOnFlush(channelContext.channel());
                                    SocksServerUtils.closeOnFlush(outboundChannel);
                                }
                            }
                        });
                    } else {
                        // Close the connection if the connection attempt has failed.
                        channelContext.channel().writeAndFlush(
                                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                        );
                        SocksServerUtils.closeOnFlush(channelContext.channel());
                    }
                }
            });
            session.setStatus(WAIT_ESTABLISH);
        }
    }, GETPOST {
        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            HttpRequest request = session.getRequest();
            final Channel inboundChannel = channelContext.channel();
            Bootstrap b = session.getBootStrap();
            b.group(inboundChannel.eventLoop())
                    .channel(OsUtils.socketChannelClazz())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel outboundChannel) throws Exception {
                            outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                            outboundChannel.pipeline().addLast(new RelayHandler(channelContext.channel(), session.getHost()));
                        }
                    });
            b.connect(session.getHost(), session.getPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        final Channel outboundChannel = future.channel();
                        String targetAddr = ((InetSocketAddress) outboundChannel.remoteAddress()).getAddress().getHostAddress();
                        session.setAttribute(TraceConstant.target.name(), targetAddr);
                        // Connection established use handler provided results
                        // 这里有几率抛出NoSuchElementException，原因是连接target host完成时，客户端已经关闭连接。
                        // 考虑到是比较小的几率，不catch。注：该异常没有啥影响。
                        channelContext.pipeline().remove(IdleStateHandler.class);
                        channelContext.pipeline().remove(SessionHandShakeHandler.class);
                        channelContext.pipeline().remove(HttpResponseEncoder.class);
                        RelayHandler clientEndtoRemoteHandler = new RelayHandler(outboundChannel, session.getHost());
                        channelContext.pipeline().addLast(clientEndtoRemoteHandler);
//                                        ctx.channel().config().setAutoRead(true);

                        //出于未知的原因，不知道为什么fireChannelread不行
                        clientEndtoRemoteHandler.channelRead(channelContext, request);
                        session.getContents().forEach(content -> {
                            try {
                                clientEndtoRemoteHandler.channelRead(channelContext, content);
                            } catch (Exception e) {
                                log.error("处理非CONNECT方法的代理请求失败！", e);
                            }
                        });

                    } else {
                        // Close the connection if the connection attempt has failed.
                        channelContext.channel().writeAndFlush(
                                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                        );
                        SocksServerUtils.closeOnFlush(channelContext.channel());
                    }
                }
            });
            session.setStatus(WAIT_ESTABLISH);
        }
    }, WAIT_ESTABLISH { // 等待到target的连接建立前不应该有新请求进入

        @Override
        public void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg) {
            log.error("receive new message before tunnel is established, msg: {}", msg);
        }
    };

    private static final Logger log = LoggerFactory.getLogger(Status.class);

    public abstract void handle(Session session, ChannelHandlerContext channelContext, HttpObject msg);
}
