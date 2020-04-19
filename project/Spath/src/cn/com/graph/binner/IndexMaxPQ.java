package cn.com.graph.binner;

import java.util.Comparator;

/**
 * 索引优先队列
 */
public class IndexMaxPQ<Key> {
    private int[] pq;  //最大堆,下标为元素位置,值是元素的索引
    private int[] qp;  //索引数组,下标是元素的索引,值为元素在最大堆pq中的位置,索引为i则 pq[qp[i]] = i
    private Object[] keys; //对象数组,下标为元素索引,值为元素
    private int n = 0; //记录元素个数
    private Comparator<? super Key> comparator; //比较器
    public IndexMaxPQ(int maxN, Comparator<? super Key> comparator){
        this.comparator = comparator;
        pq = new int[maxN+1];
        qp = new int[maxN+1]; //初始化默认值为0
        keys = new Object[maxN+1];
    }

    //无参构造方法
    public IndexMaxPQ(int maxN){
        comparator = (Key x,Key y) -> {
            if(x instanceof Comparable)
                return ((Comparable) x).compareTo(y);
            throw new RuntimeException("须传入比较器或者实现Comparable接口");
        };
        pq = new int[maxN+1];
        qp = new int[maxN+1]; //初始化默认值为0
        keys = new Object[maxN+1];
    }

    //插入索引为k的元素e
    public void insert(int k,Key e){
        pq[++n] = k;
        qp[k] = n;
        keys[k] = e;
        swim(n);
    }
    //删除最大元素并返回索引
    public int delMax(){
        int maxIndex = pq[1];
        exch(1,n--);
        sink(1);
        //删除映射
        pq[n+1] = -1;
        qp[maxIndex] = -1;
        keys[maxIndex] = null;
        return maxIndex;
    }
    //获取指定索引的值
    public Key peek(int k){ return (Key)keys[k]; }
    //将索引k的元素设置为key
    public void change(int k,Key e){
        keys[k] = e;
        //更新
        sink(qp[k]);
        swim(qp[k]);
    }
    //是否存在索引为k的元素
    public boolean contains(int k){
        return k>=0 && k <= qp.length && qp[k]>0 && qp[k]<=n;
    }
    //删除索引k及相关元素
    public void delete(int k){
        exch(qp[k],n--);
        sink(qp[k]);
        swim(qp[k]);
        //删除映射
        pq[n+1] = -1;
        qp[k] = -1;
        keys[k] = null;
    }
    //返回最大元素
    public Key maxKey(){ return (Key)keys[pq[1]]; }
    //返回最大元素的索引
    public int maxIndex(){ return pq[1]; }
    public boolean isEmpty(){ return n==0; }
    public int size(){ return n; }

    //上浮
    public void swim(int k){
        while(k > 1 && less(k>>>1,k)) exch(k,k>>>=1);
    }
    //下沉
    public void sink(int k){
        while(k<<1 <= n){
            int j = k<<1;
            if(j<n && less(j,j+1)) ++j;
            if(less(k,j)) exch(k,j);
            k = j;
        }
    }
    public void exch(int i, int j){
        if(i == j) return;
        //堆:交换
        pq[i] = pq[i] ^ pq[j];
        pq[j] = pq[i] ^ pq[j];
        pq[i] = pq[i] ^ pq[j];
        //更新索引数组
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }
    public boolean less(int i,int j){
        return comparator.compare((Key)keys[pq[i]],(Key)keys[pq[j]]) < 0;
    }


}
