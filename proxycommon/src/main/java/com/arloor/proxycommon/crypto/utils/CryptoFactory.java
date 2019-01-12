package com.arloor.proxycommon.crypto.utils;

import com.arloor.proxycommon.crypto.utils.cryptoimpl.AES128;

import java.util.HashMap;
import java.util.function.Supplier;

public class CryptoFactory {
    private static HashMap<CryptoType, Supplier<Cryptor>> cryptorMap=new HashMap<>();

    //初始化两个map
    static {
        cryptorMap.put(CryptoType.SIMPLE, Cryptor::simple);
        cryptorMap.put(CryptoType.AES, AES128::new);
    }

    public static Cryptor createCryptor(CryptoType cryptoType) {
        return cryptorMap.get(cryptoType).get();
    }
}
