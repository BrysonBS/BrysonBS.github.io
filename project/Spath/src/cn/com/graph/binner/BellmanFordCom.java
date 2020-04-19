package cn.com.graph.binner;

import java.util.ArrayDeque;
import java.util.Arrays;

public class BellmanFordCom implements SPath{
    private int[] edgeTo;
    private double[] distTo;
    private int s; //起点

    public BellmanFordCom(Graph g, int s) {
        this.s = s;
        edgeTo = new int[g.V()];
        distTo = new double[g.V()];
        Arrays.fill(distTo,Double.POSITIVE_INFINITY);
        distTo[s] = 0;
        for(int i=0;i<g.V();++i) {
            for (int j = 0; j < g.V(); ++j)
                relax(g, j);

            SearchPath.panel.updateUI();    //更新界面
            SearchPath.frame.repaint();     //更新界面
        }
    }
    private void relax(Graph g,int v){

        if(SearchPath.thread == 2) return;    //结束线程
        SearchPath.showProcedure(v);    //显示查询过程

        for(int w : g.adj(v)){
            if(distTo[w] < distTo[v] + 1) continue;
            distTo[w] = distTo[v] + 1;
            edgeTo[w] = v;
        }
    }

    public boolean hasPathTo(int v){ return distTo[v] < Integer.MAX_VALUE; }
    public Iterable<Integer> pathTo(int v){
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int i=v;i!=s;i=edgeTo[i])
            stack.push(i);
        return stack;
    }
}
