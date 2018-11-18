package com.arloor.proxycommon.filter.crypto.utils.cryptoimpl;

import com.arloor.proxycommon.filter.crypto.utils.Decrypter;
import io.netty.buffer.ByteBuf;

import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class DESdecrypter implements Decrypter {
    private String myEncryptionKey="012345678901234567890123456789012345";
    private SecretKey key ;
    private Cipher cipher;

    public DESdecrypter() {
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

    @Override
    public void decrypt(ByteBuf buf) {
        logger.debug("读时解密(DES)");
//        int lengh=buf.writerIndex();
//        byte[] bytes=new byte[lengh];
//        buf.readBytes(bytes);
////        System.out.println(new String(bytes));
//
//        try {
//            cipher.init(Cipher.DECRYPT_MODE, key);
//            byte[] plainText = cipher.doFinal(bytes);
//            System.out.println(new String(plainText));
//            buf.clear();
//            buf.writeBytes(plainText);
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//        } catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//        }



    }
}
