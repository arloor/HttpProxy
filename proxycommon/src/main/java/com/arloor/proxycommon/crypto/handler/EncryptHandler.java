package com.arloor.proxycommon.crypto.handler;

import com.arloor.proxycommon.crypto.utils.Cryptor;
import com.arloor.proxycommon.crypto.utils.CryptoFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public  class EncryptHandler extends ChannelOutboundHandlerAdapter implements CryptoHandler{
    private Cryptor cryptor= CryptoFactory.createCryptor(cryptoType);
    private static Logger logger= LoggerFactory.getLogger(EncryptHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf buf=(ByteBuf)msg;
            cryptor.encrypt(buf);
            super.write(ctx, buf, promise);
        }else {
            logger.error("EncryptHandler所处理的入参不是Bytebuf，下面将会退出");
            System.exit(1);
        }

    }
}
