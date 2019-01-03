package com.arloor.proxyserver.Manman;

public class Transform {
    public static void main(String[] args){
        String raw="asdasd";
    }

    public static String confusion(String raw){
        int v13=raw.length();
        byte[] ebbytes=raw.getBytes();
        char[] v11=new char[((v13 + 2) / 3)];//不需要乘4
        for (int i = 0; i < v13; i += 3 ){
            int v9 = 0;
            for ( int j = i; j < i + 3; ++j )
            {
                v9 <<= 8;
                if ( j < v13 )
                    v9 |= ebbytes[j];//在这里忽略了指针转型*(Uint8 *)，个人觉得没关系
            }
            char[] v12 = "Pz#`(:7F-a%diHm<kQDTVEKXI68loAqwsGgC42!R^ju0h@xYc][}S9B{M~+t$.>,J".toCharArray();
            char temp= v11[4 * (i / 3)];

        }






        return null;
    }
}
