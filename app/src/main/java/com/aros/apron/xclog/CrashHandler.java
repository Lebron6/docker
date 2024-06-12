package com.aros.apron.xclog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Date: 2016-12-06
 * Time: 17:17
 * Version 1.0
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static String TAG = CrashHandler.class.getSimpleName();

    private static CrashHandler INSTANCE = new CrashHandler();

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String stringBuilder = "------Crash------\n" +
                writer.toString();
        XcFileLog.getInstace().e(TAG, stringBuilder);
        //如果处理了，让主程序继续运行3秒再退出，保证异步的写操作能及时完成
//        try {
//            Thread.sleep(3*1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

}
