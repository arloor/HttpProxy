package com.arloor.forwardproxy.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.NONE)
                    .trustManager(new File(fullchainFile))
                    .ciphers(ciphers)
                    .build();

        } catch (IOException e) {
            LOGGER.warn("Failed to establish SSL Context");
            LOGGER.debug("Failed to establish SSL Context", e);
            throw e;
        }
    }

    public static SslContext getSSLContext(String rootCrt, String crt, String key) throws IOException, GeneralSecurityException {
        try {
            final PrivateKey privateKey = loadPrivateKey(key);
            final X509Certificate certificate = loadX509Cert(crt);
            final X509Certificate rootCA = loadX509Cert(rootCrt);
            //jdk8删除gcm加密
            List<String> ciphers = Arrays.asList("ECDHE-RSA-AES128-SHA", "ECDHE-RSA-AES256-SHA", "AES128-SHA", "AES256-SHA", "DES-CBC3-SHA");

            return SslContextBuilder.forServer(privateKey,certificate,rootCA)
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.NONE)
                    .trustManager(rootCA)
                    .ciphers(ciphers)
                    .build();

        } catch (IOException | GeneralSecurityException e) {
            LOGGER.warn("Failed to establish SSL Context");
            LOGGER.debug("Failed to establish SSL Context", e);
            throw e;
        }
    }

    private static InputStream getExternalStream(String filename) {
        try {
            LOGGER.debug("looking for " + filename + " externally");
            InputStream is = new FileInputStream(filename);
            if (is == null) {
                LOGGER.debug("Could not load configuration file from external path: " + filename);
            } else {
                LOGGER.debug("Found it!");
            }
            return is;
        } catch (Throwable e) {
            LOGGER.warn("Could not load configuration file from external path: " + e.getMessage());
            return null;
        }
    }

    public static PrivateKey loadPrivateKey(String fileName)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getExternalStream(fileName);
            if (is == null) {
                throw new FileNotFoundException("Key file could not be found via path or classpath");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                        inKey = true;
                    }
                } else {
                    if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
            byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
            LOGGER.error("Could not load Private Key: " + e.getMessage());
            throw e;
        } finally {
            closeSilent(is);
        }
        return key;
    }

    private static void closeSilent(final InputStream is) {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Loads an X.509 certificate from the classpath resources in
     * src/main/resources/keys.
     *
     * @param fileName name of a file in src/main/resources/keys.
     */
    public static X509Certificate loadX509Cert(String fileName) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (InputStream in = getExternalStream(fileName)) {
            return (X509Certificate) cf.generateCertificate(in);
        }
    }
}