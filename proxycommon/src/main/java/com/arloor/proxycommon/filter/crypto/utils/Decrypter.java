package com.arloor.proxycommon.filter.crypto.utils;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Decrypter {
    Logger logger= LoggerFactory.getLogger(Decrypter.class);

    static Decrypter simple(){
        return (buf)->{
            logger.debug("读时解密");
            int lengh=buf.writerIndex();
            byte[] bytes=new byte[lengh];
            buf.readBytes(bytes);
            for (int i = 0; i <lengh ; i++) {
                bytes[i]=(byte) ~bytes[i];
            }
            buf.clear();
            buf.writeBytes(bytes);
        };
    }

    void decrypt(ByteBuf buf);
}
