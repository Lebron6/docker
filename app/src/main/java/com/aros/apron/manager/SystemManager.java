package com.aros.apron.manager;


import android.text.TextUtils;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import org.eclipse.paho.android.service.MqttAndroidClient;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.v5.manager.KeyManager;


public class SystemManager extends BaseManager {

    public boolean mediaFilePushOver = false;
    public boolean itCentered = false;


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
        setItCentered(true);
        if (isMediaFilePushOver()) {
            DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient);
        }
    }

    public void upLoadMedia(MqttAndroidClient mqttAndroidClient) {

        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getUploadUrl())
                && !TextUtils.isEmpty(PreferenceUtils.getInstance().getAccessKey())
                && !TextUtils.isEmpty(PreferenceUtils.getInstance().getSecretKey())) {
            MediaManager.INSTANCE.enablePlayback(mqttAndroidClient);
        } else {
            LogUtil.log(TAG, "minio上传参数有误,直接入库");
            setMediaFilePushOver(true);
            if (isItCentered()) {
                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient);
            }
        }

    }

    public boolean isMediaFilePushOver() {
        return mediaFilePushOver;
    }

    public void setMediaFilePushOver(boolean mediaFilePushOver) {
        this.mediaFilePushOver = mediaFilePushOver;
    }

    public boolean isItCentered() {
        return itCentered;
    }

    public void setItCentered(boolean itCentered) {
        this.itCentered = itCentered;
    }
}
