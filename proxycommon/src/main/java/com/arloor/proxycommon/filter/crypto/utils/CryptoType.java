package com.arloor.proxycommon.filter.crypto.utils;

public enum CryptoType {
    SIMPLE,DES;

    public static CryptoType parse(String type){
        if(type==null){
            return SIMPLE;
        }
        if(type.equals("DES")){
            return DES;
        }else return SIMPLE;
    }
}
