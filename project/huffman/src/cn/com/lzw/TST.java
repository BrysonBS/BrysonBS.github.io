package cn.com.lzw;

/**
 * 稍作改动的三向字符串查找树
 * 概述: 稍作改动可以直接处理字符数组
 */
public class TST<V> {
    private Node<V> root;//根节点
    private class Node<V>{  //节点
        private char c; //字符
        private Node<V> left; //左连接 <c
        private Node<V> mid;  //中连接 =c
        private Node<V> right;//右连接 >c
        private V value;    //保存键值
    }

    //获取: 键-buff[begin,end]组成的字符串
    public V get(byte[] buff,int begin,int end){
        Node x = get(root,buff,begin,end);
        if(x != null) return (V)x.value;
        if(x == null) System.out.println("x为null" + begin + ":" + end);
        return null;
    }
    private Node get(Node x,byte[] buff,int begin,int end){
        if(buff == null || buff.length == 0 || begin > end) return null;
        if(x == null) {
            System.out.println("x.为null" + begin+":"+end);
            return null;
        }
        char c = toUnsigned(buff[begin]);
        if(x.c > c) return get(x.left,buff,begin,end);
        else if(x.c < c) return get(x.right,buff,begin,end);
        else if(begin < end) return get(x.mid,buff,begin+1,end);
        else {
            if(x.value == null) System.out.println("begin:"+ begin+" end:"+end);
            return x;
        }
    }
    //插入: 键-byte数组buff[begin,end]组成的字符串,值-value
    public void put(byte[] buff,int begin,int end,V value){
        if(buff == null || buff.length == 0 || begin > end) return;
        root = put(root,buff,begin,end,value);
    }
    private Node put(Node x,byte[] buff,int begin,int end,V value){
        char c = toUnsigned(buff[begin]);
        if(x == null){
            x = new Node();
            x.c = c;
        }
        if(x.c > c) x.left = put(x.left,buff,begin,end,value);
        else if(x.c < c) x.right = put(x.right,buff,begin,end,value);
        else if(begin < end) x.mid = put(x.mid,buff,begin+1,end,value);
        else x.value = value;
        return x;
    }
    //获取最长前缀并所在byte数组的索引
    public int longestPrefixof(byte[] buff,int begin,int end){
        if(buff == null || buff.length == 0 || begin > end) return -1;
        int index = 0;
        Node x = root;
        while(x != null && begin <= end){
            char c = toUnsigned(buff[begin]);
            if(x.c > c) x = x.left;
            else if(x.c < c) x = x.right;
            else{
                if(x.value != null) index = begin;
                begin++;
                x = x.mid;
            }
        }
        return index;
    }

    //插入
    public void put(String key, V value){
        if(key == null || key.length() == 0) return;
        root = put(root,key,value,0);
    }
    /**插入: 与获取思路相同,只是遇到空节点创建新节点,到末尾时保存键值*/
    private Node put(Node x,String key,V value,int d){
        char c = key.charAt(d);
        if(x == null) { //没有节点就创建
            x = new Node();
            x.c = c;
        }
        if(x.c > c) x.left = put(x.left,key,value,d);
        else if(x.c < c) x.right = put(x.right,key,value,d);
        else if(d < key.length()-1) x.mid = put(x.mid,key,value,d+1);
        else x.value = value;   //末尾时保存键值
        return x;
    }

    //byte转无符号
    private char toUnsigned(byte value){ return (char)(value < 0 ? value & 0xff : value); }
}
