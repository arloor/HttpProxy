package com.arloor.proxycommon.util;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Base64 {
    public static byte[] encode(byte[] raw) {
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

    public static byte[] decode(byte[] confusioned){
        int a1=0;
        byte v6=0;
        byte v7=0;
        byte v8=0;
        byte v9=0;
        byte v10=0;
        byte v11=0;
        byte v12=0;
        byte[] v15="Pz#`(:7F-a%diHm<kQDTVEKXI68loAqwsGgC42!R^ju0h@xYc][}S9B{M~+t$.>,J".getBytes(UTF_8);
        v15= Arrays.copyOf(v15,v15.length+1);
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
}
