package cn.com.graph.binner;

import java.util.ArrayDeque;
import java.util.Arrays;

public class Dijkstra implements SPath{
    private int[] edgeTo;
    private double[] distTo;
    private IndexMaxPQ<Double> pq;
    private int s; //起点

    public Dijkstra(Graph g, int s) {
        this.s = s;
        edgeTo = new int[g.V()];
        distTo = new double[g.V()];
        Arrays.fill(distTo,Double.POSITIVE_INFINITY);
        distTo[s] = 0;

        pq = new IndexMaxPQ<>(g.V(),(x,y) -> -x.compareTo(y));
        pq.insert(s,0.0);
        while(!pq.isEmpty()){
            int v = pq.delMax();
            if(SearchPath.end == v) break;     //遇到终点结束
            relax(g,v);
        }
    }
    private void relax(Graph g,int v){

        if(SearchPath.thread == 2) return;    //结束线程
        SearchPath.showProcedure(v);    //显示查询过程

        for(int w : g.adj(v)){
            if(distTo[w] < distTo[v]+1) continue;
            distTo[w] = distTo[v] + 1;
            edgeTo[w] = v;
            if(pq.contains(w)) pq.change(w,distTo[w]);
            else pq.insert(w,distTo[w]);
        }
    }
    public boolean hasPathTo(int v){ return distTo[v] < Double.POSITIVE_INFINITY; }
    public Iterable<Integer> pathTo(int v){
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int i=v;i!=s;i=edgeTo[i])
            stack.push(i);
        return stack;
    }
}
