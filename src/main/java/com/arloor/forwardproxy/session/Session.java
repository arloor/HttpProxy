package com.arloor.forwardproxy.session;

import com.arloor.forwardproxy.handler.SessionHandShakeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

public class Session {

    private static final Logger log = LoggerFactory.getLogger(SessionHandShakeHandler.class);
    private final Map<String, String> auths;
    private Span streamSpan;
    private Status status = Status.HTTP_REQUEST;
    private final Bootstrap bootstrap = new Bootstrap();

    private String host;
    private int port;
    private HttpRequest request;
    private ArrayList<HttpContent> contents = new ArrayList<>();
    private volatile boolean hasChunkedWriter = false;


    public Session(Map<String, String> auths, Span streamSpan) {
        this.auths = auths;
        this.streamSpan = streamSpan;
    }

    public void ensureChunkedWriter(ChannelHandlerContext channelHandlerContext) {
        if (!hasChunkedWriter) {
            synchronized (this) {
                if (!hasChunkedWriter) {
                    channelHandlerContext.pipeline().addBefore(SessionHandShakeHandler.NAME, "chunked", new ChunkedWriteHandler());
                    hasChunkedWriter = true;
                }
            }
        }

    }

    public void handle(ChannelHandlerContext channelHandlerContext, HttpObject msg) {
        this.status.handle(this, channelHandlerContext, msg);
    }

    public Bootstrap getBootStrap() {
        return bootstrap;
    }

    public void addContent(HttpContent httpContent) {
        this.contents.add(httpContent);
    }

    public void setAttribute(String key, String value) {
        this.streamSpan.setAttribute(key, value);
    }

    public Span getStreamSpan() {
        return streamSpan;
    }

    public Map<String, String> getAuths() {
        return auths;
    }

    public ArrayList<HttpContent> getContents() {
        return contents;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }
}
