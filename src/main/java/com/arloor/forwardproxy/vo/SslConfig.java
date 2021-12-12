package com.arloor.forwardproxy.vo;

import java.util.Map;

public class SslConfig {
    private Integer port;
    private Map<String, String> auth; // base64 - raw
    private String fullchain;
    private String privkey;

    public SslConfig(Integer port, Map<String, String> auth, String fullchain, String privkey) {
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
