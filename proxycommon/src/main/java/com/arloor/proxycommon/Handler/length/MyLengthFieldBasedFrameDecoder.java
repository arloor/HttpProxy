package com.arloor.proxycommon.Handler.length;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MyLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder {
    public MyLengthFieldBasedFrameDecoder() {
        super(Integer.MAX_VALUE, 0, 4, 0, 4);
    }
}
