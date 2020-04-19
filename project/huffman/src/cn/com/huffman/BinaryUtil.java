package cn.com.huffman;

public class BinaryUtil {
    public static void println(byte[] buff,int start,int len){
        String hex = "";
        for(int i = start;i<len;++i){
            byte dec = buff[i];
            String result = "";
            for(int j = 0;j<Byte.SIZE;++j){
                result += (dec>>>j) & 1;
            }
            hex += new StringBuilder(result).reverse()+" ";
        }
        System.out.println(hex);
    }
}
