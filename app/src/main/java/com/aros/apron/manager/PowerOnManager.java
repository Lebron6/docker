package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;
import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.MessageEvent;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PowerOnManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendPowerOnSuccessTimes;
    private boolean isSendPowerOnSuccess;

    private PowerOnManager() {
    }

    private static class PowerOnHolder {
        private static final PowerOnManager INSTANCE = new PowerOnManager();
    }

    public static PowerOnManager getInstance() {
        return PowerOnHolder.INSTANCE;
    }


    public void sendPowerOnMsg2Server(String sn) {
        if (sendPowerOnSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开机"+isSendPowerOnSuccess+sendPowerOnSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendPowerOnMessage(sn);
            } else {
                handleNotConnected(sn);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开机发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendPowerOnMessage(String sn){
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setBid(UUID.randomUUID().toString());
        messageEvent.setTid(UUID.randomUUID().toString());
        messageEvent.setTimestamp(System.currentTimeMillis());
        messageEvent.setMethod(Constant.AIRCRAFT_ON);
        MessageEvent.Data data=new MessageEvent.Data();
        data.setMsg("飞行器已连接");
        data.setLevel(1);
        data.setSn(sn);
        messageEvent.setData(data);
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).
                getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_SERVICES_REPLY,
                    mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
	                    LogUtil.log(TAG, "开机发送成功："+new Gson().toJson(messageEvent));
                    sendEvent2Server("AMS通知开机",1);
                    isSendPowerOnSuccess = true;
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "开机发送回调失败：" + exception.toString());
                    retrySend(sn);
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "开机发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(String sn) {
        sendPowerOnSuccessTimes++;
        if (sendPowerOnSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendPowerOnMsg2Server(sn), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数， 开机发送失败：" + sendPowerOnSuccessTimes);
        }
    }

    private void handleNotConnected(String sn) {
        if (!isSendPowerOnSuccess && sendPowerOnSuccessTimes < maxRetries) {
            sendPowerOnSuccessTimes++;
            mainHandler.postDelayed(() -> sendPowerOnMsg2Server(sn), 2000);
	            LogUtil.log(TAG, " 开机发送失败：mqtt未连接"  + sendPowerOnSuccessTimes);
        } else {
            LogUtil.log(TAG, " 开机发送失败：" + sendPowerOnSuccessTimes);
        }
    }

}