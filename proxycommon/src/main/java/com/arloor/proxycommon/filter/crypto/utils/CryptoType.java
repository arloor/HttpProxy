package com.arloor.proxycommon.filter.crypto.utils;

public enum CryptoType {
    SIMPLE,AES;

    public static CryptoType parse(String type){
        if(type==null){
            return SIMPLE;
        }
        if(type.equals("AES")){
            return AES;
        }else return SIMPLE;
    }
}
