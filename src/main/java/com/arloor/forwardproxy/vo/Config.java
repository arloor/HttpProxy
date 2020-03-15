package com.arloor.forwardproxy.vo;

import java.util.Base64;
import java.util.Properties;

public class Config {
    private static final String TRUE = "true";

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
        String httpsEnable = properties.getProperty("https.enable");
        if (TRUE.equals(httpsEnable)) {
            String httpsPortStr = properties.getProperty("https.port");
            Integer port = Integer.parseInt(httpsPortStr);
            String auth = properties.getProperty("https.auth");
            auth = auth==null?auth:"Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
            String rootCrt = properties.getProperty("https.rootCrt");
            String crt = properties.getProperty("https.crt");
            String key = properties.getProperty("https.key");
            Ssl ssl = new Ssl(port, auth, rootCrt, crt, key);
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
        private String rootCrt;
        private String crt;
        private String key;

        public Ssl(Integer port, String auth, String rootCrt, String crt, String key) {
            this.port = port;
            this.auth = auth;
            this.rootCrt = rootCrt;
            this.crt = crt;
            this.key = key;
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

        public String getRootCrt() {
            return rootCrt;
        }

        public void setRootCrt(String rootCrt) {
            this.rootCrt = rootCrt;
        }

        public String getCrt() {
            return crt;
        }

        public void setCrt(String crt) {
            this.crt = crt;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}