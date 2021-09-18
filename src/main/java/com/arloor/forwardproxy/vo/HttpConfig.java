package com.arloor.forwardproxy.vo;

import java.util.Map;

public class HttpConfig {
    private Integer port;
    private Map<String, String> auth; // base64 - raw

    public HttpConfig(Integer port, Map<String, String> auth) {
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
