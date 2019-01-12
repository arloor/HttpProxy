package com.arloor.proxyserver;

import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.httpentity.HttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthVerifyInboundhandler extends ChannelInboundHandlerAdapter {
    private static Logger logger= LoggerFactory.getLogger(AuthVerifyInboundhandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String delimiter= Config.delimiter();
        ByteBuf buf=(ByteBuf)msg;
        boolean valid=true;
        if(buf.readableBytes()<delimiter.length()){
            valid=false;
        }else{
            ByteBuf slice=buf.slice(buf.readableBytes()-delimiter.length(),2);
            byte[] sliceBytes=new byte[delimiter.length()];
            slice.readBytes(sliceBytes);
            if(!new String(sliceBytes).equals(delimiter)){
                valid=false;
            }

        }
        if(valid){
            logger.info("验证通过");
            ctx.pipeline().remove(this.getClass());
        }else{
            logger.info("验证失败,关闭channel。可能遭到嗅探");
            //ctx.writeAndFlush(PooledByteBufAllocator.DEFAULT.buffer().writeBytes(HttpResponse.ERROR404()));
            ctx.channel().close();
            return;
        }
        super.channelRead(ctx, msg);
    }
}
