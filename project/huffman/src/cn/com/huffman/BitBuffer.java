package cn.com.huffman;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.util.Arrays;

/**
 * BitterBuffer
 * 按bit位处理字节数组,在此仅实现使用到的方法
 */
public class BitBuffer {
    private long mark = -1;
    private long position = 0;
    private long limit;
    private long capacity;

    private byte[] hb;

    public BitBuffer(int byteCapacity) {
        this.hb = new byte[byteCapacity];
        this.limit = (long)byteCapacity << 3;
        this.capacity = limit;
    }
    private BitBuffer(byte[] array, long bitLength){
        this.hb = array;
        this.limit = bitLength;
        this.capacity = limit;
    }
    public static BitBuffer wrap(byte[] array, long bitLength){
        return new BitBuffer(array,bitLength);
    }


    //byte转无符号
    private int toUnsigned(byte value){ return value < 0 ? value & 0xff : value; }

    //转ByteBuffer
    public ByteBuffer toByteBuffer(){
        int length = (int) ((position & 7) == 0 ? position >>> 3 : (position >>> 3) + 1);
        ByteBuffer byteBuffer = ByteBuffer.wrap(hb,0,length);
        return byteBuffer;
    }
    //获取底层数组
    public byte[] array(){ return hb; }

    //取出bits位的值返回
    public long get(int bits){
        if(position + bits > limit) throw new RuntimeException("超出BitBuffer范围无法取出");
        if(bits > 64) throw new RuntimeException("单次取出超过64bit位,无法取出");
        int index = (int) (position >>> 3);
        int remain = (int)(8 - (position & 7));
        //更新position
        position += bits;
        int num = remain - bits;
        if(num >= 0){
            return (toUnsigned(hb[index]) >>> num) & ((1 << bits) - 1);
        }
        else{
            long code = toUnsigned(hb[index]) & ((1 << remain) - 1);
            return get(code,-num,index+1);
        }
    }
    private long get(long code,int bits,int index){
        if(bits <= 8){
            int num = 8 - bits;
            int tail = (toUnsigned(hb[index]) >>> num) & ((1 << bits) - 1);
            return (code << bits) | tail;
        }
        code = (code << 8) | toUnsigned(hb[index]);
        return get(code,bits-8,index+1);
    }

    //将code的bits位(低位->高位数bits位)放入
    public void put(long code,int bits){
        if(position + bits > limit) throw new RuntimeException("超出BitBuffer范围无法取出");
        if(bits > 64) throw new RuntimeException("单次放入超过64bit位,无法放入");
        int index = (int)(position >>> 3);
        int remain = (int)(8 - (position & 7));
        //更新position
        position += bits;
        int num = remain - bits;
        if(num >= 0){
            hb[index] |= code << num;
        }
        else{
            hb[index] |= code >>> -num;
            put(code,-num,index+1);
        }
    }
    private void put(long code,int bits,int index){
        if(bits <= 8){
            hb[index] |= code << 8-bits;
            return;
        }
        hb[index] |= code >>> (bits-=8);
        put(code,bits,index+1);
    }

    public long capacity(){ return capacity; }
    public BitBuffer clear(){
        mark = -1;
        position = 0;
        limit = capacity;
        Arrays.fill(hb,(byte)0); //重置为0
        return this;
    }
    public BitBuffer flip(){
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
    public boolean hasRemaining(){ return position < limit; }
    public long limit(){ return limit; }
    public BitBuffer limit(long newLimit){
        if ((newLimit > capacity) || (newLimit < 0))
            throw new IllegalArgumentException();
        limit = newLimit;
        if (position > limit) position = limit;
        if (mark > limit) mark = -1;
        return this;
    }
    public BitBuffer mark(){
        mark = position;
        return this;
    }
    //获取当前position值
    public long position(){ return position; }
    //设置当前position值
    public BitBuffer position(long newPosition){
        if ((newPosition > limit) || (newPosition < 0))
            throw new IllegalArgumentException();
        position = newPosition;
        if (mark > position) mark = -1;
        return this;
    }
    public long remaining(){ return limit-position; }
    public BitBuffer reset(){
        long m = mark;
        if (m < 0) throw new InvalidMarkException();
        position = m;
        return this;
    }
    public BitBuffer rewind(){
        position = 0;
        mark = -1;
        return this;
    }
}
