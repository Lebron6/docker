package com.aros.apron.xclog;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date: 2016-12-06
 * Time: 16:26
 * Version 1.0
 */

public class XcFileLog extends BaseLog {

    private static XcLogBaseConfig mXcLogBaseConfig;

    private static XcFileLog mXcFileLog;
    private static DeleteHelper mDeleteHelper;

    /**
     * 初始化配置文件
     * */
    public static void init(XcLogBaseConfig xcLogBaseConfig){
        mXcLogBaseConfig = xcLogBaseConfig;
        if(xcLogBaseConfig == null){ // 默认配置
            mXcLogBaseConfig = new SimpleXcLogBaseConfig();
        }
        if(mXcFileLog == null){
            mXcFileLog = new XcFileLog();
            mDeleteHelper = new DeleteHelper(mXcLogBaseConfig);
            mDeleteHelper.deleteCacheLog(); // 删除过期缓存
        }
    }

    private XcFileLog(){
        this(mXcLogBaseConfig, mXcLogBaseConfig.getPreFixName()+"_"+fileNameDate()+".txt");
    }

    private XcFileLog(XcLogBaseConfig xcLogBaseConfig, String fileName) {
        super(xcLogBaseConfig, fileName);
    }

    public static XcFileLog getInstace(){
        return mXcFileLog;
    }

    private static String fileNameDate(){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(new Date());
    }



}
