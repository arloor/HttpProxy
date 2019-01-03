package com.arloor.proxyserver.Manman;

import com.arloor.proxyserver.Manman.AESUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Encoder;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.*;

public class SmsCode {
    public static  void main(String[] args) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        String url="/searchtruck-app/truck/search-f367e4e5fa7b38ed";
        String key=string2SHA256(url);
        System.out.println("key: "+key);
//        String result=encrypt("{\"abc\":\"123\"}",key.substring(0,16));
        String result= null;
        System.out.println("{\"abc\":\"123\"}");
        try {
            result = AESUtil.encrypt("{\"abc\":\"123\"}",key.substring(0,16));
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        System.out.println(result);
    }

    public static String string2SHA256(String str){
        MessageDigest messageDigest;
        String encdeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(str.getBytes("UTF-8"));
            // 將 byte 轉換爲 string
            // 得到返回結果
            encdeStr = bytes2Hex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encdeStr;
    }


    public static final String encrypt(String plainText, String key) {
        Key secretKey = getKey(key);
        try {
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding","BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] p = plainText.getBytes("UTF-8");
            byte[] result = cipher.doFinal(p);
            // 得到返回結果
            System.out.println(bytes2Hex(result));

            BASE64Encoder encoder = new BASE64Encoder();
            String encoded = encoder.encode(result);
            return encoded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Key getKey(String keySeed) {
        if (keySeed == null) {
            keySeed = System.getenv("AES_SYS_KEY");
        }
        if (keySeed == null) {
            keySeed = System.getProperty("AES_SYS_KEY");
        }
        if (keySeed == null || keySeed.trim().length() == 0) {
            keySeed = "abcd1234!@#$";// 默认种子
        }
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(keySeed.getBytes());
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(secureRandom);
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String bytes2Hex(byte[] array){
        StringBuffer strHexString = new StringBuffer();
        for (int i = 0; i < array.length; i++)
        {
            String hex = Integer.toHexString(0xff & array[i]);
            if (hex.length() == 1)
            {
                strHexString.append('0');
            }
            strHexString.append(hex);
        }
        return strHexString.toString();
    }

}
