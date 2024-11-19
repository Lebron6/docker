
package com.aros.apron.tools;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.gosuncn.lib28181agent.bean.AngleEvent;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

    // NAL单元类型
    private static final int NAL_P = 1;
    private static final int NAL_B = 2;
    private static final int NAL_I = 5;

    private static final int NULL_FRAME = -1;
    private static final int I_FRAME = 1;
    private static final int P_FRAME = 2;
    private static final int B_FRAME = 3;
    private static final int Unknown = -1;

    // 判断帧类型
    public static int getFrameType(byte[] frame) {
        // 获取字节码流中的第一个NAL单元（假设视频帧数据是以NAL单元为基本单位）
        byte[] nalUnit = getFirstNalUnit(frame);
        if (nalUnit == null) {
            return Unknown; // 未知
        }

        int nalType = getNalType(nalUnit);
        switch (nalType) {
            case NAL_I:
                return I_FRAME; // I帧
            case NAL_P:
                return P_FRAME; // P帧
            case NAL_B:
                return B_FRAME; // B帧
            default:
                return Unknown; // 其他类型的NAL单元（例如SPS、PPS等）
        }
    }

    // 获取NAL单元类型
    private static int getNalType(byte[] nalUnit) {
        // H.264 NAL单元的类型位于起始字节的第1位到第5位
        return (nalUnit[0] & 0x1F);
    }

    // 从视频帧数据中提取第一个NAL单元
    private static byte[] getFirstNalUnit(byte[] frame) {
        // 这里简化处理，实际情况可能需要更复杂的逻辑来处理起始码
        int startCodeLength = 3;
        if (frame != null && frame.length >= startCodeLength) {
            // 查找起始码0x000001或0x00000001
            for (int i = 0; i < frame.length - startCodeLength; i++) {
                if ((frame[i] == 0x00 && frame[i + 1] == 0x00 && frame[i + 2] == 0x01) ||
                        (frame[i] == 0x00 && frame[i + 1] == 0x00 && frame[i + 2] == 0x00 && frame[i + 3] == 0x01)) {
                    return Arrays.copyOfRange(frame, i + startCodeLength, frame.length);
                }
            }
        }
        return null;
    }

    public static AngleEvent countCmos(float cmosH, float cmosW, double currentZoom) {
        AngleEvent angleEvent = new AngleEvent();
        float angleH = (float)(2.0D * Math.atan((double)(cmosW / (float)(2 * currentZoom))) * 360.0D / 2.0D / 3.141592653589793D);
        float angleV = (float)(2.0D * Math.atan((double)(cmosH / (float)(2 * currentZoom))) * 360.0D / 2.0D / 3.141592653589793D);
        angleEvent.setAngleH(angleH);
        angleEvent.setAngleV(angleV);
        return angleEvent;
    }

}
