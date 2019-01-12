package com.arloor.proxycommon.crypto.utils.cryptoimpl;

import com.arloor.proxycommon.crypto.utils.Cryptor;
import com.arloor.proxycommon.util.Base64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AES128 implements Cryptor {

    private static final Charset CHARSET=UTF_8;//所有string转byte都使用UTF-8

    private static byte[] key=getKey(cryptoKey);

    @Override
    public void decrypt(ByteBuf buf) {
        int lengh=buf.writerIndex();
        byte[] bytes=new byte[lengh];
        buf.readBytes(bytes);
//        System.out.println("解密以下： "+new String(bytes));
        byte[] decrypt=decrypt(bytes);
        buf.clear();
        buf.writeBytes(decrypt);
    }

    @Override
    public void encrypt(ByteBuf buf) {
        int lengh=buf.writerIndex();
        byte[] bytes=new byte[lengh];
        buf.readBytes(bytes);
//        System.out.println("加密以下： "+new String(bytes));
        byte[] encrypt=encrypt(bytes);
        buf.clear();
        buf.writeBytes(encrypt);
    }

    /**
     * 加密
     * @param source
     * @return 加密后的字节数组
     */
    public byte[] encrypt(byte[] source) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec=new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE,keySpec );
            return Base64.encode(cipher.doFinal(source));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 解密
     * @param encoded
     * @return 解密后的字节数组
     */
    public byte[] decrypt(byte[] encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(Base64.decode(encoded));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 由keyStr经过SHA256再取128bit作为秘钥
     * 这里SHA-256也可以换成SHA-1
     * @param keyStr
     * @return
     */
    public static byte[] getKey(String keyStr){
        byte[] raw=keyStr.getBytes(CHARSET);
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] key = sha.digest(raw);
        key = Arrays.copyOf(key, 16); // use only first 128 bit
        return key;
    }
    /**
     * 返回byte数组的16进制字符串
     * @param array
     * @return
     */
    public static String byte2Hex(byte[] array){
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