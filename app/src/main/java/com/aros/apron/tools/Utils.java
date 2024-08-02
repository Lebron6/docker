
package com.aros.apron.tools;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Utils {
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
}
