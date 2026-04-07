package com.jby.apron.tools;

import android.util.Log;

import com.jby.apron.xclog.XcFileLog;

public class LogUtil {
    public static void log(String tag,String content){
        Log.e(tag,content);
        XcFileLog.getInstace().i(tag,content);
    }
    public static void logA(String tag,String content){
//        Log.e(tag,content);
        XcFileLog.getInstace().i(tag,content);
    }
}
