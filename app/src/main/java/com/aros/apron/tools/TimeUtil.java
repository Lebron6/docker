package com.aros.apron.tools;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.O)
public class TimeUtil {

    // 将格式化器定义为静态常量，线程安全且可以复用，避免频繁创建对象
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将 ISO 8601 格式的时间字符串转换为中文习惯的格式
     * @param isoTime 例如："2026-05-16T01:41:52+08:00"
     * @return 例如："2026年5月16日 01:41:52"
     */
    public static String convertIsoToChineseFormat(String isoTime) {
        // 1. 解析 ISO 8601 字符串为 ZonedDateTime 对象
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoTime, INPUT_FORMATTER);
        
        // 2. 格式化为我们需要的中文展示格式
        return zonedDateTime.format(OUTPUT_FORMATTER);
    }

}