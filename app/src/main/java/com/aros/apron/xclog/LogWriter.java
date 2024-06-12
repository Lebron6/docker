package com.aros.apron.xclog;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

/**
 * Date: 2016-12-06
 * Time: 15:26
 * Version 1.0
 *
 * 向指定文件写数据
 */

public class LogWriter {

    private ExecutorService mThreadPool;
    private File mDir;
    private String mFileName;

    private String LINE_SEPARATOR = System.getProperty("line.separator");

    public LogWriter(XcLogBaseConfig xcLogBaseConfig, String fileName){
        mDir = new File(xcLogBaseConfig.getLogDir());
        mFileName = fileName;
        mThreadPool = xcLogBaseConfig.getExecutorService();
    }

    /**
     * 创建新文件
     * */
    private File createFile(File file){
        try {
            if(!file.exists()){
                boolean successCreate = file.createNewFile();
                if(!successCreate){
                    return null;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return file;
    }


    /**
     * 开启新线程写数据
     * */
    public void writerLog(final String msg){
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("thread"+"   "+msg);
                synchronized (LogWriter.class){
                    if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                        return;
                    }
                    if(!mDir.exists()){
                        mDir.mkdirs();
                    }
                    mFileName=new SimpleDateFormat("yyyyMMdd").format(new Date())+".txt";
                    File logFile = new File(mDir+"/"+mFileName);
                    if(!logFile.exists()){ // 文件不存在，创建文件
                        createFile(logFile);
                    }
                    doWrtiter(logFile, msg+LINE_SEPARATOR);
                }
            }
        });
    }

    /**
     * 向文件写数据
     * */
    private void doWrtiter(File file, String content){
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(content.getBytes("UTF-8"));
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(fos != null){
                    fos.flush();
                    fos.close();
                }
            } catch (Exception e){

            }
        }
    }


}
