package cn.com.compress;

import cn.com.huffman.Huffman;
import cn.com.lzw.LZW;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Compress {
    private static JPanel panel;
    private static JButton btn;
    private static JPopupMenu popupMenu;
    private static JFileChooser chooser;
    public static JFrame frame;
    public static JDialog dialog;
    public static ExecutorService executor;
    public static int mark = 0; //0=huffman压缩; 1=lzw压缩
    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        frame = new JFrame();
        setBtn();
        setPanel();

        Container container = frame.getContentPane();

        setPopMenu(); //弹出菜单

        container.add(panel);
        panel.add(btn);

        drag(); //拖拽



        frame.setTitle("huffman/lzw压缩");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.pack();
        //frame.setSize(300,200);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
    }

    public static void setPopMenu(){
        popupMenu = new JPopupMenu();
        JMenuItem huffman = new JMenuItem("huffman");
        JMenuItem lzw = new JMenuItem("lzw");
        //图标
        URL url = Compress.class.getResource("/plus/mark16.png");
        ImageIcon icon = new ImageIcon(url);
        huffman.setIcon(icon);
        //事件
        ActionListener listener = e -> {
            JMenuItem item = (JMenuItem) e.getSource();
            if(item.getIcon() == null){
                item.setIcon(icon);
                if(item == huffman) {
                    mark = 0;
                    lzw.setIcon(null);
                }
                else{
                    mark = 1;
                    huffman.setIcon(null);
                }
            }
        };
        huffman.addActionListener(listener);
        lzw.addActionListener(listener);


        popupMenu.add(huffman);
        popupMenu.addSeparator();
        popupMenu.add(lzw);
        btn.setComponentPopupMenu(popupMenu);
    }
    public static void setBtn(){
        URL url = Compress.class.getResource("/plus/file_plus_72.png");
        btn = new JButton(new ImageIcon(url));
        btn.setFocusPainted(false);
        btn.setHorizontalTextPosition(JButton.CENTER);
        btn.setVerticalTextPosition(JButton.BOTTOM);
        btn.setPreferredSize(new Dimension(300,200));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooser.setDialogTitle("打开文件");
                chooser.setApproveButtonText("确定");
                int state = chooser.showOpenDialog(frame);
                if(state == JFileChooser.APPROVE_OPTION)
                    start(chooser.getSelectedFiles());
            }
        });
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    public static void setPanel(){
        panel = new JPanel(new GridLayout(1,1));
    }

    public static void huffmanThread(InputStream in, OutputStream out,boolean isCompress){
        new Thread(new Runnable() {
            @Override
            public void run() {
                cn.com.huffman.Huffman huffman = new cn.com.huffman.Huffman();
                if(isCompress) huffman.compress(in,out);
                else huffman.expand(in,out);
            }
        }).start();
    }
    public static void start(File[] files){
        executor = Executors.newSingleThreadExecutor();
        try {
            Huffman huffman = new Huffman();
            for(int i=0;i<files.length;++i) {
                File file = files[i];
                //文件夹直接压缩即可
                if(file.isDirectory()){
                    executor.submit(()->{
                        File outFile = new File(file.getParent(),file.getName()+".huffman");
                        if(outFile.exists()) {
                            JOptionPane.showMessageDialog(frame,"无法压缩,已存在文件"+outFile.getName());
                            return;
                        }
                        try {
                            //outFile.createNewFile();
                            huffman.compress(file,new FileOutputStream(outFile));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                else{//文件
                    FileInputStream input = new FileInputStream(file);
                    String fileName = file.getName();
                    int dot = fileName.lastIndexOf(".");
                    String suffix = fileName.substring( dot + 1, fileName.length());
                    if ("huffman".equals(suffix) && dot != -1) { //后缀为.huffman则解压
                        String name = fileName.substring(0, dot);
                        File expandFile = new File(file.getParent(), name);
                        if(expandFile.exists()) {
                            JOptionPane.showMessageDialog(frame,"无法解压,已存在文件或文件夹"+expandFile.getName());
                            return;
                        }
                        executor.submit(() -> huffman.expand(input,file.getParentFile()));
                    } else if("lzw".equals(suffix) && dot != -1){//后缀为.lzw则lzw解压
                        String name = fileName.substring(0, dot);
                        File expandFile = new File(file.getParentFile(), name);
                        if(expandFile.exists()) {
                            JOptionPane.showMessageDialog(frame,"无法解压,已存在文件或文件夹"+expandFile.getName());
                            return;
                        }
                        LZW lzw = new LZW();
                        FileOutputStream out = new FileOutputStream(expandFile);
                        executor.submit(() -> lzw.expand(input,out));
                    }
                    else {//否则进行压缩
                        if(mark == 1){
                            File compressFile = new File(file.getParentFile(), fileName + ".lzw");
                            if (compressFile.exists()) {
                                JOptionPane.showMessageDialog(frame, "无法压缩,已存在文件" + compressFile.getName());
                                return;
                            }
                            LZW lzw = new LZW();
                            FileOutputStream output = new FileOutputStream(compressFile);
                            executor.submit(() -> lzw.compress(input, output));
                        }
                        else {
                            String name = null;
                            if (dot != -1)
                                name = fileName.substring(0, dot) + "." + suffix;
                            else
                                name = suffix;
                            File compressFile = new File(file.getParent(), name + ".huffman");
                            if (compressFile.exists()) {
                                JOptionPane.showMessageDialog(frame, "无法压缩,已存在文件" + compressFile.getName());
                                return;
                            }
                            FileOutputStream output = new FileOutputStream(compressFile);
                            executor.submit(() -> huffman.compress(file, output));
                        }
                    }
                }
            }
            dialog = new JDialog(frame,"信息");
            //等待任务结束进度条
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);//设置进度条的样式为不确定的进度条样式（进度条来回滚动)
            progressBar.setStringPainted(true);//设置进度条显示提示信息
            progressBar.setString("正在执行...");//设置提示信息
            dialog.setSize(240,80);
            dialog.setLocationRelativeTo(frame);
            dialog.add(progressBar);
            frame.setEnabled(false);
            dialog.setVisible(true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(1);
                }
            });
            //结束:最后一个任务
            executor.submit(()->{
                dialog.dispose();
                frame.setEnabled(true);
            });
            executor.shutdown();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    public static void drag(){
        new DropTarget(panel, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try{
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){ //如果拖入的文件格式受支持
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);//接收拖拽来的数据
                        java.util.List<File> list =  (java.util.List<File>) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                        File[] files = new File[list.size()];
                        for(int i=0;i<list.size();++i)
                            files[i] = list.get(i);
                        start(files);
                        dtde.dropComplete(true);//指示拖拽操作已完成
                    }
                    else dtde.rejectDrop();//否则拒绝拖拽来的数据
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

}
