package com.aros.apron.xclog;

import java.util.concurrent.ExecutorService;

/**
 * Date: 2016-12-06
 * Time: 15:03
 * Version 1.0
 *
 * 日志文件系统配置：
 * 日志缓存大小和缓存天数策略逻辑为：
 * 1. 缓存大小优先，即缓存大小没有超过最大缓存，则不执行删除操作
 */

public interface XcLogBaseConfig {

    long getCacheSize(); // 日志缓存大小

    int getCacheEffectiveDays(); // 日志缓存有效天数

    String getLogDir(); // 日志缓存路径

    String getPreFixName(); // 日志前缀，"XcLog", 比如：XcLog_20161206.txt

    ExecutorService getExecutorService(); // 配置线程池，对日志进行异步写入操作；不配置将使用默认的线程池；

}
