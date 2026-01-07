package com.aros.apron.manager;


import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aros.apron.activity.MainActivity;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
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


    public void checkRemoteControlPowerStatus(MessageDown message) {
            sendMsg2Server(message);
    }

    public void checkAircraftPowerStatus(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            sendMsg2Server( message);
        } else {
            sendFailMsg2Server( message, "无人机未连接");
        }
    }

    //文件是否上传结束
    public void aircraftStoredReply(MessageDown message) {
        if (ApronExecutionStatus.getInstance().isAircraftWaitShutDown()) {
            sendMsg2Server( message);
        } else {
            sendFailMsg2Server( message, "不可关机");
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
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            Movement.getInstance().setTaskFail(true);
        }
    }
}
