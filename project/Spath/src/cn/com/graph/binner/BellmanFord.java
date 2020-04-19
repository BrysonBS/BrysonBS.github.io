package cn.com.graph.binner;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class BellmanFord implements SPath{
    private double[] distTo;    //维护起点到其他顶点的权重,索引为其他顶点
    private int[] edgeTo;  //维护路径,顶点为索引,值为到达该顶点的边
    private boolean[] onQ;      //判断是否存在与队列中,顶点为索引,也可以使用队列的constains方法判断,但影响性能
    private Queue<Integer> queue;   //维护将要被放松的顶点
    private int s; //起点

    public BellmanFord(Graph g,int s) {
        this.s = s;
        edgeTo = new int[g.V()];
        onQ = new boolean[g.V()];
        queue = new ArrayDeque<>();
        distTo = new double[g.V()];
        Arrays.fill(distTo,Double.POSITIVE_INFINITY);
        distTo[s] = 0.0;
        queue.add(s);
        while(!queue.isEmpty()){
            int v = queue.poll();
            onQ[v] = false;
            relax(g,v);
        }
    }
    private void relax(Graph g, int v){

        if(SearchPath.thread == 2) return;    //结束线程
        SearchPath.showProcedure(v);    //显示查询过程

        for(int w : g.adj(v)){
            if(distTo[w] < distTo[v] + 1) continue;
            distTo[w] = distTo[v] + 1;
            edgeTo[w] = v;
            if(onQ[w]) continue;
            queue.offer(w);
            onQ[w] = true;
        }
    }
    public double distTo(int v){ return distTo[v]; }
    public boolean hasPathTo(int v){ return distTo[v] < Double.POSITIVE_INFINITY; }
    public Iterable<Integer> pathTo(int v){
        if(!hasPathTo(v)) return null;
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int x = v;x != s;x = edgeTo[x])
            stack.push(x);
        return stack;
    }
}
