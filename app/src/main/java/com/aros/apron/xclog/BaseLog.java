package com.aros.apron.xclog;

import com.aros.apron.manager.AMSLogManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date: 2016-12-06
 * Time: 15:09
 * Version 1.0
 */

public abstract class BaseLog extends LogWriter{

    public BaseLog(XcLogBaseConfig xcLogBaseConfig, String fileName) {
        super(xcLogBaseConfig, fileName);
    }

    public void v(String tag, String msg){
        log("V", tag, msg);
    }

    public void d(String tag, String msg){
        log("D", tag, msg);
    }

    public void i(String tag, String msg){
        if (!AMSLogManager.getInstance().isUploadingAMSLog()){
            log("I", tag, msg);
        }
    }

    public void w(String tag, String msg){
        log("W", tag, msg);
    }

    public void e(String tag, String msg){
        log("E", tag, msg);
    }

    public void e(String tag, Throwable t){
        e(tag, String.valueOf(t));
    }

    public void f(String tag, String msg){
        if (!AMSLogManager.getInstance().isUploadingAMSLog()){
            log("F", tag, msg);
        }
    }

    /**
     * 记录日志到SD卡中
     * */
    public void log(String level, String tag, String msg){
        writerLog(level,printerLogTime() + " | " + level + " | " + tag + " | " + msg);
    }

    private String printerLogTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

}
