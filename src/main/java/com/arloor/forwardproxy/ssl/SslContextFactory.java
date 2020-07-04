package com.arloor.forwardproxy.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class SslContextFactory {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SslContextFactory.class);

    static {
        // 解决algid parse error, not a sequence
        // https://blog.csdn.net/ls0111/article/details/77533768
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );
    }


    public static SslContext getSSLContext(String fullchainFile, String privkeyFile) throws IOException, GeneralSecurityException {
        try {
            //jdk8删除gcm加密
            List<String> ciphers = Arrays.asList("ECDHE-RSA-AES128-SHA", "ECDHE-RSA-AES256-SHA", "AES128-SHA", "AES256-SHA", "DES-CBC3-SHA");

            return SslContextBuilder.forServer(new File(fullchainFile),new File(privkeyFile))
                    .protocols("TLSv1.3", "TLSv1.2")
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.NONE)
                    .trustManager(new File(fullchainFile))
//                    .ciphers(ciphers)
                    .build();

        } catch (IOException e) {
            LOGGER.warn("Failed to establish SSL Context");
            LOGGER.debug("Failed to establish SSL Context", e);
            throw e;
        }
    }



    private static void closeSilent(final InputStream is) {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Exception ignored) {
        }
    }
}