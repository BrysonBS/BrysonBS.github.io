package cn.com.graph.binner;

import java.util.ArrayDeque;

public class Floyd{
    private double[][] dist;
    private int[][] edge;
    public Floyd(Graph g) {
        dist = new double[g.V()][g.V()];
        edge = new int[g.V()][g.V()];
        for(int i=0;i<g.V();++i)
            for(int j=0;j<g.V();++j) {
                edge[i][j] = -1;
                if (i == j) {
                    dist[i][j] = 0.0;
                    edge[i][j] = i;
                }
                else dist[i][j] = Double.POSITIVE_INFINITY;
            }
        for(int v=0;v<g.V();++v){
            for(int w : g.adj(v)) {
                dist[v][w] = 1;
                edge[v][w] = v;
            }
        }
        floyd();
    }

    private void floyd(){
        for(int k=0;k<dist.length;++k)
            for(int i=0;i<dist.length;++i) {
                for (int j = 0; j < dist.length; ++j) {

                    if (SearchPath.thread == 2) return;    //结束线程
                    SearchPath.showProcedure(j);    //显示查询过程

                    if (dist[i][j] <= dist[i][k] + dist[k][j]) continue;
                    dist[i][j] = dist[i][k] + dist[k][j];
                    edge[i][j] = edge[k][j];
                }
                SearchPath.panel.updateUI();
                SearchPath.frame.repaint();
            }
    }

    public double disTo(int s,int v){ return dist[s][v];}
    public boolean hasPathTo(int s,int v){ return dist[s][v] < Double.POSITIVE_INFINITY; }
    public Iterable<Integer> pathTo(int s,int v){
        if(!hasPathTo(s,v)) return null;
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for(int i=v;i!=s;i=edge[s][i])
            stack.push(i);
        stack.push(s);
        return stack;
    }
}
