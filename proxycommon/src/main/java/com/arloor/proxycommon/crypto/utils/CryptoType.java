package com.arloor.proxycommon.crypto.utils;

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


    @Override
    public String toString() {
        switch (this){
            case AES:return "AES";
            case SIMPLE:return "SIMPLE";
            default:return "SIMPLE";
        }
    }
}
