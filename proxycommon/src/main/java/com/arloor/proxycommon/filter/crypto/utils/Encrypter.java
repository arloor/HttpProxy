package com.arloor.proxycommon.filter.crypto.utils;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Encrypter {
    Logger logger= LoggerFactory.getLogger(Encrypter.class);

    static Encrypter simple(){
        return (buf)->{logger.debug("写时加密");};
    }
    void encrypt(ByteBuf buf);
}
