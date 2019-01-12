package com.arloor.proxycommon.crypto.handler;

import com.arloor.proxycommon.crypto.utils.CryptoFactory;
import com.arloor.proxycommon.crypto.utils.Cryptor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public  class DecryptHandler extends SimpleChannelInboundHandler<ByteBuf> implements CryptoHandler{
    private Cryptor cryptor= CryptoFactory.createCryptor(cryptoType);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byteBuf.retain();
        cryptor.decrypt(byteBuf);
        ctx.fireChannelRead(byteBuf);
    }
}
