package cn.com.graph.binner;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;

public class ShortPathDFS implements SPath{
    private boolean[] marked;
    private int count;

    private int[] edgeTo;
    private final int s;
    private int[] min;

    int i = 0;
    public ShortPathDFS(Graph g, int s) {
        marked = new boolean[g.V()];
        edgeTo = new int[g.V()];
        min = new int[g.V()];
        this.s = s;
        dfs(g,s);
    }
    private void dfs(Graph g,int v){

        if(SearchPath.thread == 2) return;    //结束线程
        SearchPath.showProcedure(v);    //显示查询过程

        marked[v] = true;
        ++count;
        min[v] = count;
        for(int w : g.adj(v)) {
            if (!marked[w]) {
                if(min[w] != 0 && min[w] < count+1) continue;
                edgeTo[w] = v;
                dfs(g, w);
            }
        }
        marked[v] = false;

        SearchPath.clearPath(v);    //递归结束清除

        --count;
    }
    public boolean hasPathTo(int v){ return min[v] != 0; }
    public Iterable<Integer> pathTo(int v){
        if(!hasPathTo(v)) return null;
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int x=v;x!=s;x=edgeTo[x]) stack.push(x);
        stack.push(s);
        return stack;
    }
    public boolean marked(int v){ return marked[v]; }

}
