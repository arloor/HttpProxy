package com.arloor.proxycommon.filter.crypto.handler;

import com.arloor.proxycommon.filter.crypto.utils.Decrypter;
import com.arloor.proxycommon.filter.crypto.utils.CryptoFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public  class DecryptHandler extends SimpleChannelInboundHandler<ByteBuf> implements CryptoHandler{
    private Decrypter decrypter= CryptoFactory.createDecrypter(cryptoType);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byteBuf.retain();
        decrypter.decrypt(byteBuf);
        ctx.fireChannelRead(byteBuf);
    }
}
