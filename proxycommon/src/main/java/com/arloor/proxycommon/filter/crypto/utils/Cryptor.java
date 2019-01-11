package com.arloor.proxycommon.filter.crypto.utils;

import com.arloor.proxycommon.Config;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Cryptor {
    Logger logger= LoggerFactory.getLogger(Cryptor.class);
    String cryptoKey= Config.cryptoKey();

    void decrypt(ByteBuf buf);

    void encrypt(ByteBuf buf);

    static Cryptor simple(){
        return new Cryptor() {

            private void qufan(ByteBuf buf){
                int lengh=buf.writerIndex();
                byte[] bytes=new byte[lengh];
                buf.readBytes(bytes);
                System.out.println("读到： "+new String(bytes));
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
