package com.arloor.proxycommon.filter.crypto.utils;

import com.arloor.proxycommon.filter.crypto.handler.CryptoHandler;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface Cryptor {
    Logger logger= LoggerFactory.getLogger(Cryptor.class);
    String cryptoKey= Cryptor.getCryptoKey();

    void decrypt(ByteBuf buf);

    void encrypt(ByteBuf buf);

    static String getCryptoKey() {
        Properties prop = new Properties();
        InputStream in = CryptoHandler.class.getResourceAsStream("/proxy.properties");
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cryptoKey=prop.getProperty("crypto.key","竟然不设置密码。。真粗心");
        return cryptoKey;
    }

    static Cryptor simple(){
        return new Cryptor() {

            private void qufan(ByteBuf buf){
                int lengh=buf.writerIndex();
                byte[] bytes=new byte[lengh];
                buf.readBytes(bytes);
                for (int i = 0; i <lengh ; i++) {
                    bytes[i]=(byte) ~bytes[i];
                }
                buf.clear();
                buf.writeBytes(bytes);
            }

            @Override
            public void decrypt(ByteBuf buf) {
                logger.debug("读时解密(字节取反)");
                qufan(buf);
            }

            @Override
            public void encrypt(ByteBuf buf) {
                logger.debug("读时解密(字节取反)");
                qufan(buf);
            }
        };
    }
}
