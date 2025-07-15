package com.aros.apron.manager;


import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.manager.KeyManager;


public class SystemManager extends BaseManager {


    private SystemManager() {
    }

    private static class AirLinkHolder {
        private static final SystemManager INSTANCE = new SystemManager();
    }

    public static SystemManager getInstance() {
        return AirLinkHolder.INSTANCE;
    }


    public void checkRemoteControlPowerStatus(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
            sendMsg2Server(mqttAndroidClient, message);
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "遥控器未连接");
//        }

    }

    public void checkAircraftPowerStatus(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            sendMsg2Server(mqttAndroidClient, message);
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //飞机已经执行过航线，落地后没有关遥控器，或重启AMS
    public void replyAlreadyFlown(MqttAndroidClient mqttAndroidClient, MQMessage message) {
            sendMsg2Server(mqttAndroidClient, message, "请等待或手动重启遥控器或AMS软件");
            LogUtil.log(TAG,"请等待或手动重启遥控器或AMS软件");
    }

    //检查航线下发时参数是否缺少
    public boolean checkMissionParameter(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getAlternate_lat()) && !TextUtils.isEmpty(message.getAlternate_lng()) && !TextUtils.isEmpty(message.getSafe_land_height())&& !TextUtils.isEmpty(message.getTask_id())) {
            PreferenceUtils.getInstance().setAlternatePointLat(message.getAlternate_lat());
            PreferenceUtils.getInstance().setAlternatePointLon(message.getAlternate_lng());
            PreferenceUtils.getInstance().setAlternatePointSecurityHeight(message.getSafe_land_height());
            PreferenceUtils.getInstance().setTaskId(message.getTask_id());
            Movement.getInstance().setAlternatePointLon(PreferenceUtils.getInstance().getAlternatePointLon());
            Movement.getInstance().setAlternatePointLat(PreferenceUtils.getInstance().getAlternatePointLat());
            return true;
        } else {
            LogUtil.log(TAG, "航线参数有误,直接入库："+new Gson().toJson(message));
            DroneStorageManager.getInstance().sendDroneStorageMsg2Server(mqttAndroidClient, -1);
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            Movement.getInstance().setTaskFail(true);
            sendMsg2Server(mqttAndroidClient, message, "航线参数有误");
            return false;
        }
    }

    //收到60012表示飞机已归中,立即回复60012
    //收到60012表示服务端在确认飞机此时时候处于可关机的状态
    public void aircraftStoredReply(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        if (ApronExecutionStatus.getInstance().isAircraftWaitShutDown()) {
            sendMsg2Server(mqttAndroidClient, message);
        } else {
            sendMsg2Server(mqttAndroidClient, message, "不可关机");
        }
    }

    public void upLoadMedia(MqttAndroidClient mqttAndroidClient) {

        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getUploadUrl())
                && !TextUtils.isEmpty(PreferenceUtils.getInstance().getAccessKey())
                && !TextUtils.isEmpty(PreferenceUtils.getInstance().getSecretKey())) {
            Handler handler=new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MediaManager.getInstance().enablePlayback();
                }
            },1000);
        } else {
            LogUtil.log(TAG, "minio上传参数有误,直接入库");
            DroneStorageManager.getInstance().sendDroneStorageMsg2Server(mqttAndroidClient, -1);
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            Movement.getInstance().setTaskFail(true);

        }

    }

}
