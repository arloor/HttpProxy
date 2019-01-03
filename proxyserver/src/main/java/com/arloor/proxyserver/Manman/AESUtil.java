package com.arloor.proxyserver.Manman;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

public class AESUtil {

    // 密钥算法
    public static final String KEY_ALGORITHM = "AES";
    // 加密/解密算法/工作模式/填充方式
    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS7Padding";

    /**
     * AES加密
     * @param source
     * @param key 密钥
     * @return
     */
    static String encrypt(String source, String key) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM,"BC");
            SecretKeySpec keySpec=new SecretKeySpec(key.getBytes(), KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,keySpec );

            byte[] secret = cipher.doFinal(source.getBytes());
            return bytes2Hex(secret);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * AES解密
     * @param encoded
     * @param key 密钥
     * @return
     */
    static String decrypt(String encoded, String key) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), KEY_ALGORITHM));

//            byte[] secret = Base64Utils.decodeFromString(encoded);
            return new String(cipher.doFinal(encoded.getBytes()));
        } catch (Exception e) {
            throw e;
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
