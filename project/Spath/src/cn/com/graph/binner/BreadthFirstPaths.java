package cn.com.graph.binner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;

public class BreadthFirstPaths implements SPath{
    private boolean[] marked;
    private int[] edgeTo;
    private final int s;

    public BreadthFirstPaths(Graph g, int s) {
        marked = new boolean[g.V()];
        edgeTo = new int[g.V()];
        this.s = s;
        bfs(g,s);
    }
    private void bfs(Graph g,int s){
        marked[s] = true;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.offer(s);
        while(!queue.isEmpty()){
            int v = queue.poll();

            if(SearchPath.end == v) break;        //遇到终点结束
            if(SearchPath.thread == 2) return;    //结束线程
            SearchPath.showProcedure(v);    //显示查询过程

            for(int w : g.adj(v)){
                if(marked[w]) continue;
                edgeTo[w] = v;
                marked[w] = true;
                queue.offer(w);
            }
        }
    }
    public boolean hasPathTo(int v){ return marked[v];}
    public Iterable<Integer> pathTo(int v){
        if(!hasPathTo(v)) return null;
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int x=v;x!=s;x=edgeTo[x]) stack.push(x);
        stack.push(s);
        return stack;
    }

}
