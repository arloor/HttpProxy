package com.arloor.proxyserver.Manman;

//好好看看 https://blog.csdn.net/a10615/article/details/51749321
// java int= c int
// java byte= c char
//
public class Transform {
    public static byte[] confusion(byte[] raw) {
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
}
