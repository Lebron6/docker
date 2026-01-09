package com.aros.apron.callback;


import android.os.Handler;
import android.os.Looper;

import com.aros.apron.app.ApronApp;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.CameraManager;
import com.aros.apron.manager.FlightManager;
import com.aros.apron.manager.GimbalManager;
import com.aros.apron.manager.MissionV3Manager;
import com.aros.apron.manager.StickManager;
import com.aros.apron.manager.StreamManager;
import com.aros.apron.manager.SystemManager;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.RestartAPPTool;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttCallBack implements MqttCallbackExtended {

    private String TAG = "MqttCallBack";


    @Override
    public void connectionLost(Throwable cause) {
        LogUtil.log(TAG, "MQtt connectionLost:"+cause.toString());

    }

    //断线重连
    public void reConnect() throws Exception {
        if (null != MqttManager.getInstance().mqttAndroidClient) {
            LogUtil.log(TAG, "MQtt reConnect-----");
        }
    }


    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        LogUtil.log(TAG, "--------" + mqttMessage.toString());
        String jsonString = null;
        try {
            jsonString = new String(mqttMessage.getPayload(), "UTF-8");
        } catch (Exception e) {
            LogUtil.log(TAG, "解析异常");
            throw new RuntimeException(e);
        }
        MessageDown message = new Gson().fromJson(jsonString, MessageDown.class);
        switch (message.getMethod()) {
            case Constant.PILOT_ON:
                LogUtil.log(TAG, "收到：遥控器是否开机" + jsonString);
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
                break;
            case Constant.AIRCRAFT_ON:
                LogUtil.log(TAG, "收到：飞机是否开机" + jsonString);
                SystemManager.getInstance().checkAircraftPowerStatus(message);
                break;
            case Constant.MEDIA_UPLOAD_COMPLETE:
                LogUtil.log(TAG, "收到：文件上传是否结束" + jsonString);
                SystemManager.getInstance().aircraftStoredReply(message);
                break;
            case Constant.OPEN_SLOW_PROPELLER_ROTATION:
                LogUtil.log(TAG, "收到：开启低速转浆" + jsonString);
                FlightManager.getInstance().startPropellerRotation(message);
                break;
            case Constant.CLOSE_SLOW_PROPELLER_ROTATION:
                LogUtil.log(TAG, "收到：停止低速转浆" + jsonString);
                FlightManager.getInstance().stopPropellerRotation(message);
                break;
            case Constant.LIVE_START_PUSH:
                LogUtil.log(TAG, "收到：开始直播" + jsonString);
                StreamManager.getInstance().startLive(message);
                break;
            case Constant.LIVE_SET_QUALITY:
                LogUtil.log(TAG, "收到：设置直播清晰度" + jsonString);
                StreamManager.getInstance().setLiveStreamQuality(message);
                break;
            case Constant.LIVE_LENS_CHANGE:
                LogUtil.log(TAG, "收到：切换直播镜头" + jsonString);
                CameraManager.getInstance().setCameraVideoStreamSource(message);
                break;
            case Constant.FLIGHTTASK_EXECUTE:
                LogUtil.log(TAG, "收到：航线" + jsonString);
                MissionV3Manager.getInstance().taskExecute(message);
                break;
            case Constant.FLIGHTTASK_PAUSE:
                LogUtil.log(TAG, "收到：航线暂停" + jsonString);
                MissionV3Manager.getInstance().pauseMission(message);
                break;
            case Constant.FLIGHTTASK_RECOVERY:
                LogUtil.log(TAG, "收到：航线继续" + jsonString);
                MissionV3Manager.getInstance().resumeMission(message);
                break;
            case Constant.RETURN_HOME:
                LogUtil.log(TAG, "收到：返航" + jsonString);
                FlightManager.getInstance().startGoHome(message);
                break;
            case Constant.RETURN_HOME_CANCEL:
                LogUtil.log(TAG, "收到：取消返航" + jsonString);
                FlightManager.getInstance().stopGoHome(message);
                break;
            case Constant.CLOSE_DOOR:
                LogUtil.log(TAG, "收到：服务端响应关舱门" + jsonString);
//                ApronExecutionStatus.getInstance().setServerReplyDockIn(true);
                break;
            case Constant.OPEN_DOOR:
                LogUtil.log(TAG, "收到：服务端响应开舱门" + jsonString);
                ApronExecutionStatus.getInstance().setServerReplyDockOpen(true);
                break;
            case Constant.INBOUND:
                LogUtil.log(TAG, "收到：服务端响应入库" + jsonString);
                ApronExecutionStatus.getInstance().setServerReplyDockIn(true);
                break;
            case Constant.TAKEOFF_TO_POINT:
                LogUtil.log(TAG, "收到：一键起飞" + jsonString);
                break;
            case Constant.FLY_TO_POINT:
                LogUtil.log(TAG, "收到：飞向目标点" + jsonString);
                break;
            case Constant.FLY_TO_POINT_STOP:
                LogUtil.log(TAG, "收到：结束 flyto 飞向目标点任务" + jsonString);

                break;
            case Constant.FLY_TO_POINT_STOP_UPDATE:
                LogUtil.log(TAG, "收到：更新 flyto 目标点" + jsonString);

                break;
            case Constant.FLIGHT_AUTHORITY_GRAB:
                LogUtil.log(TAG, "收到：飞行控制权抢夺" + jsonString);
                StickManager.getInstance().setVirtualStickModeEnabled(message);
                break;
            case Constant.PAYLOAD_AUTHORITY_GRAB:
                LogUtil.log(TAG, "收到：负载控制权抢夺" + jsonString);
                GimbalManager.getInstance().payloadAuthorityGrab(message);
                break;
            case Constant.DRC_MODE_ENTER:
                LogUtil.log(TAG, "收到：进入指令飞行控制模式" + jsonString);

                break;
            case Constant.DRC_MODE_EXIT:
                LogUtil.log(TAG, "收到：退出指令飞行控制模式" + jsonString);

                break;
            case Constant.DRONE_CONTROL:
                LogUtil.log(TAG, "收到：DRC-飞行控制" + jsonString);

                break;
            case Constant.DRONE_EMERGENCY_STOP:
                LogUtil.log(TAG, "收到：DRC-飞行器急停" + jsonString);

                break;
            case Constant.CAMERA_MODE_SWITCH:
                LogUtil.log(TAG, "收到：负载控制—切换相机模式" + jsonString);
                CameraManager.getInstance().setCameraMode(message);
                break;
            case Constant.CAMERA_PHOTO_TAKE:
                LogUtil.log(TAG, "收到：负载控制—开始拍照" + jsonString);
                CameraManager.getInstance().startShootPhoto(message);
                break;
            case Constant.CAMERA_PHOTO_STOP:
                LogUtil.log(TAG, "收到：负载控制—停止拍照" + jsonString);
                CameraManager.getInstance().stopShootPhoto(message);
                break;
            case Constant.CAMERA_RECORDING_START:
                LogUtil.log(TAG, "收到：负载控制—开始录像" + jsonString);
                CameraManager.getInstance().startRecordVideo(message);
                break;
            case Constant.CAMERA_RECORDING_STOP:
                LogUtil.log(TAG, "收到：负载控制—停止录像" + jsonString);
                CameraManager.getInstance().stopRecordVideo(message);
                break;
            case Constant.CAMERA_SCREEN_DRAG:
                LogUtil.log(TAG, "收到：负载控制—画面拖动控制" + jsonString);

                break;
            case Constant.CAMERA_AIM:
                LogUtil.log(TAG, "收到：负载控制—双击成为 AIM" + jsonString);
                break;
            case Constant.CAMERA_FOCAL_LENGTH_SET:
                LogUtil.log(TAG, "收到：负载控制—变焦" + jsonString);
                break;
            case Constant.GIMBAL_RESET:
                LogUtil.log(TAG, "收到：负载控制—重置云台" + jsonString);
                break;
            case Constant.CAMERA_LOOK_AT:
                LogUtil.log(TAG, "收到：负载控制—Look At" + jsonString);
                break;
            case Constant.CAMERA_SCREEN_SPLIT:
                LogUtil.log(TAG, "收到：负载控制—分屏" + jsonString);
                break;
            case Constant.PHOTO_STORAGE_SET:
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
                LogUtil.log(TAG, "收到：负载控制—照片存储设置" + jsonString);
                break;
            case Constant.VIDEO_STORAGE_SET:
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
                LogUtil.log(TAG, "收到：负载控制—视频存储设置" + jsonString);
                break;
            case Constant.CAMERA_EXPOSURE_MODE_SET:
                LogUtil.log(TAG, "收到：负载控制—相机曝光模式设置" + jsonString);
                break;
            case Constant.CAMERA_EXPOSURE_SET:
                LogUtil.log(TAG, "收到：负载控制—相机曝光值调节" + jsonString);
                break;
            case Constant.CAMERA_FOCUS_MODE_SET:
                LogUtil.log(TAG, "收到：负载控制—相机对焦模式" + jsonString);
                break;
            case Constant.CAMERA_FOCUS_MODE_VALUE_SET:
                LogUtil.log(TAG, "收到：负载控制—相机对焦值设置" + jsonString);
                break;
            case Constant.CAMERA_POINT_FOCUS_ACTION:
                LogUtil.log(TAG, "收到：负载控制—点对焦" + jsonString);
                break;
            case Constant.IR_METERING_MODE_SET:
                LogUtil.log(TAG, "收到：负载控制—红外测温模式设置" + jsonString);
                break;
            case Constant.IR_METERING_POINT_SET:
                LogUtil.log(TAG, "收到：负载控制—红外测温点设置" + jsonString);
                break;
            case Constant.IR_METERING_AREA_SET:
                LogUtil.log(TAG, "收到：负载控制—红外测温区域设置" + jsonString);
                break;
            case Constant.POI_MODE_ENTER:
                LogUtil.log(TAG, "收到：飞行控制—进入 POI 环绕模式" + jsonString);

                break;
            case Constant.POI_MODE_EXIT:
                LogUtil.log(TAG, "收到：飞行控制—退出 POI 环绕模式" + jsonString);

                break;
            case Constant.POI_CIRCLE_SPEED_SET:
                LogUtil.log(TAG, "收到：飞行控制—POI 环绕速度设置" + jsonString);

                break;
//            //获取控制权
//            case 60007:
//                LogUtil.log(TAG, "收到：获取控制权" + jsonString);
//                StickManager.getInstance().setVirtualStickModeEnabled(message);
//                break;
//            //虚拟摇杆数据
//            case 60008:
//                LogUtil.log(TAG, "收到：虚拟摇杆数据" + jsonString);
//                StickManager.getInstance().sendVirtualStickAdvancedParam(message);
//                break;
//            //云台角度控制
//            case 60009:
//                LogUtil.log(TAG, "收到：云台角度控制" + jsonString);
//                GimbalManager.getInstance().gimbalRotateByRelativeAngle(message);
//                break;
//            //收到此指令表示舱门已关闭，等待归中60012后可调用60011关机
//            case 60010:
////                LogUtil.log(TAG, "收到：舱门已关闭" + jsonString);
////                SystemManager.getInstance().droneShutdown(message);
//                break;
//
//            //收到60014表示Server已收到架次开始
//            case 60014:
//                LogUtil.log(TAG, "收到Server架次开始");
//                break;
//            //收到60015表示Server已收到架次结束
//            case 60015:
//                LogUtil.log(TAG, "收到Server架次结束");
//                break;
//            //取消控制权
//            case 60016:
//                LogUtil.log(TAG, "收到：取消控制权" + jsonString);
//                StickManager.getInstance().setVirtualStickModeDisable(message);
//                break;
//
//            //设置相机模式 0拍照 1录像
//            case 60018:
//                LogUtil.log(TAG, "收到：设置相机模式" + jsonString);
//                CameraManager.getInstance().setCameraMode(message);
//                break;
//            //开始拍照
//            case 60019:
//                LogUtil.log(TAG, "收到：开始拍照" + jsonString);
//                CameraManager.getInstance().startShootPhoto(message);
//                break;
//            //开始录像
//            case 60020:
//                LogUtil.log(TAG, "收到：开始录像" + jsonString);
//                CameraManager.getInstance().startRecordVideo(message);
//                break;
//            //停止录像
//            case 60021:
//                LogUtil.log(TAG, "收到：停止录像" + jsonString);
//                CameraManager.getInstance().stopRecordVideo(message);
//                break;
//            //设置变焦倍率
//            case 60022:
//                LogUtil.log(TAG, "收到：设置变焦倍率" + jsonString);
//                CameraManager.getInstance().setCameraZoomRatios(message);
//                break;
//            //设置红外变焦倍率
//            case 60023:
//                LogUtil.log(TAG, "收到：设置红外变焦倍率" + jsonString);
//                CameraManager.getInstance().setThermalZoomRatios(message);
//                break;
//            //开始推流，可配置推流视角
//            case 60100:
//                LogUtil.log(TAG, "收到：开始推流" + jsonString);
//                StreamManager.getInstance().startLive(message);
//                break;
//
//            //设置限高
//            case 60102:
//                LogUtil.log(TAG, "收到：设置限高" + jsonString);
//                FlightManager.getInstance().setHeightLimit(message);
//                break;
//            //设置限远
//            case 60103:
//                LogUtil.log(TAG, "收到：设置限远" + jsonString);
//                FlightManager.getInstance().setDistanceLimit(message);
//                break;
//            //设置限远是否启用
//            case 60104:
//                LogUtil.log(TAG, "收到：设置限远是否启用" + jsonString);
//                FlightManager.getInstance().setDistanceLimitEnabled(message);
//                break;
//            //设置红外分屏
//            case 60105:
//                LogUtil.log(TAG, "收到：设置红外分屏" + jsonString);
//                CameraManager.getInstance().setThermalDisplayMode(message);
//                break;
//            //取消返航
//            case 60106:
//                LogUtil.log(TAG, "收到：取消返航" + jsonString);
//                FlightManager.getInstance().stopGoHome(message);
//                break;
//            //终止航线
//            case 60109:
//                LogUtil.log(TAG, "收到：终止航线" + jsonString);
//                MissionManager.getInstance().stopMission(message);
//                break;
//            //开始喊话
//            case 60110:
//                LogUtil.log(TAG, "收到：开始喊话" + jsonString);
//                MegaphoneManager.getInstance().startMegaphonePlay(message);
//                break;
//            //结束喊话
//            case 60111:
//                LogUtil.log(TAG, "收到：结束喊话" + jsonString);
//                MegaphoneManager.getInstance().stopPlay(message);
//                break;
//            //一键飞往紧急备降点
//            case 60112:
//                LogUtil.log(TAG, "收到：备降点降落" + jsonString);
//                AlternateLandingManager.getInstance().startTaskProcess(message);
//                break;
//            //降落
//            case 60113:
//                LogUtil.log(TAG, "收到：降落" + jsonString);
//                FlightManager.getInstance().startAutoLanding(message);
//                break;
//            //取消降落
//            case 60114:
//                LogUtil.log(TAG, "收到：取消降落" + jsonString);
//                FlightManager.getInstance().stopAutoLanding(message);
//                break;
//            //解锁抛投器
//            case 60115:
//                LogUtil.log(TAG, "收到：解锁" + jsonString);
//                PayloadWidgetManager.getInstance().unlock(message);
//                break;
//            //锁定抛投器
//            case 60116:
//                LogUtil.log(TAG, "收到：锁定" + jsonString);
//                PayloadWidgetManager.getInstance().lock(message);
//                break;
//            //抛投
//            case 60117:
//                LogUtil.log(TAG, "收到：抛投" + jsonString);
//                PayloadWidgetManager.getInstance().throwOne(message);
//                break;
//            //一键全抛
//            case 60118:
//                LogUtil.log(TAG, "收到：一键全投" + jsonString);
//                PayloadWidgetManager.getInstance().throwAll(message);
//                break;
//            //设置备降点
////            case 60119:
////                LogUtil.log(TAG, "收到：设置备降点" + jsonString);
////                AlternateLandingManager.getInstance().setAlternatePoint(message);
////                break;
//            //开始定时拍照
//            case 60120:
//                LogUtil.log(TAG, "收到：开始定时拍照" + jsonString);
//                CameraManager.getInstance().startTakePhotoWithInterval(message);
//                break;
//            //结束拍照
//            case 60121:
//                LogUtil.log(TAG, "收到：结束拍照" + jsonString);
//                CameraManager.getInstance().stopShootPhoto(message);
//                break;
//            //重置相机
//            case 60122:
//                LogUtil.log(TAG, "收到：重置相机设置" + jsonString);
//                CameraManager.getInstance().resetCameraSetting(message);
//                break;
//            //云台回中
//            case 60123:
//                LogUtil.log(TAG, "收到：云台回中" + jsonString);
//                GimbalManager.getInstance().gimbalResetWithPitchAndYaw(message);
//                break;
//            //设置对焦模式
//            case 60124:
//                LogUtil.log(TAG, "收到：设置对焦模式" + jsonString);
//                CameraManager.getInstance().setCameraFocusMode(message);
//                break;
//            //指点对焦
//            case 60125:
//                LogUtil.log(TAG, "收到：指点对焦" + jsonString);
//                CameraManager.getInstance().tapZoomAtTarget(message);
//                break;
//            //设置照片xmp写入
//            case 60126:
//                LogUtil.log(TAG, "收到：写入exif" + jsonString);
//                MediaManager.getInstance().setMediaFileXMPCustomInfo(message);
//                break;
//            //设置曝光模式
//            case 60127:
//                LogUtil.log(TAG, "收到：设置曝光模式" + jsonString);
//                CameraManager.getInstance().setExposureMode(message);
//                break;
//            //设置ev
//            case 60128:
//                LogUtil.log(TAG, "收到：设置曝光补偿" + jsonString);
//                CameraManager.getInstance().setExposureCompensation(message);
//                break;
//            //发送数据到psdk
//            case 60129:
//                LogUtil.log(TAG, "收到：发送数据到psdk" + jsonString);
//                PayloadWidgetManager.getInstance().sendMsgToPayload(message);
//                break;
//            //停止推流
//            case 60130:
//                LogUtil.log(TAG, "收到：停止推流" + jsonString);
//                StreamManager.getInstance().stopLive(message);
//                break;
//            //设置对焦值
//            case 60131:
//                LogUtil.log(TAG, "收到：设置对焦值" + jsonString);
//                CameraManager.getInstance().setCameraFocusRingValue(message);
//                break;
//            //获取里程
//            case 60132:
//                LogUtil.log(TAG, "收到：获取里程" + jsonString);
//                FlightManager.getInstance().getAircraftTotalFlightDistance(message);
//                break;
//            //上传媒体文件
//            case 60134:
////                LogUtil.log(TAG, "收到：获取里程" + jsonString);
////                FlightManager.getInstance().getAircraftTotalFlightDistance(message);
//                break;
//            //异地降落
//            case 60135:
//                LogUtil.log(TAG, "收到：异地降落" + jsonString);
//                OffSiteLandingManager.getInstance().startTaskProcess(message);
//                break;
//            //刷新返航点
//            case 60136:
//                LogUtil.log(TAG, "收到：重置返航点" + jsonString);
//                ResetHomePointManager.getInstance().startTaskProcess(message);
//                break;
//            //设置纯净模式
//            case 60137:
//                LogUtil.log(TAG, "收到：设置纯净模式" + jsonString);
//                PreferenceUtils.getInstance().setIsCleanMode(message.getIsCleanMode()==1?true:false);
//                EventBus.getDefault().post(FLAG_RESET_CLEAN_MODE);
//                break;
//            //设置测温模式
//            case 60138:
//                LogUtil.log(TAG, "收到：设置测温模式" + jsonString);
//                CameraManager.getInstance().setThermalTemperatureMeasureMode(message);
//                break;
//            //设置测温点
//            case 60139:
//                LogUtil.log(TAG, "收到：设置测温点" + jsonString);
//                CameraManager.getInstance().setThermalSpotMetersurePoint(message);
//                break;
//            //设置测温区域
//            case 60140:
//                LogUtil.log(TAG, "收到：设置测温区域" + jsonString);
//                CameraManager.getInstance().setThermalRegionMetersureArea(message);
//                break;
//            //切换直播视角
//            case 60141:
//                LogUtil.log(TAG, "收到：切换直播视角" + jsonString);
//                StreamManager.getInstance().switchCurrentView(message);
//                break;
//            //设置云台控制的最大速度
//            case 60142:
//                LogUtil.log(TAG, "收到：设置云台控制的最大速度" + jsonString);
//                GimbalManager.getInstance().setGimbalControlMaxSpeed(message);
//                break;
//            //紧急悬停
//            case 60143:
//                LogUtil.log(TAG, "收到：设置紧急悬停" + jsonString);
//                FlightManager.getInstance().emergencyHover(message);
//                break;
//            //设置失控动作
//            case 60144:
//                LogUtil.log(TAG, "收到：设置失控动作" + jsonString);
//                FlightManager.getInstance().setFailsafeAction(message);
//                break;
//            //设置返航高度
//            case 60145:
//                LogUtil.log(TAG, "收到：设置返航高度" + jsonString);
//                FlightManager.getInstance().setGoHomeHeight(message);
//                break;
//            //设置低电量报警阈值
//            case 60146:
//                LogUtil.log(TAG, "收到：设置低电量报警阈值" + jsonString);
//                FlightManager.getInstance().setLowBatteryWarningThreshold(message);
//                break;
//            //设置严重低电量报警阈值
//            case 60147:
//                LogUtil.log(TAG, "收到：设置严重低电量报警阈值" + jsonString);
//                FlightManager.getInstance().setSeriousLowBatteryWarningThreshold(message);
//                break;
//            //设置智能低电量返航
//            case 60148:
//                LogUtil.log(TAG, "收到：设置智能低电量返航" + jsonString);
//                FlightManager.getInstance().setLowBatteryRTHEnabled(message);
//                break;
//            //指点对焦
//            case 60149:
//                LogUtil.log(TAG, "收到：指点对焦" + jsonString);
//                break;
//            //云台偏航回中
//            case 60150:
//                LogUtil.log(TAG, "收到：云台偏航回中" + jsonString);
//                GimbalManager.getInstance().gimbalResetWithYaw(message);
//                break;
//            //云台偏航向下
//            case 60151:
//                LogUtil.log(TAG, "收到：云台偏航向下" + jsonString);
//                GimbalManager.getInstance().gimbalDownWithPitch(message);
//                break;
//            //云台向下
//            case 60152:
//                LogUtil.log(TAG, "收到：云台向下" + jsonString);
//                GimbalManager.getInstance().gimbalDownWithPitchAndYaw(message);
//                break;
//            //msdk日志上传
//            case 60666:
//                LogUtil.log(TAG, "收到：日志上传" + jsonString);
//                AMSLogManager.getInstance().enableLogList(message);
//                break;
//            //监听机库收到AMS命令后的回执
//            case 60999:
//                if (!TextUtils.isEmpty(message.getStatus())) {
//                    switch (message.getStatus()) {
//                        case "0":
//                            LogUtil.log(TAG, "收到：服务端响应关舱门" + jsonString);
//                            break;
//                        case "1":
//                            ApronExecutionStatus.getInstance().setServerReplyDockOpen(true);
//                            LogUtil.log(TAG, "收到：服务端响应开舱门" + jsonString);
//                            break;
//                        case "2":
//                            ApronExecutionStatus.getInstance().setServerReplyDockIn(true);
//                            LogUtil.log(TAG, "收到：服务端响应入库" + jsonString);
//                            break;
//                        case "3":
//                            ApronExecutionStatus.getInstance().setServerReplyDroneShut(true);
//                            LogUtil.log(TAG, "收到：服务端响应关机" + jsonString);
//                            break;

//                } else {
//                    LogUtil.log(TAG, "收到：机库动作参数有误" + jsonString);
//                }
//                break;

        }
    }

    public static final String FLAG_RESET_CLEAN_MODE = "FLAG_RESET_CLEAN_MODE";


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            if (reconnect) {//重新订阅
                LogUtil.log(TAG, "MQtt ConnectComplete:" + serverURI);
                MqttManager.getInstance().mqttAndroidClient.subscribe(AMSConfig.DOWN_UAV_SERVICES, 1);//订阅主题:注册
                MqttManager.getInstance().mqttAndroidClient.subscribe(AMSConfig.DOWN_UAV_EVENT, 1);//订阅主题:注册
                // publish(topic,"注册",0);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "MQtt ConnectException:" + e.toString());
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkVtxRunnable;

    private void checkVtxWithDelay(Runnable onVtxReady) {

        if (Movement.getInstance().isVtx()) {
            // 图传正常，直接继续后面的流程
            PreferenceUtils.getInstance().setRestartAMSTimes(0);
            LogUtil.log(TAG, "图传正常，继续执行任务流程");
            onVtxReady.run();
            return;
        }

        LogUtil.log(TAG, "未检测到图传，5 秒后再次确认...");

        handler.postDelayed(() -> {
            if (!Movement.getInstance().isVtx()) {
                // 图传仍未恢复 → 执行重启逻辑
                int times = PreferenceUtils.getInstance().getRestartAMSTimes();
                if (times < 5) {
                    PreferenceUtils.getInstance().setRestartAMSTimes(times + 1);
                    LogUtil.log(TAG, "图传仍未恢复，重启 AMS 第 " + (times + 1) + " 次");
                    RestartAPPTool.INSTANCE.restartApp(ApronApp.Companion.getContext());
                }
            } else {
                // 图传恢复 → 执行后续逻辑
                PreferenceUtils.getInstance().setRestartAMSTimes(0);
                LogUtil.log(TAG, "图传在延迟期间恢复，继续执行任务流程");
                onVtxReady.run();
            }
        }, 5000);
    }

}
