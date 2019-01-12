package com.arloor.proxycommon.crypto.utils;

import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.util.Base64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Cryptor {
    Logger logger = LoggerFactory.getLogger(Cryptor.class);
    String cryptoKey = Config.cryptoKey();

    void decrypt(ByteBuf buf);

    void encrypt(ByteBuf buf);

    static Cryptor simple() {
        return new Cryptor() {

            @Override
            public void decrypt(ByteBuf buf) {
//                logger.debug("读时解密(字节取反)");
                int lengh = buf.writerIndex();
                byte[] bytes = new byte[lengh];
                buf.readBytes(bytes);
                bytes = Base64.decode(bytes);
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) ~bytes[i];
                }
                buf.clear();
                buf.writeBytes(bytes);
            }

            @Override
            public void encrypt(ByteBuf buf) {
//                logger.debug("读时解密(字节取反)");
                int lengh = buf.writerIndex();
                byte[] bytes = new byte[lengh];
                buf.readBytes(bytes);
                for (int i = 0; i < lengh; i++) {
                    bytes[i] = (byte) ~bytes[i];
                }
                bytes = Base64.encode(bytes);
                buf.clear();
                buf.writeBytes(bytes);
            }
        };
    }
}
