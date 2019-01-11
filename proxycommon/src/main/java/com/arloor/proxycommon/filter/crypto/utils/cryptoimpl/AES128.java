package com.arloor.proxycommon.filter.crypto.utils.cryptoimpl;

import com.arloor.proxycommon.filter.crypto.utils.Cryptor;
import io.netty.buffer.ByteBuf;

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
        System.out.println("解密以下： "+new String(bytes));
        byte[] decrypt=decrypt(bytes);
        buf.clear();
        buf.writeBytes(decrypt);
    }

    @Override
    public void encrypt(ByteBuf buf) {
        int lengh=buf.writerIndex();
        byte[] bytes=new byte[lengh];
        buf.readBytes(bytes);
        System.out.println("加密以下： "+new String(bytes));
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
            return base64encode(cipher.doFinal(source));
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
            return cipher.doFinal(base64decode(encoded));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] base64encode(byte[] raw) {
        int v13 = raw.length;
        byte[] ebbytes = raw;
        byte[] v11 = new byte[((v13 + 2) / 3) * 4];//这里直接用byte数组
        for (int i = 0; i < v13; i += 3) {
            int v9 = 0;
            for (int j = i; j < i + 3; ++j) {
                v9 <<= 8;
                if (j < v13)
                    v9 |= ebbytes[j]&0xFF;//c语言unsigned char转int
            }
            //byte: 80 122 35 96 40 58 55 70 ............................44 74(共65个   v12[64]=74
            //这个byte[] 就是c语言下的char*的char[]
            byte[] v12 = "Pz#`(:7F-a%diHm<kQDTVEKXI68loAqwsGgC42!R^ju0h@xYc][}S9B{M~+t$.>,J".getBytes();
            //0
            v11[4 * (i / 3)] = v12[((v9 >> 18) & 0x3F)];
            //1
            v11[4 * (i / 3) + 1] = v12[((v9 >> 12) & 0x3F)];
            //2
            byte v7;
            if (i + 1 >= v13)
                v7 = v12[64];
            else {
                v7 = v12[((v9 >> 6) & 0x3F)];
            }
            v11[4 * (i / 3) + 2] = v7;
            //3
            byte v6;
            if (i + 2 >= v13)
                v6 = v12[64];
            else {
                v6 = v12[(v9 & 0x3F)];
            }
            v11[4 * (i / 3) + 3] = v6;
        }
        return v11;
    }
    public static byte[] base64decode(byte[] confusioned){
        int a1=0;
        byte v6=0;
        byte v7=0;
        byte v8=0;
        byte v9=0;
        byte v10=0;
        byte v11=0;
        byte v12=0;
        byte[] v15="Pz#`(:7F-a%diHm<kQDTVEKXI68loAqwsGgC42!R^ju0h@xYc][}S9B{M~+t$.>,J".getBytes(UTF_8);
        v15=Arrays.copyOf(v15,v15.length+1);
        v15[v15.length-1]=0;
        byte[] v16=confusioned;
        int a3=v16.length;
        int v14=a3/4;
        a1=3*((int)a3/4);
        byte[] v13=new byte[a1];
        for (int i = 0; i < v14; ++i ){
            //v6 = strchr(v15, *(char *)(v16 + 4 * i));
            int index1=indexOfbyte(v15,v16[4*i]);
            if(index1==-1){
                return null;
            }
            v6=v15[index1];
            v12=(byte)((4*index1));
            int index2=indexOfbyte(v15,v16[4*i+1]);
            if(index2==-1){
                return null;
            }
            v7=v15[index2];
            v11 = (byte)(index2);
            v13[3*i]=(byte)(v12+((index2 & 0x30) >> 4));

            int index3 = indexOfbyte(v15, v16[ 4 * i + 2]);
            if(index3==-1){
                return null;
            }
            v8=v15[index3];
            if ( index3 == 64 ) {
                a1 = 3 * i + 1;
                return Arrays.copyOf(v13,a1);
            }
            v10 = (byte)(index3);
            v13[3 * i + 1]=(byte)(16 * v11+((index3 & 0x3C) >> 2));
            int index4=indexOfbyte(v15,v16[ 4 * i + 3]);
            if(index4==-1){
                return null;
            }
            v9=v15[index4];
            if(index4 == 64){
                a1 = 3 * i + 2;
                return Arrays.copyOf(v13,a1);
            }
            v13[3 * i + 2] = (byte)((v10 << 6) + index4);

        }
        return v13 ;
    }

    private static int indexOfbyte(byte[] source,byte target){
        for (int j = 0; j <source.length ; j++) {
            if(source[j]==target){
                return j;
            }
        }
        return -1;
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