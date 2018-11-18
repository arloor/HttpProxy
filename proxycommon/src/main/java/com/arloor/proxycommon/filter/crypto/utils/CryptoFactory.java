package com.arloor.proxycommon.filter.crypto.utils;

import com.arloor.proxycommon.filter.crypto.utils.cryptoimpl.DESdecrypter;
import com.arloor.proxycommon.filter.crypto.utils.cryptoimpl.DESencrypter;

import java.util.HashMap;
import java.util.function.Supplier;

public class CryptoFactory {
    private static HashMap<CryptoType, Supplier<Encrypter>> encyptTypeMap=new HashMap<>();

    private static HashMap<CryptoType, Supplier<Decrypter>> decyptTypeMap=new HashMap<>();

    //初始化两个map
    static {
        encyptTypeMap.put(CryptoType.SIMPLE, Encrypter::simple);
        decyptTypeMap.put(CryptoType.SIMPLE, Decrypter::simple);

        encyptTypeMap.put(CryptoType.DES, DESencrypter::new);
        decyptTypeMap.put(CryptoType.DES, DESdecrypter::new);
    }

    public static Encrypter createEncrypter(CryptoType type){
        return encyptTypeMap.get(type).get();
    }

    public static Decrypter createDecrypter(CryptoType type){
        return decyptTypeMap.get(type).get();
    }
}
