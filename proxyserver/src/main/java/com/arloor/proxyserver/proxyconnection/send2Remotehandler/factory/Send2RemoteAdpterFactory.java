package com.arloor.proxyserver.proxyconnection.send2Remotehandler.factory;

import com.arloor.proxycommon.httpentity.HttpMethod;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.impl.Send2HttpRemoteInboundAdpter;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.impl.Send2HttpsRemoteInboundAdpter;
import com.arloor.proxyserver.proxyconnection.send2Remotehandler.Send2RemoteAdapter;
import io.netty.channel.socket.SocketChannel;


public class Send2RemoteAdpterFactory {
    public static Send2RemoteAdapter create(HttpMethod method, SocketChannel remoteChannel){
        if(method.equals(HttpMethod.CONNECT)){
            return new Send2HttpsRemoteInboundAdpter(remoteChannel);
        }else {
            return new Send2HttpRemoteInboundAdpter(remoteChannel);
        }
    }
}
