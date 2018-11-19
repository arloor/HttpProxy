package com.arloor.proxycommon.filter.crypto.handler;

import com.arloor.proxycommon.filter.crypto.utils.Encrypter;
import com.arloor.proxycommon.filter.crypto.utils.CryptoFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public  class EncryptHandler extends ChannelOutboundHandlerAdapter implements CryptoHandler{
    private Encrypter encrypter= CryptoFactory.createEncrypter(cryptoType);


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf buf=(ByteBuf)msg;
            encrypter.encrypt(buf);
        }
        super.write(ctx, msg, promise);
    }
}
