package com.arloor.proxycommon.Handler.length;

import io.netty.handler.codec.LengthFieldPrepender;

public class MyLengthFieldPrepender extends LengthFieldPrepender {
    public MyLengthFieldPrepender() {
        super(4);
    }
}
