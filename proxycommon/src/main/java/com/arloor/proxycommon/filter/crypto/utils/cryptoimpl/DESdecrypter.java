package com.arloor.proxycommon.filter.crypto.utils.cryptoimpl;

import com.arloor.proxycommon.filter.crypto.utils.Decrypter;
import io.netty.buffer.ByteBuf;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class DESdecrypter implements Decrypter {
    private String myEncryptionKey = "01234567";

    @Override
    public void decrypt(ByteBuf buf) {
        logger.debug("读时解密(DES)");
        int lengh = buf.writerIndex();
        byte[] bytes = new byte[lengh];
        buf.readBytes(bytes);
        byte[] plainText = decrypt(bytes, myEncryptionKey);
        buf.clear();
        buf.writeBytes(plainText);

    }

    public static byte[] decrypt(byte[] content, String key) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, securekey, random);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
