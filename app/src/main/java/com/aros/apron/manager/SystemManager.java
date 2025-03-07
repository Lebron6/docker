package com.aros.apron.manager;


import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


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

    //收到60012表示飞机已归中,立即回复60012
    public void aircraftStoredReply(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        sendMsg2Server(mqttAndroidClient, message);
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
                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient);
        }

    }

}
