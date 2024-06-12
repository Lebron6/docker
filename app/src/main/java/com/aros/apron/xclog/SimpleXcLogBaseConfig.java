package com.aros.apron.xclog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Date: 2016-12-06
 * Time: 15:05
 * Version 1.0
 */

public class SimpleXcLogBaseConfig implements XcLogBaseConfig {

    @Override
    public long getCacheSize() {
        return 30 * 1024 * 1014; // 30M 默认缓存大小
    }

    @Override
    public int getCacheEffectiveDays() {
        return 30;
    }

    @Override
    public String getLogDir() {
        return "/mnt/data/android/"; // 默认缓存路径
    }

    @Override
    public String getPreFixName() {
        return "XcLog";
    }

    @Override
    public ExecutorService getExecutorService() {
        return Executors.newFixedThreadPool(2); // 配置线程池
    }
}
