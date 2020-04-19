package cn.com.huffman;

import cn.com.compress.Compress;
import com.sun.org.apache.bcel.internal.generic.MONITORENTER;

import javax.swing.*;

public class Monitor{
    private int ms;     //时间
    private Timer timer;
    private JFrame frame;
    private ProgressMonitor monitor;
    public Monitor(JFrame frame) {
        this.frame = frame;
    }

    public void start(int max,String type){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                if("compress".equals(type)) message = "正在压缩...";
                else message = "正在解压...";
                monitor = new ProgressMonitor(frame,message,null,0,max);
                monitor.setProgress(0);
                monitor.setMillisToDecideToPopup(0);
                monitor.setMillisToPopup(1000);
                timer = new Timer(10,e ->{
                    monitor.setProgress(ms);
                    if(ms >= max) {
                        timer.stop();
                    }
                });
                timer.start();
            }
        }).start();
    }
    public void setMs(int ms){
        this.ms = ms;
    }
    public int getMs(){ return ms; }
    public void movement(int end){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(ms <= end){
                    ms += (end - ms)/100;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void close(){
        monitor.close();
    }
    public boolean isClosed(){
        if(monitor != null && monitor.isCanceled())
            return true;
        return false;
    }
}
