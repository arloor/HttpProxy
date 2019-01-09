package com.arloor.proxycommon.filter.crypto.handler;

import com.arloor.proxycommon.filter.crypto.utils.CryptoType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface CryptoHandler {
    CryptoType cryptoType= CryptoHandler.getCryptoType();


    static CryptoType getCryptoType(){
        Properties prop = new Properties();
        InputStream in = CryptoHandler.class.getResourceAsStream("/proxy.properties");
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cryptoType=prop.getProperty("crypto.type","SIMPLE");
        return CryptoType.parse(cryptoType);
    }


}
