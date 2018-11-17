package com.arloor.proxycommon.filter.crypto.utils;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Decrypter {
    Logger logger= LoggerFactory.getLogger(Decrypter.class);

    static Decrypter simple(){
        return (buf)->{logger.debug("读时解密");};
    }

    void decrypt(ByteBuf buf);
}
