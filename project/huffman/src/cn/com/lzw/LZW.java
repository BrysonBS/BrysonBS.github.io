package cn.com.lzw;

import cn.com.huffman.BitBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * LZW算法
 * 概述: gif格式用到此算法思想,适合有大量重复值的数据,如果重复数据很少,反而会有反效果
 * 实现: 1.在LZW算法基础上使用变长的编码,从9bit位开始,用完时扩展,并使用最后一位L-1作为变更标记
 *      2.JDK1.7+之后substring方法性能下降,在此直接处理字节数组,不再像算法4中转为字符串处理
 *      3.压缩性能有待改进
 */
public class LZW {
    private int buffLength = 8192; //缓冲大小8M
    private int R = 256; //基本码表
    private int W = 9; //从9bit位开始扩展码表
    private int maxW = 24; //最大扩展bit位
    private int L = 1 << W; //码表大小

    /** 压缩 **/
    public void compress(InputStream in, OutputStream out){
        try {
            byte[] buff = new byte[in.available()];
            int length = in.read(buff);
            //基础表
            TST<Integer> st = new TST<>(); //稍作更改的三向单词查找树(直接处理字节数组)
            for(int i=0;i<R;++i)
                st.put(""+(char)i,i);
            int code = R+1;
            BitBuffer bitBuffer = new BitBuffer(length << 1);
            //压缩
            for(int i=0;i<length;){
                //获取最长前缀索引
                int index = st.longestPrefixof(buff,i,length-1);
                //压缩后写入缓冲区
                Integer value = st.get(buff,i,index);
                bitBuffer.put(value,W);
                //扩展表
                if(index < length-1 && code < L-1) //W位表没有满就扩展码表
                    st.put(buff,i,index+1,code++);
                else if(code == L-1){   //L-1标记当前M bit位码表编满,扩展码表为M+1 bit位
                    bitBuffer.put(L-1,W);
                    if(W == maxW){ //达到最大就重新创建码表
                        st = new TST<>();
                        for(int k=0;k<R;++k)
                            st.put(""+(char)k,k);
                        code = R+1;
                        W = 9;
                    }
                    else {  //没有达到最大就增加bit位扩展码表
                        code++;
                        if (index < length - 1)
                            st.put(buff, i, index + 1, code++);
                        W++;
                    }
                    L = 1 << W;
                }
                i = index + 1;
            }
            long position = bitBuffer.position();
            int len = (int)((position & 7 ) == 0 ? position >>> 3 : (position >>> 3) + 1);
            byte[] buf = bitBuffer.array();
            //分段写入
            for(int k = 0;k<len;k+=buffLength){
                if(k + buffLength > len)
                    out.write(buf,k,len - k);
                else
                    out.write(buf,k,buffLength);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** 解压 **/
    public void expand(InputStream in,OutputStream out){
        try {
            byte[] buff = new byte[in.available()];
            long length = in.read(buff);
            //不确定码表总大小,使用hashMap
            HashMap<Integer,String> st = new HashMap<>(L);
            int i;
            //基础表
            for(i=0;i<R;++i)
                st.put(i,""+(char)i);
            st.put(i++,"");
            BitBuffer bitBuffer = BitBuffer.wrap(buff,length << 3);
            if(bitBuffer.remaining() < W) return; //达到末尾结束
            //先取出第一个编码字符
            int codeword = (int) bitBuffer.get(W);
            String val = st.get(codeword); //解码第一个字符
            StringBuilder builder = new StringBuilder(); //存储解码后的字符串
            while(true){
                builder.append(val); //解码后存入StringBuilder
                if(bitBuffer.remaining() < W) break; //达到末尾结束
                codeword = (int) bitBuffer.get(W); //读取一个编码字符
                if(codeword == L-1){ //达到最大码表,增加一bit位或者重置
                    if(W == maxW){ //达到最大bit位重置码表
                        W = 9;
                        L = 1 << W;
                        st = new HashMap<>(L);
                        //基础表
                        for(i=0;i<R;++i)
                            st.put(i,""+(char)i);
                        st.put(i++,"");
                        if(bitBuffer.remaining() < W) return; //结束
                        codeword = (int) bitBuffer.get(W);
                        val = st.get(codeword);
                        continue;
                    }
                    else {
                        W++;
                        L = 1 << W;
                        if(bitBuffer.remaining() < W) return; //结束
                        codeword = (int) bitBuffer.get(W);
                        i++; //L-1作为标记,不再使用跳过
                    }
                }
                String s = st.get(codeword);
                //扩展表
                if(i == codeword) s = val+ val.charAt(0); //特殊情况,当前解码字符刚好是下一个要加入码表的字符
                if(i < L) st.put(i++,val+s.charAt(0));
                val = s; //更新
                if(builder.length() > buffLength){ //分次写入
                    out.write(builder.toString().getBytes("ISO8859-1"));
                    builder.delete(0,builder.length());
                }
            }
            //写入最后部分
            out.write(builder.toString().getBytes("ISO8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}