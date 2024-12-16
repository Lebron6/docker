package com.aros.apron.callback;

import static com.gosuncn.lib28181agent.Types.PTZ_DOWN;
import static com.gosuncn.lib28181agent.Types.PTZ_LEFT;
import static com.gosuncn.lib28181agent.Types.PTZ_LEFT_DOWN;
import static com.gosuncn.lib28181agent.Types.PTZ_LEFT_UP;
import static com.gosuncn.lib28181agent.Types.PTZ_RIGHT;
import static com.gosuncn.lib28181agent.Types.PTZ_RIGHT_DOWN;
import static com.gosuncn.lib28181agent.Types.PTZ_RIGHT_UP;
import static com.gosuncn.lib28181agent.Types.PTZ_UP;
import static com.gosuncn.lib28181agent.Types.PTZ_ZOOM_IN;
import static com.gosuncn.lib28181agent.Types.PTZ_ZOOM_OUT;
import static com.gosuncn.lib28181agent.Types.ZOOM_IN_CTRL;
import static com.gosuncn.lib28181agent.Types.ZOOM_OUT_CTRL;

import android.util.Log;

import androidx.annotation.NonNull;

import com.aros.apron.entity.Movement;
import com.aros.apron.manager.CameraManager;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.util.CameraControllerUtil;
import com.google.gson.Gson;
import com.gosuncn.lib28181agent.GS28181SDKManager;
import com.gosuncn.lib28181agent.Jni28181AgentSDK;
import com.gosuncn.lib28181agent.bean.MobilePosSubInfo;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoublePoint2D;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.gimbal.CtrlInfo;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode;
import dji.sdk.keyvalue.value.gimbal.GimbalResetType;
import dji.sdk.keyvalue.value.gimbal.GimbalSpeedRotation;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class MGS28181Listener implements GS28181SDKManager.listenerServerControl {

    private String TAG = getClass().getSimpleName();

    /**
     * 设备信息查询回调 onQueryDevInfoAll(long
     * sessionHandle, String deviceGBCode,String deviceName, String
     * devManufacturer,String devModel, String devFirmware, int channel);
     *
     * @param sessionHandle   会话句柄
     * @param deviceGBCode    设备国标编码
     * @param deviceName      设备名称
     * @param devManufacturer 设备生产商
     * @param devModel        设备型号
     * @param devFirmware     设备固件版本
     * @param channel         视频输入通道数
     * @return 错误码
     */
    @Override
    public void onQueryDevInfoAll(long sessionHandle, String deviceGBCode, String deviceName, String devManufacturer,
                                  String devModel, String devFirmware, int channel) {

        Jni28181AgentSDK.getInstance().responseDevInfoQuery(sessionHandle, PreferenceUtils.getInstance().getClientCode(), deviceName,
                devManufacturer, devModel, devFirmware, channel);
    }

    /**
     * 设备状态查询回调 onQueryDevStatus(long
     * sessionHandle, String deviceGBCode, String dateTime, String
     * errReason, boolean isEncode, boolean isRecord,boolean isOnline,
     * boolean isStatusOK);
     *
     * @param sessionHandle 会话句柄
     *                      * @param deviceGBCode 设备国标编码
     *                      * @param dateTime 设备时间和日期
     *                      * @param errReason 不正常工作原因
     *                      * @param isEncode 是否编码
     *                      * @param isRecord 是否录像
     *                      * @param isOnline 是否在线
     *                      * @param isStatusOK 是否正常工作
     *                      * @return 错误码
     */
    @Override
    public void onQueryDevStatus(long sessionHandle, String deviceGBCode, String dateTime, String errReason,
                                 boolean isEncode, boolean isRecord, boolean isOnline, boolean isStatusOK) {

        Jni28181AgentSDK.getInstance().responseDevStatusQuery(sessionHandle, PreferenceUtils.getInstance().getClientCode(), dateTime,
                errReason, isEncode, isRecord, isOnline, isStatusOK);
    }

    /**
     * Rtp 流回调
     * @param rtpErrCode 点流请求
     */
    @Override
    public void onRtpStreamErr(int rtpErrCode) {
        Log.e(TAG, "onRtpStreamErr:" + rtpErrCode);
    }

    /**
     * 透传数据回调
     *
     * @param transData 透传数据
     */
    @Override
    public void onTransDataReceive(String transData) {
        Log.e(TAG, "onTransDataReceive:" + transData);
    }

    /**
     * 云台 PTZ 控制 onPTZControl(int ctrlType,int
     * ptzType,int speedParam)
     *
     * @param ctrlType   0:停止 1:开始
	     * @param ptzType    PTZ 操作类型（参考 Types.PTZCtrlType）
     * @param speedParam 摄像头相关速度参数
     */
    @Override
    public void onPTZControl(int ctrlType, int ptzType, int speedParam) {
        GimbalSpeedRotation speedRotation = new GimbalSpeedRotation();
        CtrlInfo ctrlInfo = new CtrlInfo();
        LogUtil.log(TAG,"onPTZControl 参数："+ctrlType+"==="+ptzType+"==="+speedParam);
        float pitch = 0, yaw = 0;
        float up_down = speedParam, right_left = speedParam;
        if (ctrlType == 1) {
            switch (ptzType) {
                case PTZ_UP:
                    pitch = up_down;
                    break;
                case PTZ_DOWN:
                    pitch = -up_down;
                    break;
                case PTZ_RIGHT:
                    yaw = right_left;
                    break;
                case PTZ_LEFT:
                    yaw = -right_left;
                    break;
                case PTZ_LEFT_UP:
                    pitch = up_down;
                    yaw = -right_left;
                    break;
                case PTZ_LEFT_DOWN:
                    pitch = -up_down;
                    yaw = -right_left;
                    break;
                case PTZ_RIGHT_UP:
                    pitch = up_down;
                    yaw = right_left;
                    break;
                case PTZ_RIGHT_DOWN:
                    pitch = -up_down;
                    yaw = right_left;
                    break;
                case PTZ_ZOOM_IN://放大
                    //        需要设置的倍率，当前倍率
                    double ratios1 = Movement.getInstance().getCameraZoomRatios();
                    double v1 = CameraControllerUtil.searchZoom(1,ZOOM_IN_CTRL, ratios1);
                    //        变焦
                    CameraManager.getInstance().setCameraZoom(v1);
                    break;
                case PTZ_ZOOM_OUT://缩小
                    //        需要设置的倍率，当前倍率
                    double ratios2 = Movement.getInstance().getCameraZoomRatios();
                    double v2 = CameraControllerUtil.searchZoom(1,ZOOM_OUT_CTRL, ratios2);
                    //        变焦
                    CameraManager.getInstance().setCameraZoom(v2);
                    break;
            }
            
            speedRotation.setPitch((double) pitch);
            speedRotation.setYaw((double) yaw);
            ctrlInfo.setEnableGimbalLock(false);
            speedRotation.setCtrlInfo(ctrlInfo);
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateBySpeed), speedRotation, null);
        } else {
            ctrlInfo.setEnableGimbalLock(true);
            speedRotation.setCtrlInfo(ctrlInfo);
        }
    }

    /**
     * 焦点控制
     *
     * @param zoomType  焦点控制类型
     * @param winLength 播放窗口长度像素值
     * @param winWidth  播放窗口宽度像素值
     * @param lenX      拉框长度像素值
     * @param lenY      拉框宽度像素值 (这个值 imp 没有用)
     * @param midPointX 拉框中心的横轴坐标像素值
     * @param midPointY 拉框中心的纵轴坐标像素值
     */
    @Override
    public void onZoomControl(int zoomType, int winLength, int winWidth,
                              int lenX, int lenY, int midPointX, int midPointY) {
//        PT值小于零，云台回中
        if (winLength <= 0 && winWidth <= 0) {
            KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyGimbalReset), GimbalResetType.PITCH_YAW, null);
            return;
        }
        // 获取屏幕像素宽度 px
//        int width = Movement.getInstance().getWidth();
        int width = 1920;

        // 获取屏幕像素高度 px
        int height = 1080;
        double x = ((double) midPointX / winWidth) * width;
        double y = ((double) midPointY / winLength) * height;


//      计算角度移动云台，有对边和临边获取夹角转动
        double xx, yy;
        if (x > (width/2)) {
            xx = ((x - (width/2)) / (width/2)) * (getHFOV() / 2);//在xy为100的坐标系中的比例乘以视场角，得到在视场角中的xy长度，依赖计算夹角
        } else {
            xx = -(((width/2) - x) / (width/2)) * (getHFOV() / 2);
        }
        if (y > (height/2)) {//向下转，即负数
            yy = -((y - (height/2)) / (height/2)) * (getVFOV() / 2);
        } else {
            yy = (((height/2) - y) / (height/2)) * (getVFOV() / 2);
        }

        GimbalAngleRotation gimbalAngleRotation = new GimbalAngleRotation();

        double tarPitch = (Math.abs(yy)/yy)*Math.toDegrees(Math.atan(Math.abs(yy)/Movement.getInstance().getFocalLenght()));//反正切求角度
        double tarYaw =(Math.abs(xx)/xx)*Math.toDegrees(Math.atan(Math.abs(xx)/Movement.getInstance().getFocalLenght()));//反正切求角度;

        gimbalAngleRotation.setPitch(tarPitch);
        gimbalAngleRotation.setYaw(tarYaw);
        gimbalAngleRotation.setMode(GimbalAngleRotationMode.RELATIVE_ANGLE);

        KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle), gimbalAngleRotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                LogUtil.log(TAG, "云台转动成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "云台转动失败" + idjiError);
            }
        });


//        需要设置的倍率，当前倍率
        double ratios = Movement.getInstance().getCameraZoomRatios();
        ratios = CameraControllerUtil.searchZoom(0,ZOOM_OUT_CTRL, ratios);
//        变焦
        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                        ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), ratios,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "放缩成功");
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG, "放缩失败" + idjiError);
                    }
                });
//        对焦中间点
        DoublePoint2D d = new DoublePoint2D();
        d.setY(0.5);
        d.setX(0.5);
        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusTarget,
                ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), d, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "焦点控制对焦完成");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "焦点控制对焦失败" + idjiError);
            }
        });
    }

    @Override
    public void onMobilePosSub(int i, int i1, int i2) {

    }


    @Override
    public void onMobilePosSub(MobilePosSubInfo mobilePosSubInfo) {
        Log.e(TAG,"收到开始订阅的通知:"+new Gson().toJson(mobilePosSubInfo));
        Movement.getInstance().setSubId(mobilePosSubInfo.getiSubID());
    }

    //    获取水平视场角
    public double getHFOV() {
//        double re = 2 * Math.toDegrees(Math.atan(Movement.getInstance().getAngleH() / (2 * Movement.getInstance().getFocalLenght())));
        return Movement.getInstance().getAngleH();
    }

    //    获取垂直视场角
    public double getVFOV() {
//        double re = 2 * Math.toDegrees(Math.atan(Movement.getInstance().getAngleV() / (2 * Movement.getInstance().getFocalLenght())));
        return Movement.getInstance().getAngleV();
    }
}