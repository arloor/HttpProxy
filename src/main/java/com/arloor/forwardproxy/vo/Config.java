package com.arloor.forwardproxy.vo;

import java.util.Base64;
import java.util.Properties;

public class Config {
    private static final String TRUE = "true";

    public static boolean ask4Authcate=false;
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
        ask4Authcate =TRUE.equals(properties.getProperty("ask4Authcate"));

        String httpsEnable = properties.getProperty("https.enable");
        if (TRUE.equals(httpsEnable)) {
            String httpsPortStr = properties.getProperty("https.port");
            Integer port = Integer.parseInt(httpsPortStr);
            String auth = properties.getProperty("https.auth");
            auth = auth==null?auth:"Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
            String fullchain = properties.getProperty("https.fullchain.pem");
            String cert = properties.getProperty("https.cert.pem");
            String privkey = properties.getProperty("https.privkey.pem");
            Ssl ssl = new Ssl(port, auth, fullchain,cert, privkey);
            config.ssl = ssl;
        }

        String httpEnable = properties.getProperty("http.enable");
        if (TRUE.equals(httpEnable)) {
            String httpPortStr = properties.getProperty("http.port");
            Integer port = Integer.parseInt(httpPortStr);
            String auth = properties.getProperty("http.auth");
            auth = auth==null?auth:"Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
            String reverseBitStr = properties.getProperty("http.reverseBit");
            Boolean reverseBit = TRUE.equals(reverseBitStr);
            Http http = new Http(port, reverseBit, auth);
            config.http = http;
        }

        return config;
    }


    public static class Http {
        private Integer port;
        private Boolean reverseBit;
        private String auth;

        public Http(Integer port, Boolean reverseBit, String auth) {
            this.port = port;
            this.reverseBit = reverseBit;
            this.auth = auth;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Boolean getReverseBit() {
            return reverseBit;
        }

        public void setReverseBit(Boolean reverseBit) {
            this.reverseBit = reverseBit;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }

    public static class Ssl {
        private Integer port;
        private String auth;
        private String fullchain;
        private String cert;
        private String privkey;

        public Ssl(Integer port, String auth, String fullchain, String cert, String privkey) {
            this.port = port;
            this.auth = auth;
            this.fullchain = fullchain;
            this.cert = cert;
            this.privkey = privkey;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

        public String getFullchain() {
            return fullchain;
        }

        public void setFullchain(String fullchain) {
            this.fullchain = fullchain;
        }

        public String getCert() {
            return cert;
        }

        public void setCert(String cert) {
            this.cert = cert;
        }

        public String getPrivkey() {
            return privkey;
        }

        public void setPrivkey(String privkey) {
            this.privkey = privkey;
        }
    }
}