
package com.jby.apron.tools;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Locale;

import dji.v5.common.error.IDJIError;

public class Utils {
    public static void printJson(String tag, String json) {
        if (json == null || json.length() == 0) {
            Log.e(tag, "JSON is empty");
            return;
        }

        int maxLogSize = 4000;
        for (int i = 0; i <= json.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            if (end >= json.length()) {
                end = json.length();
            }
            Log.e(tag, json.substring(start, end));
        }
    }
    /**
     * //修改变焦数据为从前端拿2-200自己计算然后放入官方的sdk
     *
     * @param smallZoomFromWeb
     * @return
     */
    public static int getbigZoomValue(String smallZoomFromWeb) {
        int zoomLength = Integer.parseInt(smallZoomFromWeb);
        int bigZoom = (47549 - 317) / 199 * (zoomLength - 2) + 317;
        return bigZoom;
    }

    public static String sHA1(Context context){
        try {
            PackageInfo info = null;
            try {
                info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.GET_SIGNATURES);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getByte(String str){
        try {
            byte[] bytes = str.getBytes("UTF-8");
            return bytes;
            // 使用bytes
        } catch (UnsupportedEncodingException e) {
            // 处理异常，比如使用默认字符集
            byte[] bytes = str.getBytes();
            return null;
        }
    }

    public static double parseLatLon(String latLonStr) {
        // 检查输入字符串是否为空或无效
        if (latLonStr == null || latLonStr.isEmpty()) {
            throw new IllegalArgumentException("Input string is null or empty");
        }

        // 获取最后一个字符，即方向标识符
        char direction = latLonStr.charAt(latLonStr.length() - 1);

        // 检查方向标识符是否有效
        if (direction != 'N' && direction != 'S' && direction != 'E' && direction != 'W') {
return Double.parseDouble(latLonStr);        }

        // 提取数字部分
        String numberPart = latLonStr.substring(0, latLonStr.length() - 1);

        // 将数字部分转换为Double
        double number = Double.parseDouble(numberPart);

        // 根据方向标识符调整数值
        if (direction == 'S' || direction == 'W') {
            number = -number;
        }

        return number;
    }

    public static String getIDJIErrorMsg(IDJIError idjiError){
        if (TextUtils.isEmpty(idjiError.description())){
            return new Gson().toJson(idjiError);
        }else{
            return idjiError.description();
        }
    }

    public static String getSDCardPath() {
        String sdCardPathString = "";
        if (checkSDCard()) {
            sdCardPathString = Environment.getExternalStorageDirectory().getPath();
        } else {
            sdCardPathString = Environment.getExternalStorageDirectory()
                    .getParentFile()
                    .getPath();
        }
        return sdCardPathString;
    }

    public static boolean checkSDCard() {
        return TextUtils.equals(
                Environment.MEDIA_MOUNTED,
                Environment.getExternalStorageState()
        );
    }

}
