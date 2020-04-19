package cn.com.graph.binner;

public interface SPath {
    boolean hasPathTo(int v);
    Iterable<Integer> pathTo(int v);
}
