package com.arloor.forwardproxy.session;

import com.arloor.forwardproxy.handler.SessionHandShakeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class Session {
    private static final int MAX_REQUEST_NUM = 20;
    private static final Logger log = LoggerFactory.getLogger(SessionHandShakeHandler.class);
    private final Map<String, String> auths;
    private Span streamSpan;
    private Set<String> whiteDomains;
    private Status status = Status.HTTP_REQUEST;
    private final Bootstrap bootstrap = new Bootstrap();

    private String host;
    private int port;
    private int requestCount = 0;
    private HttpRequest request;
    private ArrayList<HttpContent> contents = new ArrayList<>();

    public Session(Map<String, String> auths, Span streamSpan, Set<String> whiteDomains) {
        this.auths = auths;
        this.streamSpan = streamSpan;
        this.whiteDomains = whiteDomains;
    }

    public boolean incrementCountAndIfNeedClose() {
        requestCount++;
        return requestCount >= MAX_REQUEST_NUM;
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

    public boolean checkAuth(String basicAuth) {
        if (isCheckAuthByWhiteDomain()) {
            if (host != null) {
                for (String whiteDomain : whiteDomains) {
                    if (host.endsWith(whiteDomain)) {
                        return true;
                    }
                }
            }
        } else {
            if (auths != null && auths.size() != 0) {
                if (basicAuth != null && auths.containsKey(basicAuth)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckAuthByWhiteDomain() {
        return !whiteDomains.isEmpty();
    }
}
