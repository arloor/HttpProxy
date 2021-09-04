package com.arloor.forwardproxy.vo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final String TRUE = "true";

    public static boolean ask4Authcate = false;
    private static final String POUND_SIGN = "\u00A3";  // £

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
                    users.computeIfAbsent(genBasicAuth(user), (cell) -> user);
                    users.computeIfAbsent(genBasicAuthWithOut£(user), (cell) -> user);
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
                    users.computeIfAbsent(genBasicAuth(user), (cell) -> user);
                    users.computeIfAbsent(genBasicAuthWithOut£(user), (cell) -> user);
                }
            }
            Http http = new Http(port, users);
            config.http = http;
        }

        return config;
    }

    /**
     * https://datatracker.ietf.org/doc/html/rfc7617
     * The user's name is "test", and the password is the string "123"
     * followed by the Unicode character U+00A3 (POUND SIGN).  Using the
     * character encoding scheme UTF-8, the user-pass becomes:
     * <p>
     * 't' 'e' 's' 't' ':' '1' '2' '3' pound
     * 74  65  73  74  3A  31  32  33  C2  A3
     * <p>
     * Encoding this octet sequence in Base64 ([RFC4648], Section 4) yields:
     * <p>
     * dGVzdDoxMjPCow==
     *
     * @param user
     * @return
     */
    private static String genBasicAuth(String user) {
        user += POUND_SIGN;
        return "Basic " + Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8));
    }


    private static String genBasicAuthWithOut£(String user) {
        return "Basic " + Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8));
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