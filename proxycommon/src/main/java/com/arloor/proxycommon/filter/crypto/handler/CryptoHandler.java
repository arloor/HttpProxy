package com.arloor.proxycommon.filter.crypto.handler;

import com.arloor.proxycommon.Config;
import com.arloor.proxycommon.filter.crypto.utils.CryptoType;

public interface CryptoHandler {
    CryptoType cryptoType= Config.cryptoType();
}
