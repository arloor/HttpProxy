package com.arloor.proxycommon.crypto.handler;

import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.crypto.utils.CryptoType;

public interface CryptoHandler {
    CryptoType cryptoType= Config.cryptoType();
}
