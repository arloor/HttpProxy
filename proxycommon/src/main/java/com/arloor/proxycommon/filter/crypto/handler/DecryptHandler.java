package com.arloor.proxycommon.filter.crypto.handler;

import com.arloor.proxycommon.filter.crypto.utils.Cryptor;
import com.arloor.proxycommon.filter.crypto.utils.CryptoFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Arrays;

public  class DecryptHandler extends SimpleChannelInboundHandler<ByteBuf> implements CryptoHandler{
    private Cryptor cryptor= CryptoFactory.createCryptor(cryptoType);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byteBuf.retain();
        cryptor.decrypt(byteBuf);
        ctx.fireChannelRead(byteBuf);
    }
}
