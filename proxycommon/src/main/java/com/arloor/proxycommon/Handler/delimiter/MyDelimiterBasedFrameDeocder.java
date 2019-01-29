package com.arloor.proxycommon.Handler.delimiter;

import com.arloor.proxycommon.Config;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

public class MyDelimiterBasedFrameDeocder extends DelimiterBasedFrameDecoder {

     public MyDelimiterBasedFrameDeocder(){
        super(Integer.MAX_VALUE,true,true,Unpooled.wrappedBuffer(Config.delimiter().getBytes()));
     }
}
