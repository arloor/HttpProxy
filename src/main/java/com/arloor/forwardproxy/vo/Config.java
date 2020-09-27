package com.arloor.forwardproxy.vo;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final String TRUE = "true";

    public static boolean ask4Authcate = false;

    private Ssl ssl;
    private Http http;

    public Ssl ssl() {
        return ssl;
    }

    public Http http() {
        return http;
    }

    public static Config parse(Properties properties) {

        Config config = new Config();
        ask4Authcate = TRUE.equals(properties.getProperty("ask4Authcate"));

        String httpsEnable = properties.getProperty("https.enable");
        if (TRUE.equals(httpsEnable)) {
            String httpsPortStr = properties.getProperty("https.port");
            Integer port = Integer.parseInt(httpsPortStr);
            String auth = properties.getProperty("https.auth");
            Map<String, String> users = new HashMap<>();
            if (auth != null && auth.length() != 0) {
                for (String user : auth.split(",")) {
                    users.computeIfAbsent("Basic " + Base64.getEncoder().encodeToString(user.getBytes()), (cell) -> user);
                }
            }
            String fullchain = properties.getProperty("https.fullchain.pem");
            String privkey = properties.getProperty("https.privkey.pem");
            Ssl ssl = new Ssl(port, users, fullchain, privkey);
            config.ssl = ssl;
        }

        String httpEnable = properties.getProperty("http.enable");
        if (TRUE.equals(httpEnable)) {
            String httpPortStr = properties.getProperty("http.port");
            Integer port = Integer.parseInt(httpPortStr);
            String auth = properties.getProperty("http.auth");
            Map<String, String> users = new HashMap<>();
            if (auth != null && auth.length() != 0) {
                for (String user : auth.split(",")) {
                    users.computeIfAbsent("Basic " + Base64.getEncoder().encodeToString(user.getBytes()), (cell) -> user);
                }
            }
            Http http = new Http(port, users);
            config.http = http;
        }

        return config;
    }


    public static class Http {
        private Integer port;
        private Map<String, String> auth; // base64 - raw

        public Http(Integer port, Map<String, String> auth) {
            this.port = port;
            this.auth = auth;
        }

        public Integer getPort() {
            return port;
        }

        public String getAuth(String base64Auth) {
            return auth.get(base64Auth);
        }

        public Map<String, String> getAuthMap() {
            return auth;
        }

        public boolean needAuth() {
            return auth != null && auth.size() != 0;
        }

    }

    public static class Ssl {
        private Integer port;
        private Map<String, String> auth; // base64 - raw
        private String fullchain;
        private String privkey;

        public Ssl(Integer port, Map<String, String> auth, String fullchain, String privkey) {
            this.port = port;
            this.auth = auth;
            this.fullchain = fullchain;
            this.privkey = privkey;
        }

        public Integer getPort() {
            return port;
        }

        public String getAuth(String base64Auth) {
            return auth.get(base64Auth);
        }

        public Map<String, String> getAuthMap() {
            return auth;
        }

        public String getFullchain() {
            return fullchain;
        }

        public String getPrivkey() {
            return privkey;
        }

        public boolean needAuth() {
            return auth != null && auth.size() != 0;
        }
    }
}