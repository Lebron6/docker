package com.aros.apron.xclog;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;

/**
 * Date: 2016-12-06
 * Time: 18:39
 * Version 1.0
 *
 * 删除缓存
 */

public class DeleteHelper {

    private static String TAG = DeleteHelper.class.getSimpleName();
    private File mDir; // 缓存目录
    private long mCacheSize; // 缓存大小
    private int mCacheDays; // 缓存有效天数
    private ExecutorService mThreadTool;

    public DeleteHelper(XcLogBaseConfig xcLogBaseConfig){
        if(xcLogBaseConfig != null){
            mDir = new File(xcLogBaseConfig.getLogDir());
            mCacheDays = xcLogBaseConfig.getCacheEffectiveDays();
            mCacheSize = xcLogBaseConfig.getCacheSize();
            mThreadTool = xcLogBaseConfig.getExecutorService();
        }
    }

    /**
     * 删除缓存
     * */
    public void deleteCacheLog(){
        if(mThreadTool == null) return;
        if(mDir == null) return;
        if(!mDir.exists()) return;
        if(mCacheSize <= 0) return;
        if(mCacheDays <= 0) return;
        doDeleteCacheLogFile();
    }

    private void doDeleteCacheLogFile(){
        final File[] files = mDir.listFiles();
        if(files == null || files.length==0) return;
        mThreadTool.execute(new Runnable() {
            @Override
            public void run() {
                long actualCacheSize = getActualCacheSize(files);
                if(actualCacheSize >= mCacheDays){
                    // 删除操作
                    doDeleteLogForDate(files);
                }
            }
        });
    }

    /**
     * 根据过期实际删除日志
     * */
    private void doDeleteLogForDate(File[] files){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DATE, -mCacheDays);
        for (File file :files){
            String fileName = file.getName();
            if(checkExpired(calendar, file)){
                boolean delete = file.delete();
                if(delete){
                    // 删除文件成功
                    XcFileLog.getInstace().i(TAG, "删除文件成功:"+fileName);
                } else {
                    // 删除文件失败
                    XcFileLog.getInstace().i(TAG, "删除文件失败:"+fileName);
                }
            }
        }
    }

    /**
     * 检查文件是否过期
     * */
    private boolean checkExpired(Calendar calendar, File file){
        try {
            long lastModified = file.lastModified();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(lastModified);
            return cal.before(calendar);
        } catch (Throwable e){
            return true; // 删除文件夹中不是日志的文件
        }
    }

    /**
     * 获取实际缓存的大小
     * */
    private long getActualCacheSize(File[] files){
        int cacheSzie = 0;
        for (File file :files){
            cacheSzie += file.length();
        }
        return cacheSzie;
    }


}
