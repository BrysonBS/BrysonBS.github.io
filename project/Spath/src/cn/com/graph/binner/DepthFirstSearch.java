package cn.com.graph.binner;

public class DepthFirstSearch implements SPath{
    private boolean[] marked;
    private int count;
    public DepthFirstSearch(Graph g, int s){
        marked = new boolean[g.V()];
        dfs(g,s);
    }
    private void dfs(Graph g,int v){

        SearchPath.showProcedure(v);    //路径

        marked[v] = true;
        count++;
        for(int w : g.adj(v)) {
            if (!marked[w]) dfs(g, w);
        }
    }
    public boolean marked(int v){ return marked[v]; }
    public int count(){ return count; }

    @Override
    public boolean hasPathTo(int v) { return false; }

    @Override
    public Iterable<Integer> pathTo(int v) { return null; }
}
