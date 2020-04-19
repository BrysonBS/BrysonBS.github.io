package cn.com.graph.binner;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Graph{
    private int v;
    private int e;
    private int[][] adj;
    public int rows;
    public int cols;
    public Graph(int[][] adj){
        this.adj = adj;
        rows = adj.length;
        cols = adj[0].length;
        v = rows*cols;
        e = 0;
    }
    public int V(){ return v; }
    public int E(){ return e;}
    public void addEdge(int v, int w){
        adj[v][w] = 1;
        e++;
    }
    public Iterable<Integer> adj(int v){
        int row = v/cols;
        int col = v%cols;
        if(v < 0 || v >= this.v) return null;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if(col != cols-1 && adj[row][col+1] != 1) queue.add(v+1);
        if(col != 0 && adj[row][col-1] != 1) queue.add(v-1);
        if(row != rows-1 && adj[row+1][col] != 1) queue.add(v+cols);
        if(row != 0 && adj[row-1][col] != 1) queue.add(v-cols);
        return queue;



/*        ++v;    //补正0
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if(v%cols != 0 && v+1 < rows*cols && adj[v/cols][v%cols] != 1) queue.add(v);
        if(v%cols != 1 && v-1 > 0 && adj[(v-2)/cols][(v-2)%cols] != 1) queue.add(v-2);
        if(v > 0 && v <= rows*cols-cols && adj[(v-1)/cols][(v-1)%cols] != 1) queue.add(v+cols-1);
        if(v > cols && v <= rows*cols && adj[(v-1)/cols][(v-1)%cols] != 1) queue.add(v-cols-1);
        return queue;*/
    }
}
