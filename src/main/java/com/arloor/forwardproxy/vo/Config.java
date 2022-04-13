package com.arloor.forwardproxy.vo;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    private static final String TRUE = "true";

    public static boolean ask4Authcate = false;
    private static final String POUND_SIGN = "\u00A3";  // £

    private SslConfig sslConfig;
    private HttpConfig httpConfig;

    public SslConfig ssl() {
        return sslConfig;
    }

    public HttpConfig http() {
        return httpConfig;
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
            SslConfig sslConfig = new SslConfig(port, users, fullchain, privkey);
            config.sslConfig = sslConfig;
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
            String whiteDomains = properties.getProperty("http.proxy.white.domain", "");
            config.httpConfig = new HttpConfig(port, users, Arrays.stream(whiteDomains.split(",")).filter(s -> s != null && s.length() != 0).collect(Collectors.toSet()));
            ;
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


}