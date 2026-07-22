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
import com.aros.apron.manager.FlyToPointManager;
import com.aros.apron.manager.GimbalManager;
import com.aros.apron.manager.MissionV3Manager;
import com.aros.apron.manager.SpeakerManager;
import com.aros.apron.manager.StickManager;
import com.aros.apron.manager.StreamManager;
import com.aros.apron.manager.SystemManager;
import com.aros.apron.manager.TakeOffToPointManager;
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
        String jsonString = null;
//        Log.e(TAG, "入口打印:" + mqttMessage.toString());
        try {
            jsonString = new String(mqttMessage.getPayload(), "UTF-8");
        } catch (Exception e) {
            LogUtil.log(TAG, "解析异常");
            throw new RuntimeException(e);
        }
        MessageDown message = new Gson().fromJson(jsonString, MessageDown.class);
        switch (message.getMethod()) {
            case Constant.PILOT_ON:
//                LogUtil.log(TAG, "收到：遥控器是否开机" + jsonString);
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
                break;
            case Constant.AIRCRAFT_ON:
//                LogUtil.log(TAG, "收到：飞机是否开机" + jsonString);
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
                StreamManager.getInstance().startLiveWithRtmp(message);
                break;
            case Constant.LIVE_STOP_PUSH:
                LogUtil.log(TAG, "收到：停止直播" + jsonString);
                StreamManager.getInstance().stopLive(message);
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
            case Constant.TASK_FAIL:
                LogUtil.log(TAG, "收到：服务端响应TaskFail" + jsonString);
                ApronExecutionStatus.getInstance().setServerReplyTaskFail(true);
                break;
            case Constant.INBOUND:
                LogUtil.log(TAG, "收到：服务端响应入库" + jsonString);
                ApronExecutionStatus.getInstance().setServerReplyDockIn(true);
                break;
            case Constant.TAKEOFF_TO_POINT:
                LogUtil.log(TAG, "收到：一键起飞" + jsonString);
                TakeOffToPointManager.getInstance().taskExecute(message);
                break;
            case Constant.FLY_TO_POINT:
                LogUtil.log(TAG, "收到：飞向目标点" + jsonString);
                FlyToPointManager.getInstance().taskExecute(message);
                break;
            case Constant.FLY_TO_POINT_STOP:
                LogUtil.log(TAG, "收到：结束 flyto 飞向目标点任务" + jsonString);
                FlyToPointManager.getInstance().stopMission(message);
                break;
            case Constant.FLY_TO_POINT_STOP_UPDATE:
                LogUtil.log(TAG, "收到：更新 flyto 目标点" + jsonString);
                FlyToPointManager.getInstance().updateTarget(message);
                break;
            case Constant.FLIGHT_AUTHORITY_GRAB:
                LogUtil.log(TAG, "收到：飞行控制权抢夺" + jsonString);
                StickManager.getInstance().enableVirtualStick(message);
                break;
            case Constant.PAYLOAD_AUTHORITY_GRAB:
                LogUtil.log(TAG, "收到：负载控制权抢夺" + jsonString);
                GimbalManager.getInstance().payloadAuthorityGrab(message);
                break;
            case Constant.DRC_MODE_ENTER:
                LogUtil.log(TAG, "收到：进入指令飞行控制模式" + jsonString);
                StickManager.getInstance().setVirtualStickModeEnabled(message);
                break;
                //退出控制权时，要自动触发续飞航线
            case Constant.DRC_MODE_EXIT:
                LogUtil.log(TAG, "收到：退出指令飞行控制模式" + jsonString);
                StickManager.getInstance().disableVirtualStick(message);
                break;
            case Constant.DRONE_CONTROL:
                LogUtil.log(TAG, "收到：DRC-飞行控制" + jsonString);
                StickManager.getInstance().sendVirtualStickAdvancedParam(message);
                break;
            case Constant.DRONE_EMERGENCY_STOP:
                LogUtil.log(TAG, "收到：DRC-飞行器急停" + jsonString);
                FlightManager.getInstance().emergencyHover(message);
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
                GimbalManager.getInstance().cameraScreenDrag(message);
                break;
            case Constant.CAMERA_AIM:
                LogUtil.log(TAG, "收到：负载控制—双击成为 AIM" + jsonString);
                CameraManager.getInstance().tapZoomAtTarget(message);
                break;
            case Constant.CAMERA_FOCAL_LENGTH_SET:
                LogUtil.log(TAG, "收到：负载控制—变焦" + jsonString);
                CameraManager.getInstance().setCameraZoomRatios(message);
                break;
            case Constant.GIMBAL_RESET:
                LogUtil.log(TAG, "收到：负载控制—重置云台" + jsonString);
                GimbalManager.getInstance().gimbalReset(message);
                break;
            case Constant.CAMERA_LOOK_AT:
                LogUtil.log(TAG, "收到：负载控制—Look At" + jsonString);
                GimbalManager.getInstance().gimbalLookAt(message);
                break;
            case Constant.CAMERA_SCREEN_SPLIT:
                LogUtil.log(TAG, "收到：负载控制—分屏" + jsonString);
                break;
            case Constant.PHOTO_STORAGE_SET:
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
//                LogUtil.log(TAG, "收到：负载控制—照片存储设置" + jsonString);
                break;
            case Constant.VIDEO_STORAGE_SET:
                SystemManager.getInstance().checkRemoteControlPowerStatus(message);
//                LogUtil.log(TAG, "收到：负载控制—视频存储设置" + jsonString);
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
            case Constant.SPEAKER_AUDIO_PLAY_START:
                LogUtil.log(TAG, "收到：喊话器-开始播放音频" + jsonString);
                SpeakerManager.getInstance().speakerAudioPlayStart(message);
                break;
            case Constant.SPEAKER_TTS_PLAY_START:
                LogUtil.log(TAG, "收到：喊话器-开始播放TTS文本" + jsonString);
                SpeakerManager.getInstance().speakerTTSPlayStart(message,0);
                break;
            case Constant.SPEAKER_REPLAY:
                LogUtil.log(TAG, "收到：喊话器-重新播放" + jsonString);
                SpeakerManager.getInstance().speakerReply(message);
                break;
            case Constant.SPEAKER_PLAY_STOP:
                LogUtil.log(TAG, "收到：喊话器-停止播放" + jsonString);
                SpeakerManager.getInstance().speakerStop(message);
                break;
            case Constant.SPEAKER_PLAY_MODE_SET:
                LogUtil.log(TAG, "收到：喊话器-设置播放模式" + jsonString);
                SpeakerManager.getInstance().speakerPlayModeSet(message);
                break;
            case Constant.SPEAKER_PLAY_VOLUME_SET:
                LogUtil.log(TAG, "收到：喊话器-设置音量" + jsonString);
                SpeakerManager.getInstance().speakerPlayVolumeSet(message);
                break;
                case Constant.DRC_SPEAKER_TTS_SET:
                LogUtil.log(TAG, "收到：喊话器-TTS喊话设置" + jsonString);
                SpeakerManager.getInstance().speakerTTSPlayStart(message,1);
                break;

        }
    }

    public static final String FLAG_RESET_CLEAN_MODE = "FLAG_RESET_CLEAN_MODE";


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
    String[] topics = new String[] {
            AMSConfig.getInstance().DOWN_UAV_EVENT_REPLY,
            AMSConfig.getInstance().DOWN_UAV_SERVICES,
    };
    int[] qos = new int[] {
            1, 1
    };
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
//            if (reconnect) {//重新订阅
                LogUtil.log(TAG, "MQtt ConnectComplete:" + serverURI);
                MqttManager.getInstance().mqttAndroidClient.subscribe(topics, qos);//订阅主题:注册
//                MqttManager.getInstance().mqttAndroidClient.subscribe(AMSConfig.DOWN_UAV_EVENT_REPLY, 1);//订阅主题:注册
                // publish(topic,"注册",0);
//            }
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
