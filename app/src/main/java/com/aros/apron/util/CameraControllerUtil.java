package com.aros.apron.util;

import android.os.Environment;
import com.aros.apron.entity.H264Frame;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.gosuncn.lib28181agent.GS28181SDKManager;
import com.gosuncn.lib28181agent.Types;

import java.io.File;

public class CameraControllerUtil {

    private static class CameraControllerUtilHolder {
        private static final CameraControllerUtil INSTANCE = new CameraControllerUtil();
    }

    private CameraControllerUtil() {
    }

    public static final CameraControllerUtil getInstance() {
        return CameraControllerUtilHolder.INSTANCE;
    }

    private static final String TAG = "CameraControllerUtil";
    private GS28181SDKManager manager = GS28181SDKManager.getInstance();
    private String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    //    防抖相关
    public float debounceThresholdPitch = 1.0f;
    public float debounceThresholdYaw = 1.0f;
    public float preFrameYaw ;
    public float preFramePitch ;
    //    水平和垂直的云台补偿
    public static volatile float compensateYaw = 0.0f;
    public static volatile float compensatePitch = 0.0f;
    FileUtil mFileUtil = FileUtil.getInstance();
    public static double searchZoom(int methodSource, int zoomType, double ratios){
        double zoomFactor = 0.8;
        if (methodSource == 1){//1，来自加减变焦
            if (ratios < 3.0){
                zoomFactor = 0.2;
            }else {
                zoomFactor = 0.8;
            }
        }
//        获取最接近的倍率下标
        int numberThree = getNumberThree(Movement.getInstance().getGears(), ratios);
        int len = Movement.getInstance().getGears().length;
        if (Movement.getInstance().isContinuous()) {//倍率连续
//            放缩不能超过倍率边界
            if (zoomType == Types.ZOOM_IN_CTRL)
                ratios = ratios + zoomFactor > Movement.getInstance().getGears()[len - 1] ? Movement.getInstance().getGears()[len - 1] : ratios + zoomFactor;//倍率递增
            else if (zoomType == Types.ZOOM_OUT_CTRL)
                ratios = ratios - zoomFactor < Movement.getInstance().getGears()[0] ? Movement.getInstance().getGears()[0] : ratios - zoomFactor;//倍率递减
        } else {//倍率不连续
            if (zoomType == Types.ZOOM_IN_CTRL) {//放大
//                如果最接近的下标是最大下标
                if (numberThree == len - 1) {
                    ratios = Movement.getInstance().getGears()[numberThree];
                } else if (Movement.getInstance().getGears()[numberThree] < ratios) {//如果最接近的下标值比当前要小，就用下一个值
                    ratios = Movement.getInstance().getGears()[numberThree + 1];
                } else if (isEqual(Movement.getInstance().getGears()[numberThree], ratios)) {//如果当前倍率刚好等于最接近的数
                    ratios = Movement.getInstance().getGears()[numberThree + 1];
                } else {//其他情况则正常用最接近的值
                    ratios = Movement.getInstance().getGears()[numberThree];
                }
            } else if (zoomType == Types.ZOOM_OUT_CTRL) {
                //   如果最接近的下标是最小下标
                if (numberThree == 0) {
                    ratios = Movement.getInstance().getGears()[numberThree];
                } else if (Movement.getInstance().getGears()[numberThree] > ratios) {//如果最接近的下标值比当前要大，就用上一个值
                    ratios = Movement.getInstance().getGears()[numberThree - 1];
                } else if (isEqual(Movement.getInstance().getGears()[numberThree], ratios)) {//如果当前倍率刚好等于最接近的数
                    ratios = Movement.getInstance().getGears()[numberThree - 1];
                } else {//其他情况则正常用最接近的值
                    ratios = Movement.getInstance().getGears()[numberThree];
                }
            }
        }
        LogUtil.log(TAG,"倍率连续否"+Movement.getInstance().isContinuous()+
                "当前倍率为"+ratios+
                "需要放缩至倍率为：==="+ratios);
        return ratios;
    }
    //    返回最接近的值的下标，自身也算
    public static int getNumberThree(int[] intarray, double number) {
        if (intarray!=null&&intarray.length>0){
            double temp = Math.abs(number - intarray[0]);
            int result = intarray[0];
            int j = 0;
            for (int i = 0 ;i<intarray.length;i++){
                double abs = Math.abs(number - intarray[i]);
                if (abs <= temp) {
                    temp = abs;
                    result = intarray[i];
                    j = i;
                }
            }
            LogUtil.log(TAG, "当前倍率为：" + number + "最接近的数为：" + result + "下标为：" + j);
            return j;
        }else{
            return 1;
        }

    }
    public static boolean isEqual(double a,double b)
    {
        return Math.abs(a-b) < 0.000001;
    }
    public void init(){

        // TODO: 2024/9/13  要延迟获取，不然空指针
        preFramePitch = Float.parseFloat(Movement.getInstance().getGimbalPitch());
        preFrameYaw = Float.parseFloat(Movement.getInstance().getGimbalYaw());
    }
    //计算PT
    public void countPT(float pitch, float yaw, float debounceThresholdYaw, float debounceThresholdPitch) {
        if (Math.abs(Math.abs(yaw) - Math.abs(this.preFrameYaw)) > debounceThresholdYaw) {
            this.preFrameYaw = yaw;
        }

        if (Math.abs(Math.abs(pitch) - Math.abs(this.preFramePitch)) > debounceThresholdPitch) {
            this.preFramePitch = pitch;
        }
    }
    public int  sendVideoStreamFun(int mimeType){
        int re;
        H264Frame enqueueFrame = mFileUtil.getEnqueueFrame();
        if (enqueueFrame == null) {
            return 2;
        }

        if (mimeType == 4) {
            re = manager.sendVideoStream(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData());
            LogUtil.log(TAG, "发送视频流sendVideoStream:" + re);
        } else {
            re = manager.sendVideoStreamH265(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData());
            LogUtil.log(TAG, "发送视频流sendVideoStreamH265:" + re);
        }
        return re;
    }
//    public int sendVideoWithARInfoFun(int mimeType) {
//        int re;
//        H264Frame enqueueFrame = mFileUtil.getEnqueueFrame();
//        if (enqueueFrame == null) {
//            return 2;
//        }
//
//        countPT(Float.parseFloat(Movement.getInstance().getGimbalPitch()),
//                Float.parseFloat(Movement.getInstance().getGimbalYaw()),
//                debounceThresholdYaw, debounceThresholdPitch);
//        if (mimeType == 4) {
//            re = manager.sendVideoWithARInfo(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(Movement.getInstance().getGimbalRoll()),
//                    preFramePitch+ compensatePitch,
//                    preFrameYaw+compensateYaw,
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude());
////                        LogUtil.log(TAG,"经度纬度高度111111"+Float.parseFloat(Movement.getInstance().getGimbalRoll())+" : "+
////                                Float.parseFloat(Movement.getInstance().getGimbalPitch())+" : "+
////                                Float.parseFloat(Movement.getInstance().getGimbalYaw())+" : "+
////                                Float.parseFloat(Movement.getInstance().getCurrentLongitude())+" : "+
////                                Float.parseFloat(Movement.getInstance().getCurrentLatitude())+" : "+
////                                Movement.getInstance().getAltitude());
//            LogUtil.log(TAG, "发送完整的帧 + 摄像机姿态信息（ AR 信息）sendVideoWithARInfo:" + re);
//        } else {
//            re = manager.sendVideoWithARInfoH265(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(Movement.getInstance().getGimbalRoll()),
//                    preFramePitch+compensatePitch,
//                    preFrameYaw+compensateYaw,
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude());
//            LogUtil.log(TAG, "发送完整的帧 + 摄像机姿态信息（ AR 信息）sendVideoWithARInfoH265:" + re);
//        }
//        return re;
//    }
//    public int sendVideoWithARInfoXFun(int mimeType){
//        int re;
//        H264Frame enqueueFrame = mFileUtil.getEnqueueFrame();
//        if (enqueueFrame == null) {
//            return 2;
//        }
//        countPT(Float.parseFloat(Movement.getInstance().getGimbalPitch()),
//                Float.parseFloat(Movement.getInstance().getGimbalYaw()),
//                debounceThresholdYaw, debounceThresholdPitch);
//        if (mimeType == 4) {
//            re = manager.sendVideoWithARInfoX(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(String.valueOf(Movement.getInstance().getCameraZoomRatios())),//double转float
//                    preFramePitch+compensatePitch, preFrameYaw+compensateYaw,
//                    Movement.getInstance().getAngleH(), Movement.getInstance().getAngleV(),
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude());
////            LogUtil.log(TAG, "发送 视频流与 AR信息流 上传sendVideoWithARInfoX:" + re);
//        } else {
//            re = manager.sendVideoWithARInfoXH265(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(String.valueOf(Movement.getInstance().getCameraZoomRatios())),//double转float
//                    preFramePitch+compensatePitch, preFrameYaw+compensateYaw,
//                    Movement.getInstance().getAngleH(), Movement.getInstance().getAngleV(),
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude());
////            LogUtil.log(TAG, "发送 视频流与 AR信息流 上传sendVideoWithARInfoXH265:" + re);
//        }
//        return re;
//    }
//    public int sendVideoWithARInfoToLocalFun(int mimeType) {
//        int re;
//        H264Frame enqueueFrame = mFileUtil.getEnqueueFrame();
//        if (enqueueFrame == null) {
//            return 2;
//        }
//        countPT(Float.parseFloat(Movement.getInstance().getGimbalPitch()),
//                Float.parseFloat(Movement.getInstance().getGimbalYaw()),
//                debounceThresholdYaw, debounceThresholdPitch);
//        if (mimeType == 4) {
//            LogUtil.log(TAG, "h264格式");
//            re = manager.sendVideoWithARInfoToLocal(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(String.valueOf(Movement.getInstance().getCameraZoomRatios())),//double转float
//                    preFramePitch+compensatePitch,
//                    preFrameYaw+compensateYaw,
//                    Movement.getInstance().getAngleH(), Movement.getInstance().getAngleV(),
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude(), absolutePath + File.separator+"111.h264");
//            LogUtil.log(TAG, "传输视频流与 AR 信息流，直接上传到指定路径 sendVideoWithARInfoToLocal:re=" + re);
//        } else {
//            re = manager.sendVideoWithARInfoToLocalH265(System.currentTimeMillis(), enqueueFrame.getFrameType(), enqueueFrame.getFrameData(),
//                    Float.parseFloat(String.valueOf(Movement.getInstance().getCameraZoomRatios())),//double转float
//                    preFramePitch+compensatePitch,
//                    preFrameYaw+compensateYaw,
//                    Movement.getInstance().getAngleH(), Movement.getInstance().getAngleV(),
//                    Float.parseFloat(Movement.getInstance().getCurrentLongitude()),
//                    Float.parseFloat(Movement.getInstance().getCurrentLatitude()),
//                    Movement.getInstance().getCurrentAltitude(), absolutePath + File.separator+ "111.h265");
//            LogUtil.log(TAG, "传输视频流与 AR 信息流，直接上传到指定路径 sendVideoWithARInfoToLocalH265:" + absolutePath + re);
//        }
//        return re;
//    }
}
