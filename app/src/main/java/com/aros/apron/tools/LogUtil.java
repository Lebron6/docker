package com.aros.apron.tools;

import android.util.Log;

import com.aros.apron.xclog.XcFileLog;

public class LogUtil {
    public static void log(String tag,String content){
        Log.e(tag,content);
        XcFileLog.getInstace().i(tag,content);
    }
}
