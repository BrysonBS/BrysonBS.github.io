package cn.com.graph.binner;

import java.util.Comparator;

public class IndexPQ<V> {
    private int[] pq;
    private int[] qp;
    private V[] values;
    private int n = 0;
    private Comparator<? super V> comparator;
    public IndexPQ(int capacity) {
        this.comparator = (V x,V y) ->{
            if (x instanceof Comparable && y instanceof Comparable)
                return ((Comparable) x).compareTo(y);
            else throw new RuntimeException("must extends Comparable");
        };
        pq = new int[capacity+1];
        qp = new int[capacity+1];
        values = (V[])new Object[capacity+1];
    }
    public IndexPQ(int capacity, Comparator<? super V> comparator) {
        this.comparator = comparator;
        pq = new int[capacity+1];
        qp = new int[capacity+1];
        values = (V[])new Object[capacity+1];
    }
    public void offer(int k,V v){
        pq[++n] = k;
        qp[k] = n;
        values[k] = v;
        swim(n);
    }
    public int poll(){
        int maxIndex = pq[1];
        exch(1,n);
        pq[n--] = -1;
        qp[maxIndex] = -1;
        values[maxIndex] = null;
        sink(1);
        return maxIndex;
    }
    public V pollV(){
        int maxIndex = pq[1];
        V v = values[maxIndex];
        exch(1,n);
        pq[n--] = -1;
        qp[maxIndex] = -1;
        values[maxIndex] = null;
        sink(1);
        return v;
    }
    public V peek(int k){ return values[k]; }
    public void change(int k,V v){
        values[k] = v;
        sink(qp[k]);
        swim(qp[k]);
    }
    public boolean contains(int k){ return k > 0 && k < qp.length && qp[k] > 0 && qp[k] <= n; }
    public boolean isEmpty(){ return n==0; }

    private void swim(int k){ while(k > 1 && less(k,k>>>1)) exch(k,k>>>=1); }
    private void sink(int k){
        while(k<<1 <= n){
            int j = k<<1;
            if(j<n && less(j+1,j)) ++j;
            if(less(j,k)) exch(j,k);
            k = j;
        }
    }
    private void exch(int i,int j){
        if(i == j) return;
        pq[i] = pq[i] ^ pq[j];
        pq[j] = pq[i] ^ pq[j];
        pq[i] = pq[i] ^ pq[j];
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }
    private boolean less(int i,int j){ return comparator.compare(values[pq[i]],values[pq[j]]) < 0; }
}
