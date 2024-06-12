package com.aros.apron.xclog;

import android.os.Environment;
import android.text.TextUtils;

import java.util.concurrent.ExecutorService;

/**
 * Date: 2016-12-06
 * Time: 15:05
 * Version 1.0
 */

public class XcLogConfig implements XcLogBaseConfig {

    @Override
    public long getCacheSize() {
        return 30 * 1024 * 1014; // 30M 默认缓存大小
    }

    @Override
    public int getCacheEffectiveDays() {
        return 3;
    }

    @Override
    public String getLogDir() {
        return getSDCardPath()+"/DJIDemo/cache/log"; // 默认缓存路径
    }

    @Override
    public String getPreFixName() {
        return "TestLog";
    }

    @Override
    public ExecutorService getExecutorService() {
        return AsyncTask.mCachedSerialExecutor; // 配置线程池
    }

    private String getSDCardPath() {
        String sdCardPathString = "";
        if (checkSDCard()) {
            sdCardPathString = Environment.getExternalStorageDirectory()
                    .getPath();
        } else {
            sdCardPathString = Environment.getExternalStorageDirectory()
                    .getParentFile().getPath();
        }

        return sdCardPathString;
    }

    private boolean checkSDCard() {
        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }
}
