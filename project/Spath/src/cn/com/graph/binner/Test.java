package cn.com.graph.binner;

import java.lang.reflect.Constructor;

public class Test {
    public void testReflect(){

        Class clazz = null;
        String name = this.getClass().getPackage().getName();
        String clazzName = name+".BreadthFirstPaths";
        try {
            clazz = Class.forName(clazzName);
            Constructor con = clazz.getConstructor(Graph.class,int.class);
            Graph g = new Graph(new int[10][10]);
            con.newInstance(g,1);
            System.out.println(con.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
