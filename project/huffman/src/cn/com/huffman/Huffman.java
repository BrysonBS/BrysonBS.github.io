package cn.com.huffman;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.*;

/**
 * Huffman压缩
 * 概述: 通过huffman编码来重新表示字符,使得出现频率高的字符编码短,出现少的字符编码长
 *      整体来说,所需的总的bit位是减少的,但对于字符频率均匀的数据几乎没有压缩效果
 * 实现: 压缩后头部第1个字节存储压缩后结尾长度(结尾时可能一个字节没占满),
 *      紧接着4个字节存储压缩后字节长度(对于大文件分批次构建不同huffman树进行压缩,确定长度,可以分批次解压)
 * 细节: 分别使用bio与nio实现,解压时使用多线程并行优化
 */
public class Huffman {
    private int max = 1 << 24; //单次最大读取字节数,对nio性能影响大(ByteBuffer大小影响)
    //private int max = 1 << 23; //单次最大读取字节数,对nio性能影响大(ByteBuffer大小影响)
    private int R = 256;//码表范围
    private int bufferLength = 8192; //写入时缓冲区大小8K

    //huffman节点类
    private class Node implements Comparable<Node>{
        private char ch;
        private int freq;
        private Node left;
        private Node right;

        public Node(char ch, int freq, Node left, Node right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }
        public boolean isLeaf(){
            return left == null && right == null;
        }
        @Override
        public int compareTo(Node o) { return this.freq - o.freq; }
    }

    //压缩: 使用bio
    public void compress(InputStream in, OutputStream out){
        int length;
        byte[] buff = null;
        try{
            while (true) {
                //对于大文件分批次构建huffman树进行压缩
                length = (length = in.available()) > max ? max : length;
                if(buff == null) buff = new byte[length];
                length = in.read(buff);
                if(length == -1 || length == 0) break; //读取结束
                //统计频率
                int[] freq = new int[R];
                for (int i = 0; i < length; ++i)
                    freq[toUnsigned(buff[i])]++;
                //创建huffman树
                Node root = buildTrie(freq);
                //生成编码表(是用long类型,防止code超过范围)
                long[] st = new long[R];
                buildCodeTable(root, st, 0L, 0);
                //写入huffman树
                BitBuffer bitBuffer = new BitBuffer(length+(R << 1));
                bitBuffer.position(40);//第一个字节标记压缩数据末尾bit数,后面4个字节记录压缩后字节长度
                writeTrie(root,bitBuffer);
                //写入压缩数据
                long mixCode,code;
                int bit;
                for(int i = 0;i<length;++i){
                    mixCode = st[toUnsigned(buff[i])];
                    bit = (int) (mixCode & 0xff);
                    code = mixCode >>> 8;
                    bitBuffer.put(code,bit);
                }
                //维护头部5字节
                long gap = bitBuffer.position() - 40;
                byte tail = (byte) (gap & 7);
                //压缩数据长度(不包含头部长度5)
                int compressLength = (int) (tail == 0 ? gap >>> 3 : (gap >>> 3) + 1);
                long position = bitBuffer.position();
                bitBuffer.position(0);
                bitBuffer.put(tail,8);
                bitBuffer.put(compressLength >>> 16,16);
                bitBuffer.put(compressLength,16);
                //写入压缩数据
                byte[] buf = bitBuffer.array();
                int maxLength = (int) ((position & 7) == 0 ? position >>> 3 : (position >>> 3) + 1);
                //分段写入
                for(int k = 0;k<maxLength;k+=bufferLength){
                    if(k + bufferLength > maxLength)
                        out.write(buf,k,maxLength - k);
                    else
                        out.write(buf,k,bufferLength);
                }

            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //压缩: 使用nio
    public void nioCompress(FileInputStream in, FileOutputStream out){
        long length = 0;
        int capacity;
        try (FileChannel channel = in.getChannel();FileChannel outChannel = out.getChannel()) {
            long size = channel.size();
            //大文件分段压缩
            while(length < size){
                capacity = (int) ((length += max) <= size ? max : size - length + max);
                MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY,length-max,capacity);
                //统计频率
                int[] freq = new int[R];
                for (int i = 0; i < capacity; ++i)
                    freq[toUnsigned(mappedByteBuffer.get())]++;
                //创建huffman树
                Node root = buildTrie(freq);
                //生成编码表(是用long类型,防止编码过长)
                long[] st = new long[R];
                buildCodeTable(root, st, 0L, 0);
                //写入huffman树
                BitBuffer bitBuffer = new BitBuffer(capacity+(R << 1));
                bitBuffer.position(40);//第一个字节标记压缩数据末尾bit数,后面4个字节记录压缩后字节长度
                writeTrie(root,bitBuffer);
                //压缩数据放入字节数组
                long mixCode,code;
                int bit;
                mappedByteBuffer.flip();
                for(int i = 0;i<capacity;++i){
                    mixCode = st[toUnsigned(mappedByteBuffer.get())];
                    bit = (int) (mixCode & 0xff);
                    code = mixCode >>> 8;
                    bitBuffer.put(code,bit);
                }
                //维护头部5字节
                long gap = bitBuffer.position() - 40;
                byte tail = (byte) (gap & 7);
                //压缩数据长度(不包含头部长度5)
                int compressLength = tail == 0 ? (int)(gap >>> 3) : (int)((gap >>> 3) + 1);
                long position = bitBuffer.position();
                bitBuffer.position(0);
                bitBuffer.put(tail,8);
                bitBuffer.put(compressLength >>> 16,16);
                bitBuffer.put(compressLength,16);
                //写入压缩数据
                bitBuffer.position(position);
                ByteBuffer byteBuffer = bitBuffer.toByteBuffer();
                outChannel.write(byteBuffer);
            }

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //解压: 使用bio
    public void expand(InputStream in,OutputStream out){
        //确定线程数与容量
        int capacity = Runtime.getRuntime().availableProcessors();
        LinkedBlockingQueue<Future<ByteBuffer>> blockingQueue = new LinkedBlockingQueue<>(capacity);
        CountDownLatch latch = new CountDownLatch(1); //闭锁: 作用等待数据写入线程结束再放行
        //数据写入线程: 负责将解压数据写入
        Thread thread = new Thread(() ->{
            try {
                while(!blockingQueue.isEmpty()) {
                    Future<ByteBuffer> future = blockingQueue.poll();
                    ByteBuffer byteBuffer = future.get();
                    //写入
                    byte[] buf = byteBuffer.array();
                    int maxLength = byteBuffer.limit();
                    //分段写入
                    for(int k = 0;k<maxLength;k+=bufferLength){
                        if(k + bufferLength > maxLength)
                            out.write(buf,k,maxLength - k);
                        else
                            out.write(buf,k,bufferLength);
                    }
                }
                latch.countDown();
            } catch (Exception e) { //异常不做处理
                e.printStackTrace();
            }
        });

        try {
            //解压线程池: 负责解压数据
            ExecutorService executor = Executors.newWorkStealingPool(capacity);
            while(true) {
                //大文件可能分多段压缩,确定当前段长度
                byte[] head = new byte[5];
                int length = in.read(head); //读取头部信息
                if(length == -1 || length == 0) break;
                ByteBuffer headByte = ByteBuffer.wrap(head);
                int tailBit = headByte.get();//获取尾部bit位
                if (length != 5) throw new RuntimeException("头部信息缺失");
                length = headByte.getInt();//获取到当前段压缩字节长度
                byte[] buff = new byte[length]; //读取当前压缩段数据
                //读取压缩段
                long len = in.read(buff);
                if(length == -1 || length == 0) break;
                if (len != length) throw new RuntimeException("读取压缩信息失败");
                //解压并写入数据到byte数组: 并行提高效率
                Future<ByteBuffer> future = executor.submit(()->{
                    long bitLength = tailBit == 0 ? len << 3 : (len << 3) + tailBit - 8;
                    BitBuffer bitBuffer = BitBuffer.wrap(buff,bitLength);
                    //读取huffman树
                    Node root = readTrie(bitBuffer);
                    //解压后先写入byte数组
                    byte[] buf = new byte[max];
                    int bufIndex = 0;
                    //特殊处理: root为叶子节点,仅一个节点
                    if(root.isLeaf()){
                        long p = bitBuffer.position();
                        long limit = bitBuffer.limit();
                        for(;p<limit;p++)
                            buf[bufIndex++] = (byte)root.ch;
                        return ByteBuffer.wrap(buf,0,bufIndex);
                    }
                    //解码
                    while(bitBuffer.hasRemaining()){
                        Node x = root;
                        //此处每次读取1bit位处理,效率低,多线程并行优化
                        while(!x.isLeaf() && bitBuffer.hasRemaining()){
                            boolean bit = bitBuffer.get(1) == 1;
                            if(bit) x = x.right;
                            else x = x.left;
                        }
                        buf[bufIndex++] = (byte) x.ch;
                    }
                    return ByteBuffer.wrap(buf,0,bufIndex);
                });
                blockingQueue.put(future); //将任务放入阻塞队列,put放入速度必定比take处理速度快
                if(!thread.isAlive()) thread.start(); //启动数据写入线程
            }

            latch.await();
        } catch (Exception e) {//异常不做处理
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //解压: 使用nio
    public void nioExpand(FileInputStream in,FileOutputStream out){
        //确定线程数与容量
        int capacity = Runtime.getRuntime().availableProcessors();
        LinkedBlockingQueue<Future<ByteBuffer>> blockingQueue = new LinkedBlockingQueue<>(capacity);
        CountDownLatch latch = new CountDownLatch(1); //闭锁: 作用等待数据写入线程结束再放行
        //数据写入线程: 负责将解压数据写入
        Thread thread = new Thread(() ->{
            try {
                FileChannel outChannel = out.getChannel();
                while(!blockingQueue.isEmpty()) {
                    Future<ByteBuffer> future = blockingQueue.poll();
                    ByteBuffer byteBuffer = future.get();
                    //写入
                    outChannel.write(byteBuffer);
                }
                latch.countDown();
            } catch (Exception e) { //异常不做处理
                e.printStackTrace();
            }
        });


        try(FileChannel inChannel = in.getChannel()){
            //解压线程池: 负责解压数据
            ExecutorService executor = Executors.newWorkStealingPool(capacity);
            while(true) {
                //大文件可能分多段压缩,确定当前段长度
                byte[] head = new byte[5];
                ByteBuffer headByte = ByteBuffer.wrap(head);
                int length = inChannel.read(headByte);
                if(length == -1 || length == 0) break;
                headByte.flip();
                int tailBit = headByte.get();//获取尾部bit位
                if (length != 5) throw new RuntimeException("头部信息缺失");
                length = headByte.getInt();//获取到当前段压缩字节长度
                byte[] buff = new byte[length]; //读取当前压缩段数据
                ByteBuffer dataByte = ByteBuffer.wrap(buff);
                long len = inChannel.read(dataByte);
                if(length == -1 || length == 0) break;
                if (len != length) throw new RuntimeException("读取压缩信息失败");

                //解压并写入数据到byte数组: 并行提高效率
                Future<ByteBuffer> future = executor.submit(()->{
                    long bitLength = tailBit == 0 ? len << 3 : (len << 3) + tailBit - 8;
                    BitBuffer bitBuffer = BitBuffer.wrap(buff,bitLength);
                    //读取huffman树
                    Node root = readTrie(bitBuffer);
                    //解压后先写入byte数组
                    byte[] buf = new byte[max];
                    int bufIndex = 0;
                    //特殊处理: root为叶子节点,仅一个节点
                    if(root.isLeaf()){
                        long p = bitBuffer.position();
                        long limit = bitBuffer.limit();
                        for(;p<limit;p++)
                            buf[bufIndex++] = (byte)root.ch;
                        return ByteBuffer.wrap(buf,0,bufIndex);
                    }
                    //解码
                    while(bitBuffer.hasRemaining()){
                        Node x = root;
                        //此处每次读取1bit位处理,效率低,多线程并行优化
                        while(!x.isLeaf() && bitBuffer.hasRemaining()){
                            boolean bit = bitBuffer.get(1) == 1;
                            if(bit) x = x.right;
                            else x = x.left;
                        }
                        buf[bufIndex++] = (byte)x.ch;
                    }
                    return ByteBuffer.wrap(buf,0,bufIndex);
                });
                blockingQueue.put(future); //将任务放入阻塞队列,put放入速度比take处理速度快
                if(!thread.isAlive()) thread.start(); //启动数据写入线程
            }
            latch.await();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    //压缩: 文件夹/文件
    public void compress(File inFile,FileOutputStream out){
        Queue<Map.Entry<Integer,File>> queue = new ArrayDeque<>();
        Queue<File> fileQueue = new ArrayDeque<>();
        queue.offer(new AbstractMap.SimpleEntry<>(-1,inFile));
        //对文件名再编码: 防止同名文件名导致的问题
        Integer dKey = 0;//文件夹,被3整除
        Integer fKey = 1;//非空文件,余1
        Integer eKey = 2;//空文件,余2
        Integer key; //当前文件对应的编码
        StringBuilder table = new StringBuilder();
        StringBuilder paths = new StringBuilder();
        while(!queue.isEmpty()){
            Map.Entry<Integer,File> entity = queue.poll();
            Integer parentKey = entity.getKey();
            File file = entity.getValue();
            if(file.isDirectory()){
                key = dKey;
                dKey+=3;
                File[] files = file.listFiles();
                for(File f : files) queue.offer(new AbstractMap.SimpleEntry<>(key,f));
            }
            else {
                if (file.length() == 0) {//空文件
                    key = eKey;
                    eKey += 3;
                } else {
                    key = fKey;
                    fKey += 3;
                    fileQueue.offer(file);
                }
            }
            paths.append(parentKey);
            paths.append("/");
            paths.append(key);
            paths.append("?");

            table.append(key);
            table.append(":");
            table.append(file.getName());
            table.append("<");
        }

        table.append(">");
        table.append(paths);

        try{
            //记录文件名长度(int值)+文件名
            byte[] pathsBuff = table.toString().getBytes();
            int len = pathsBuff.length;
            byte[] head = new byte[]{(byte)(len >>> 24),(byte)(len >>> 16),(byte)(len >>> 8),(byte)len};
            out.write(head);
            out.write(pathsBuff);

            //依次压缩文件
            while(!fileQueue.isEmpty()){
                File file = fileQueue.poll();
                long length = 0;
                int capacity;
                try(FileChannel channel = new FileInputStream(file).getChannel()){
                    long size = channel.size();
                    //大文件分段压缩
                    while(length < size){
                        capacity = (int) ((length += max) <= size ? max : size - length + max);
                        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY,length-max,capacity);
                        //统计频率
                        int[] freq = new int[R];
                        for (int i = 0; i < capacity; ++i)
                            freq[toUnsigned(mappedByteBuffer.get())]++;
                        //创建huffman树
                        Node root = buildTrie(freq);
                        //生成编码表(是用long类型,防止编码过长)
                        long[] st = new long[R];
                        buildCodeTable(root, st, 0L, 0);
                        //写入huffman树
                        BitBuffer bitBuffer = new BitBuffer(capacity+(R << 1));
                        bitBuffer.position(40);//第一个字节标记压缩数据末尾bit数,后面4个字节记录压缩后字节长度
                        writeTrie(root,bitBuffer);
                        //压缩数据放入字节数组
                        long mixCode,code;
                        int bit;
                        mappedByteBuffer.flip();
                        for(int i = 0;i<capacity;++i){
                            mixCode = st[toUnsigned(mappedByteBuffer.get())];
                            bit = (int) (mixCode & 0xff);
                            code = mixCode >>> 8;
                            bitBuffer.put(code,bit);
                        }
                        //维护头部5字节
                        long gap = bitBuffer.position() - 40;
                        byte tail = (byte) (gap & 7);
                        //压缩数据长度(不包含头部长度5)
                        int compressLength = tail == 0 ? (int)(gap >>> 3) : (int)((gap >>> 3) + 1);
                        long position = bitBuffer.position();
                        bitBuffer.position(0);
                        //若到文件末尾时标记0001
                        if(length >= size) {
                            bitBuffer.put(1,4);
                            bitBuffer.put(tail,4);
                        }else{
                            bitBuffer.put(tail,8);
                        }
                        bitBuffer.put(compressLength >>> 16,16);
                        bitBuffer.put(compressLength,16);
                        //写入压缩数据
                        byte[] buf = bitBuffer.array();
                        int maxLength = (int) ((position & 7) == 0 ? position >>> 3 : (position >>> 3) + 1);
                        //分段写入
                        for(int k = 0;k<maxLength;k+=bufferLength){
                            if(k + bufferLength > maxLength)
                                out.write(buf,k,maxLength - k);
                            else
                                out.write(buf,k,bufferLength);
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                out.close();
                System.gc();//通知尽快释放MappedByteBuffer
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //解压: 文件夹/文件
    public void expand(FileInputStream in,File outFile){
        try{
            byte[] headBuff = new byte[4];
            int len = in.read(headBuff);
            if(len != 4) throw new RuntimeException("头部信息缺失");
            ByteBuffer headByte = ByteBuffer.wrap(headBuff);
            //获取文件名字符串长度
            int length = headByte.getInt();
            byte[] pathByte = new byte[length];
            //读取文件名字符串
            len = in.read(pathByte);
            if (len != length) throw new RuntimeException("读取压缩信息失败");
            String[] arrs = new String(pathByte).split(">");
            HashMap<Integer,String> hashMap = new HashMap<>(); //值: 文件名
            Queue<Map.Entry<Integer,File>> dirsQueue = new ArrayDeque<>();//保存文件夹键值
            //创建对照表
            for(String e : arrs[0].split("<")){
                String[] entity = e.split(":");
                hashMap.put(Integer.parseInt(entity[0]),entity[1]);
            }
            //创建文件
            File f;
            Integer key = -1;
            for(String e : arrs[1].split("\\?")){
                String[] entity = e.split("/");
                int parentKey = Integer.parseInt(entity[0]);
                int childKey = Integer.parseInt(entity[1]);
                int mod = childKey % 3;
                if(parentKey == -1 && mod == 0){ //处理文件夹根目录
                    f = new File(outFile,hashMap.get(childKey));
                    f.mkdir();
                    dirsQueue.offer(new AbstractMap.SimpleEntry<>(childKey,f));
                    outFile = f;
                    key = childKey;
                    continue;
                }
                Map.Entry<Integer,File> entry = null;
                while(key != parentKey){
                    entry = dirsQueue.poll();
                    key = entry.getKey();
                }
                outFile = entry == null ? outFile : entry.getValue();
                //处理
                f = new File(outFile,hashMap.get(childKey));
                if(mod == 0){ //文件夹
                    f.mkdir();
                    dirsQueue.offer(new AbstractMap.SimpleEntry<>(childKey,f));
                }
                else if(mod == 1){ //文件直接解压
                    f.createNewFile();
                    expandFile(in,new FileOutputStream(f));
                }
                else f.createNewFile(); //空文件
            }

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void expandFile(InputStream in,OutputStream out){
        //确定线程数与容量
        int capacity = Runtime.getRuntime().availableProcessors();
        LinkedBlockingQueue<Future<ByteBuffer>> blockingQueue = new LinkedBlockingQueue<>(capacity);
        CountDownLatch latch = new CountDownLatch(1); //闭锁: 作用等待数据写入线程结束再放行
        //数据写入线程: 负责将解压数据写入
        Thread thread = new Thread(() ->{
            try {
                while(!blockingQueue.isEmpty()) {
                    Future<ByteBuffer> future = blockingQueue.poll();
                    ByteBuffer byteBuffer = future.get();
                    //写入
                    byte[] buf = byteBuffer.array();
                    int maxLength = byteBuffer.limit();
                    //分段写入
                    for(int k = 0;k<maxLength;k+=bufferLength){
                        if(k + bufferLength > maxLength)
                            out.write(buf,k,maxLength - k);
                        else
                            out.write(buf,k,bufferLength);
                    }
                }
                latch.countDown();
            } catch (Exception e) { //异常不做处理
                e.printStackTrace();
            }
        });

        try {
            //解压线程池: 负责解压数据
            ExecutorService executor = Executors.newWorkStealingPool(capacity);
            while(true) {
                //大文件可能分多段压缩,确定当前段长度
                byte[] head = new byte[5];
                int length = in.read(head); //读取头部信息
                if(length == -1 || length == 0) break;
                ByteBuffer headByte = ByteBuffer.wrap(head);

                int bits = headByte.get();//获取尾部bit位
                boolean flag = false;
                if(bits > 7) {
                    bits &= 0xf;
                    flag = true;
                }

                int tailBit = bits;//获取尾部bit位
                if (length != 5) throw new RuntimeException("头部信息缺失");
                length = headByte.getInt();//获取到当前段压缩字节长度
                byte[] buff = new byte[length]; //读取当前压缩段数据
                //读取压缩段
                long len = in.read(buff);
                if(length == -1 || length == 0) break;
                if (len != length) throw new RuntimeException("读取压缩信息失败");

                //解压并写入数据到byte数组: 并行提高效率
                Future<ByteBuffer> future = executor.submit(()->{
                    long bitLength = tailBit == 0 ? len << 3 : (len << 3) + tailBit - 8;
                    BitBuffer bitBuffer = BitBuffer.wrap(buff,bitLength);
                    //读取huffman树
                    Node root = readTrie(bitBuffer);
                    //解压后先写入byte数组
                    byte[] buf = new byte[max];
                    int bufIndex = 0;
                    //特殊处理: root为叶子节点,仅一个节点
                    if(root.isLeaf()){
                        long p = bitBuffer.position();
                        long limit = bitBuffer.limit();
                        for(;p<limit;p++)
                            buf[bufIndex++] = (byte)root.ch;
                        return ByteBuffer.wrap(buf,0,bufIndex);
                    }
                    //解码
                    while(bitBuffer.hasRemaining()){
                        Node x = root;
                        //此处每次读取1bit位处理,效率低,多线程并行优化
                        while(!x.isLeaf() && bitBuffer.hasRemaining()){
                            boolean bit = bitBuffer.get(1) == 1;
                            if(bit) x = x.right;
                            else x = x.left;
                        }
                        buf[bufIndex++] = (byte) x.ch;
                    }
                    return ByteBuffer.wrap(buf,0,bufIndex);
                });
                blockingQueue.put(future); //将任务放入阻塞队列,put放入速度必定比take处理速度快
                if(!thread.isAlive()) thread.start(); //启动数据写入线程

                if(flag) break;
            }

            latch.await();
        } catch (Exception e) {//异常不做处理
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //byte转无符号
    private int toUnsigned(byte value){ return value < 0 ? value & 0xff : value; }
    //根据统计的频率freq构建huffman树
    private Node buildTrie(int[] freq){
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for(char c=0;c<R;++c)
            if (freq[c] > 0) pq.offer(new Node(c, freq[c], null, null));
        while(pq.size() > 1){
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node('\0',left.freq+right.freq,left,right);
            pq.offer(parent);
        }
        return pq.poll();
    }
    //根据huffman树构建huffman码表,st为码表,int值的前3个字节保存码值,最后1个字节保存码值长度
    private void buildCodeTable(Node x,long[] st,long code,int length){
        if(!x.isLeaf()){
            code <<= 1;
            buildCodeTable(x.left,st,code,length+1);
            buildCodeTable(x.right,st,code+1,length+1);
        }
        else if(length == 0){ //只有一个根节点
            st[x.ch] = 1<<8|1;
        }
        else {
            if (length > 56) throw new RuntimeException("错误,编码过长超过long[] st无法保存码表");
            st[x.ch] = code<<8|length; //7个字节(码值)+1个字节(码值长度)
        }
    }
    //将huffman树存储到字节数组中,非叶子节点0,叶子节点1后面紧跟8bit位为叶子节点的码值
    private void writeTrie(Node x,BitBuffer buff){
        if(x.isLeaf()){
            buff.put(1,1);
            buff.put(x.ch,8);
            return;
        }
        else buff.position(buff.position()+1);
        writeTrie(x.left,buff);
        writeTrie(x.right,buff);
    }
    //读取头部存储的huffman树并创建(读取到叶子节点会自动递归结束)
    private Node readTrie(BitBuffer buff){
        boolean isLeaf = buff.get(1) == 1;
        if(isLeaf){
            Node node = new Node((char)buff.get(8),-1,null,null);
            return node;
        }
        else return new Node('\0',-1,readTrie(buff),readTrie(buff));
    }
}