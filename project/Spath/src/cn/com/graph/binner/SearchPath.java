package cn.com.graph.binner;

import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class SearchPath {
    public static JFrame frame;
    private static JToolBar toolBar;
    public static JPanel panel;
    private static JPopupMenu popupMenu;
    private static JComboBox<String> comboBox;
    private static JButton jtn;     //选项
    private static String comboItem; //保存comboBox值
    public static int thread = 0;   //标记线程 0:不存在线程 1:存在线程 2:需要中断线程
    private static HashMap<String,String> comboMap; //维护combox值
    private static SPath spath;     //最短路径查询结果对象
    private static Floyd floyd;     //floyd算法查询结果
    private static ArrayDeque<Integer> path = new ArrayDeque<>(); //路径
    private static HashMap<Integer,Integer> history = new HashMap(); //改动历史
    private static int rows = 20;    //行数
    public static int cols = 20;    //列数
    public static int[][] adj = new int[rows][cols];  //使用邻接矩阵表示图
    public static int w = 20;      //格子边长
    private static int size = 10;    //随机生成大小
    private static int start = -1;  //始点
    public static int end = -1;    //终点
    private static int value = -1; //记录点击时的格子
    private static int prerow = -1; //记录拖拽上一个格子
    private static int precol = -1; //记录拖拽上一个格子
    private static int preValue = -1; //记录拖拽时的格子的值
    private static boolean isDrag = true; //是否需要拖动
    private static int delay = 5;      //查询时间间隔
    private static JDialog colorDialog; //颜色选择面板
    private static HashMap<String,JButton> colorBtns = new HashMap<>();   //颜色选择按钮
    private static Color defalutColor;
    private static Color ori = new Color(0XFF00FF);
    private static Color endC = new Color(0XFF0000);
    private static Color block = new Color(0X7EC0EE);
    private static Color pathC = new Color(0X696969);
    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        initEdit();
        frame = new JFrame();
        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panel.updateUI();
                frame.repaint();
                frame.pack();
            }
        });
        container.add(toolBar,BorderLayout.NORTH);


        //列表维护
        comboMap = new LinkedHashMap<>();
        comboMap.put("DepthFirstSearch","深度优先搜索");
        comboMap.put("ShortPathDFS","深度优先搜索-路径");
        comboMap.put("BreadthFirstPaths","广度优先搜索-路径");
        comboMap.put("Dijkstra","Dijkstra算法");
        comboMap.put("BellmanFordCom","BellmanFord-common算法");
        comboMap.put("BellmanFord","BellmanFord算法");
        comboMap.put("Floyd","Floyd算法");
        comboMap.put("AStar","A*算法");

        setPopBtn();
        setCombox();
        setGlobalHotKey();

        setStartAndEnd();

        //w = 20;rows = 20;cols = 30;size = 10;
        //adj = new int[rows][cols];
        panel = getPanel(adj,w);
        panel.setPreferredSize(new Dimension(cols*w,rows*w));
        container.add(panel);

        setPanelAction();

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public static void setStartAndEnd(){
        JColorChooser colorChooser = new JColorChooser();
        AbstractColorChooserPanel[] colorPanl = colorChooser.getChooserPanels();
        colorChooser.removeChooserPanel(colorPanl[0]);
        //colorChooser.removeChooserPanel(colorPanl[1]);
        colorChooser.removeChooserPanel(colorPanl[2]);
        colorChooser.removeChooserPanel(colorPanl[4]);
        ActionListener actionListener = ea ->{
            colorDialog = JColorChooser.createDialog(frame, "颜色选择", true, colorChooser,
                    eok -> {
                        Color choose= colorChooser.getColor();
                        String command = ea.getActionCommand();
                        colorBtns.get(command).setBackground(choose);
                        switch(command){
                            case "ori":
                                ori = choose;
                                break;
                            case "end":
                                endC = choose;
                                break;
                            case "path":
                                pathC = choose;
                                break;
                            case "block":
                                block = choose;
                                break;
                            default:
                        }

                        colorDialog.dispose();
                        frame.repaint();
                        panel.updateUI();
                    },
                    ec -> { colorDialog.dispose(); }
            );
            colorDialog.pack();
            colorDialog.setVisible(true);
        };

        JButton btn = null;
        btn = new JButton("始");
        btn.setActionCommand("ori");
        btn.setBackground(ori);
        btn.setForeground(defalutColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.addActionListener(actionListener);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(25,25));
        colorBtns.put("ori",btn);
        toolBar.add(btn);

        btn = new JButton("终");
        btn.setActionCommand("end");
        btn.setBackground(endC);
        btn.setForeground(defalutColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.addActionListener(actionListener);
        btn.setPreferredSize(new Dimension(25,25));
        btn.setFocusable(false);
        colorBtns.put("end",btn);
        toolBar.add(btn);

        btn = new JButton("墙");
        btn.setActionCommand("block");
        btn.setBackground(block);
        btn.setForeground(defalutColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.addActionListener(actionListener);
        btn.setPreferredSize(new Dimension(25,25));
        btn.setFocusable(false);
        colorBtns.put("block",btn);
        toolBar.add(btn);

        btn = new JButton("路");
        btn.setActionCommand("path");
        btn.setBackground(pathC);
        btn.setForeground(defalutColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.addActionListener(actionListener);
        btn.setPreferredSize(new Dimension(25,25));
        btn.setFocusable(false);
        colorBtns.put("path",btn);
        toolBar.add(btn);

    }

    public static void initEdit() {
        start = -1;
        end = -1;
        value = -1;
        prerow = -1;
        precol = -1;
        preValue = -1;
        isDrag = true;
        ori = new Color(16711935);
        endC = new Color(16711680);
        block = new Color(8306926);
        pathC = new Color(6908265);
    }
    public static void setEdit() {
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                if(ch < '0' || ch > '9') e.setKeyChar('\0');
            }
        };
        final JDialog dialog = new JDialog(frame, "基本配置", true);
        //dialog.setLocation(MouseInfo.getPointerInfo().getLocation());
        dialog.setLocationRelativeTo(jtn);
        dialog.setResizable(false);
        Box box = Box.createVerticalBox();
        dialog.add(box);
        GridLayout grid = new GridLayout(5, 2);
        grid.setVgap(2);
        JPanel top = new JPanel(grid);
        JButton btn = new JButton("确定");
        btn.setActionCommand("submit");
        Box btnBox = Box.createHorizontalBox();
        btnBox.add(Box.createGlue());
        btnBox.add(btn);
        btnBox.add(Box.createGlue());
        box.add(top);
        box.add(Box.createVerticalStrut(10));
        box.add(btnBox);
        JLabel label = null;
        label = new JLabel("行:",JLabel.RIGHT);
        final JTextField rowF = new JTextField(rows + "");
        rowF.addKeyListener(keyListener);
        top.add(label);
        top.add(rowF);
        label = new JLabel("列:",JLabel.RIGHT);
        final JTextField colF = new JTextField(cols + "");
        colF.addKeyListener(keyListener);
        top.add(label);
        top.add(colF);
        label = new JLabel("随机生成数目:",JLabel.RIGHT);
        final JTextField sizeF = new JTextField(size + "", 15);
        sizeF.addKeyListener(keyListener);
        top.add(label);
        top.add(sizeF);
        label = new JLabel("单元格边长:",JLabel.RIGHT);
        final JTextField wF = new JTextField(w + "");
        wF.addKeyListener(keyListener);
        top.add(label);
        top.add(wF);

        label = new JLabel("查询间隔毫秒:",JLabel.RIGHT);
        final JTextField delayF = new JTextField(delay + "");
        delayF.addKeyListener(keyListener);
        top.add(label);
        top.add(delayF);

        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cur_rows = Integer.parseInt(rowF.getText());
                int cur_cols = Integer.parseInt(colF.getText());
                int cur_w = Integer.parseInt(wF.getText());
                size = Integer.parseInt(sizeF.getText());
                delay = Integer.parseInt(delayF.getText());
                if (cur_rows != rows || cur_cols != cols || cur_w != w) {
                    rows = cur_rows;
                    cols = cur_cols;
                    w = cur_w;
                    frame.remove(panel);

                    initEdit();
                    adj = new int[rows][cols];
                    panel = getPanel(adj, w);
                    panel.setPreferredSize(new Dimension(cols * w, rows * w));
                    setPanelAction();
                    frame.getContentPane().add(panel);
                    frame.pack();
                    //frame.setLocationRelativeTo(null);
                    frame.repaint();
                }

                dialog.dispose();
            }
        });
        dialog.pack();
        dialog.setVisible(true);
    }

    public static void setGlobalHotKey(){
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.addAWTEventListener(event -> {
            if(((InputEvent)event).isAltDown() && event.getID() == KeyEvent.KEY_PRESSED){
                switch(((KeyEvent) event).getKeyCode()){
                    case KeyEvent.VK_E:
                        getBtnAction().actionPerformed(new ActionEvent(new JButton(),-1,"edit"));
                        break;
                    case KeyEvent.VK_A:
                        getBtnAction().actionPerformed(new ActionEvent(new JButton(),-1,"create"));
                        break;
                    case KeyEvent.VK_C:
                        getBtnAction().actionPerformed(new ActionEvent(new JButton(),-1,"clear"));
                        break;
                    case KeyEvent.VK_S:
                        getBtnAction().actionPerformed(new ActionEvent(new JButton(),-1,"start"));
                        break;
                    case KeyEvent.VK_I:
                        getBtnAction().actionPerformed(new ActionEvent(new JButton(),-1,"stop"));
                        break;
                    default:
                }
            }
        },AWTEvent.KEY_EVENT_MASK);
    }
    public static void setPopBtn(){
        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(false);

        JMenuItem item = null;
        item = new JMenuItem("配置");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK));
        item.setActionCommand("edit");
        item.addActionListener(getBtnAction());
        popupMenu.add(item);

        item = new JMenuItem("生成");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,KeyEvent.ALT_DOWN_MASK));
        item.setActionCommand("create");
        item.addActionListener(getBtnAction());
        popupMenu.add(item);

        item = new JMenuItem("清除");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,KeyEvent.ALT_DOWN_MASK));
        item.setActionCommand("clear");
        item.addActionListener(getBtnAction());
        popupMenu.add(item);

        item = new JMenuItem("开始");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,KeyEvent.ALT_DOWN_MASK));
        item.setActionCommand("start");
        item.addActionListener(getBtnAction());
        popupMenu.add(item);

        item = new JMenuItem("终止");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,KeyEvent.ALT_DOWN_MASK));
        item.setActionCommand("stop");
        item.addActionListener(getBtnAction());
        popupMenu.add(item);



        //popupMenu.setPopupSize(item.getSize());

        jtn = new JButton("选项");


        jtn.setPreferredSize(new Dimension(50,25));
        jtn.setFocusable(false);
        jtn.setFocusPainted(false);
        jtn.addActionListener(e -> {
            ((JButton)e.getSource()).setBackground(new Color(0x87CEFF));
            popupMenu.show(jtn,0,jtn.getHeight());
        });
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                jtn.setBackground(null);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        toolBar.add(jtn);
    }
    public static void setCombox(){

        toolBar.add(new JLabel("类别:"));
        comboBox = new JComboBox<>();
        comboBox.setMaximumSize(new Dimension(100,25));
        comboBox.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                setText(comboMap.get(value.toString()));//列表项显示的值
                Color bg = DefaultLookup.getColor(this, ui, "List.dropCellBackground");
                Color fg = DefaultLookup.getColor(this, ui, "List.dropCellForeground");
                if (isSelected) {
                    setBackground(bg == null ? list.getSelectionBackground() : bg);
                    setForeground(fg == null ? list.getSelectionForeground() : fg);
                }
                else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                return this;
            }
        });
        comboMap.keySet().forEach(k -> comboBox.addItem(k));    //列表项实际值

        comboBox.addItemListener(e ->{
            if(e.getStateChange() == ItemEvent.SELECTED)
                comboItem = e.getItem().toString();
        });
        toolBar.add(comboBox);
    }
    public static void setPanelAction(){
        panel.addMouseListener(new MouseAdapter() {
            int row = -1;
            int col = -1;
            long ms = 0;
            @Override
            public void mousePressed(MouseEvent e) {
                if(thread != 0) return;
                ms = System.currentTimeMillis();
                row = e.getY()/w;
                col = e.getX()/w;
                value = adj[row][col];
                if(value == 0 || value == 2) isDrag = false; //空白与路径不能拖动
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(thread != 0) return;

                //记录变动历史
                if(!history.containsKey(row*cols+col)) history.put(row*cols+col,value);
                if(!history.containsKey(prerow*cols+precol)) history.put(prerow*cols+precol,preValue);

                preValue = -1;
                prerow = -1;
                precol = -1;
                isDrag = true;
                if(System.currentTimeMillis() - ms > 100) { //拖拽结束时
                    if(value == 4 || ("Floyd".equals(comboItem) && value == 3)) {   //起点或终点拖动结束
                        setPath();
                        panel.updateUI();
                        frame.repaint();
                    }
                    value = -1;
                    return;
                }


                if(start < 0 || start >= rows * cols) {   //没有始点时,先生成始点
                    adj[row][col] = 3;
                    start = row*cols + col;
                    setPath();  //更新路径
                }
                else if(end < 0 || end >= rows * cols){ //没有终点,先生成终点
                    adj[row][col] = 4;
                    end = row*cols + col;
                    setPath();  //路径更新
                }
                else if(adj[row][col] == 0 || adj[row][col] == 2) adj[row][col] = 1;    //否则不是障碍物,变成障碍物
                else adj[row][col] = 0;     //否则为障碍物变成非障碍物

                if(row*cols + col == start && adj[row][col] != 3) start = -1;       //重新标记开始节点
                if(row*cols + col == end && adj[row][col] != 4) end = -1;          //重新标记终点

                panel.updateUI();
                frame.repaint();

            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if(!isDrag || thread != 0) return;
                int row = e.getY()/w;
                int col = e.getX()/w;

                //处理超出范围
                if(row < 0) row = 0;
                if(col < 0) col = 0;
                if(row >= rows) row = rows - 1;
                if(col >= cols) col = cols - 1;

                if(prerow != row || precol != col) {//有变动
                    int curValue = adj[row][col];
                    //1.还原上一个
                    if(prerow != -1 && precol != -1) adj[prerow][precol] = preValue;
                    //2.更新当前
                    adj[row][col] = value;
                    //3.更新始(start) 终(end)
                    if(curValue == 3) start = -1;
                    else if(curValue == 4) end = -1;
                    if(value == 3) start = row*cols + col;
                    else if(value == 4) end = row*cols + col;
                    //4.更新记录
                    if(prerow == -1 || precol == -1) preValue = 0;
                    else preValue = curValue;
                    precol = col;
                    prerow = row;

                    panel.updateUI();
                    frame.repaint();
                }

            }
        });
    }
    public static ActionListener getBtnAction(){
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                //已经生成过路径先清除路径
                if(!path.isEmpty()) {
                    path.forEach(ep -> { if(adj[ep/cols][ep%cols] == 2) adj[ep/cols][ep%cols] = 0; });
                    path.clear();
                }

                if( thread == 1 && !"stop".equals(command)){
                    JOptionPane.showMessageDialog(frame,"存在正在执行的任务");
                    return;
                }
                else if("create".equals(command)){
                    new Random().ints(size,0,rows*cols).forEach(es -> {
                        if(!history.containsKey(es)) history.put(es,adj[es/cols][es%cols]);
                        adj[es/cols][es%cols] = 1;
                    });
                    if(start >= 0 && start < rows * cols) adj[start/cols][start%cols] = 3;
                    if(end >= 0 && end < rows * cols) adj[end/cols][end%cols] = 4;
                }
                else if("start".equals(command)){
                    threadExecute();    //创建查询线程
                }
                else if("clear".equals(command)){
                    for(int i=0;i<adj.length;++i) Arrays.fill(adj[i],0);
                    start = -1;
                    end = -1;
                }
                else if("stop".equals(command)){
                    if(thread == 1) thread = 2;
                } else if ("edit".equals(command)) {
                    SearchPath.setEdit();
                }
                else;
                panel.updateUI();
                frame.repaint();
            }
        };
    }
    public static JPanel getPanel(int[][] adj,int w){
        int rows = adj.length;
        int cols = adj[0].length;
        return new JPanel(){
            {
                defalutColor = this.getBackground();
            }
            @Override
            public void paint(Graphics g) {
                for(int x=0;x<rows;++x)
                    for(int y=0;y<cols;++y){
                        g.setColor(Color.GRAY);
                        g.drawRect(y*w,x*w,w,w);
                        if(adj[x][y] == 1){         //障碍物
                            g.setColor(block);
                            g.fillRect(y*w+1,x*w+1,w-1,w-1);
                        }
                        else if(adj[x][y] == 2){    //最短路径
                            g.setColor(pathC);
                            g.fillRect(y*w+1,x*w+1,w-1,w-1);
                        }
                        else if(adj[x][y] == 3){    //始
                            g.setColor(ori);
                            //g.fillRect(y*w+1,x*w+1,w-1,w-1);
                            g.fillRoundRect(y*w+1,x*w+1,w-1,w-1,w-1,w-1);
                        }
                        else if(adj[x][y] == 4){    //终
                            g.setColor(endC);
                            //g.fillRect(y*w+1,x*w+1,w-1,w-1);
                            g.fillRoundRect(y*w+1,x*w+1,w-1,w-1,w-1,w-1);
                        }
                    }
            }
        };
    }


    public static void showProcedure(int v){
        int x = v/SearchPath.cols;
        int y = v%SearchPath.cols;
        Graphics gs = SearchPath.panel.getGraphics();
        gs.setColor(new Color(0, 0,0, 30));
        if(adj[x][y] != 3 && adj[x][y] != 4) gs.fillRect(y*SearchPath.w+1,x*SearchPath.w+1,SearchPath.w-1,SearchPath.w-1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void clearPath(int v){
        int x = v/SearchPath.cols;
        int y = v%SearchPath.cols;
        Graphics2D gs = (Graphics2D) SearchPath.panel.getGraphics();
        gs.setColor(defalutColor);
        if(adj[x][y] != 3 && adj[x][y] != 4) gs.fillRect(y*SearchPath.w+1,x*SearchPath.w+1,SearchPath.w-1,SearchPath.w-1);
    }

    public static void threadExecute(){
        history.clear();    //开始查询,清除修改历史

        if(!"Floyd".equals(comboItem) && (start < 0 || start >= rows * cols)) {
            JOptionPane.showMessageDialog(frame,"请先选择起点");
            return;
        }
        if("AStar".equals(comboItem) && (end < 0 || end >= rows * cols)) {
            JOptionPane.showMessageDialog(frame,"请先选择终点");
            return;
        }
        else if("Floyd".equals(comboItem)){
            spath = null;
            floyd = null;
            new Thread(() -> {
                Graph g = new Graph(adj);
                floyd = new Floyd(g);

                if (thread == 2) floyd = null;   //线程被中断,无结果
                setPath();  //更新路径

                panel.updateUI();
                frame.repaint();
                thread = 0;
            }).start();
        }
        else {
            new Thread(() -> {
                floyd = null;
                spath = null;
                history.clear();
                Graph g = new Graph(adj);
                String className = comboBox.getSelectedItem().toString();
                if (!className.contains("."))
                    className = SearchPath.class.getPackage().getName() + "." + className;
                try {
                    Class clazz = Class.forName(className);
                    Constructor<SPath> constructor = clazz.getConstructor(Graph.class, int.class);
                    spath = constructor.newInstance(g, start);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (thread == 2) spath = null;   //线程被中断,无结果
                setPath();  //更新路径

                panel.updateUI();
                frame.repaint();
                thread = 0;
            }).start();
        }
        thread = 1;
    }
    public static void setPath(){
        //对比
        for(Map.Entry<Integer,Integer> entry : history.entrySet()){
            if(entry.getKey() < 0 || entry.getKey() >= rows * cols) continue;
            int row = entry.getKey()/cols;
            int col = entry.getKey()%cols;

            //终点正常变动
            if(entry.getValue() == 4 && adj[row][col] == 0) continue;
            if(entry.getValue() == 0 && adj[row][col] == 4) continue;
            if(entry.getValue() == 2 && adj[row][col] != 1) continue;

            //起点正常变动
            if("Floyd".equals(comboItem)){
                if(entry.getValue() == 3 && adj[row][col] == 0) continue;
                if(entry.getValue() == 0 && adj[row][col] == 3) continue;
            }

            //地图有变动,无法在查找路径
            if(adj[row][col] != entry.getValue()) return;
        }


        //已经生成过路径先清除路径
        if(!path.isEmpty()) {
            path.forEach(ep -> { if(adj[ep/cols][ep%cols] == 2) adj[ep/cols][ep%cols] = 0; });
            path.clear();
        }

        if(end >= 0 && end < rows * cols && start >= 0 && start < rows * cols){ //起点终点存在时才能生成路径
            if(spath != null) {
                if (spath.hasPathTo(end)) {
                    history.clear();    //没有修改地图, 清除修改历史
                    spath.pathTo(end).forEach(es -> {
                        if (es != start && es != end) adj[es / cols][es % cols] = 2;
                        path.add(es);
                    });
                }
            }
            else if(floyd != null){
                if(floyd.hasPathTo(start,end)){
                    history.clear();    //没有修改地图, 清除修改历史
                    floyd.pathTo(start,end).forEach(es -> {
                        if (es != start && es != end) adj[es / cols][es % cols] = 2;
                        path.add(es);
                    });
                }
            }
        }
    }

}
