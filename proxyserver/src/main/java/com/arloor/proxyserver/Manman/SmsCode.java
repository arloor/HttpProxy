package com.arloor.proxyserver.Manman;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Encoder;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Arrays;

public class SmsCode {
    public static  void main(String[] args) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchProviderException {
        String url="/logistics/user/getloginverifycode-d4af719008dd7f88";
        byte[] key=string2SHA256(url);
        System.out.println("aes128 key(hex): "+bytes2Hex(key));
        byte[] aesResult = AESUtil.encrypt("{\"from\":\"8\",\"telephone\":\"17625955421\"}", Arrays.copyOf(key,16));
        System.out.println("aes128 result(hex): "+bytes2Hex(aesResult));
        byte[] confusionResult= Transform.confusion(aesResult);
        System.out.println("confusion result(hex): "+bytes2Hex(confusionResult));
        System.out.println("confusion result(String): "+new String(confusionResult));
    }

    public static byte[] string2SHA256(String str){
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(str.getBytes("UTF-8"));
            // 將 byte 轉換爲 string
            // 得到返回結果
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
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
