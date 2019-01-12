package com.arloor.proxycommon;

import com.arloor.proxycommon.crypto.handler.CryptoHandler;
import com.arloor.proxycommon.crypto.utils.CryptoType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static Properties prop = new Properties();

    static {
        InputStream in = CryptoHandler.class.getResourceAsStream("/proxy.properties");
        try {
            prop.load(in);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public  static boolean crypto() {
        String crypto = prop.getProperty("crypto", "false");
        return !crypto.equals("false");
    }

    public  static String delimiter() {
        return prop.getProperty("crypto.delimiter", "br");
    }

    public  static CryptoType cryptoType() {
        return CryptoType.parse(prop.getProperty("crypto.type", "SIMPLE"));
    }

    public  static String cryptoKey() {
        return prop.getProperty("crypto.key", "竟然不设置密码。。真粗心");
    }

    public  static String serverport() {
        return prop.getProperty("server.port", "8080");
    }

    public  static String clientPort() {
        return prop.getProperty("client.port", "8081");
    }

    public  static String serverhost() {
        return prop.getProperty("server.host", "127.0.0.1");
    }
}
