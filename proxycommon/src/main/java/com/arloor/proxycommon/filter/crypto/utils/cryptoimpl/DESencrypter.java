package com.arloor.proxycommon.filter.crypto.utils.cryptoimpl;

import com.arloor.proxycommon.filter.crypto.utils.Encrypter;
import io.netty.buffer.ByteBuf;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class DESencrypter implements Encrypter {
    private String myEncryptionKey="01234567";
    private SecretKey key ;
    private Cipher cipher;

    public DESencrypter() {
        DESKeySpec myKeySpec = null;
        try {
            KeyGenerator generator = KeyGenerator.getInstance("DES");
            SecureRandom secureRandom= SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(myEncryptionKey.getBytes());
            generator.init(secureRandom);
            key = generator.generateKey();
            cipher = Cipher.getInstance("DES");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

//    @Override
//    public void encrypt(ByteBuf buf) {
//        logger.debug("写时(DES)");
////        int lengh=buf.writerIndex();
////        byte[] bytes=new byte[lengh];
////        System.out.println(new String(bytes));
////        buf.readBytes(bytes);
////
////        try {
////            cipher.init(Cipher.ENCRYPT_MODE, key);
////            byte[] plainText = cipher.doFinal(bytes);
//////            System.out.println(new String(plainText));
////            buf.clear();
////            buf.writeBytes(plainText);
////        } catch (InvalidKeyException e) {
////            e.printStackTrace();
////        } catch (BadPaddingException e) {
////            e.printStackTrace();
////        } catch (IllegalBlockSizeException e) {
////            e.printStackTrace();
////        }
//    }

    @Override
    public void encrypt(ByteBuf buf) {
        logger.debug("写时加密(DES)");
        int lengh = buf.writerIndex();
        byte[] bytes = new byte[lengh];
        buf.readBytes(bytes);
        byte[] plainText = encrypt(bytes, myEncryptionKey);
        buf.clear();
        buf.writeBytes(plainText);

    }

    public static byte[] encrypt(byte[] content, String key) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
