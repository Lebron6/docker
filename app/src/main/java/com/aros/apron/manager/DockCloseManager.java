package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.MessageEvent;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DockCloseManager extends BaseManager {

    private final int maxRetries = 2;
    private int sendDockCloseSuccessTimes;
    private boolean isSendDockCloseSuccess;

    private DockCloseManager() {
    }

    private static class DockCloseHolder {
        private static final DockCloseManager INSTANCE = new DockCloseManager();
    }

    public static DockCloseManager getInstance() {
        return DockCloseHolder.INSTANCE;
    }


    public void sendDockCloseMsg2Server() {
        if (sendDockCloseSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送关舱"+isSendDockCloseSuccess+sendDockCloseSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendDockCloseMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关舱异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDockCloseMessage(){
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setBid(UUID.randomUUID().toString());
        messageEvent.setTid(UUID.randomUUID().toString());
        messageEvent.setTimestamp(System.currentTimeMillis());
        messageEvent.setMethod(Constant.CLOSE_DOOR);
        MessageEvent.Data data=new MessageEvent.Data();
        data.setResult(1);
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
	                    LogUtil.log(TAG, "关舱发送成功："+sendDockCloseSuccessTimes+"clientId:"+MqttManager.getInstance().mqttAndroidClient.getClientId());
                    sendEvent2Server("AMS通知机库关舱");
                    isSendDockCloseSuccess = true;
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "关舱发送回调失败：" + exception.toString());
                    retrySend();
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "关舱发送异常：" + e.toString());
            e.printStackTrace();
        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend() {
        sendDockCloseSuccessTimes++;
        if (sendDockCloseSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDockCloseMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，关舱发送失败：" + sendDockCloseSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendDockCloseSuccess && sendDockCloseSuccessTimes < maxRetries) {
            sendDockCloseSuccessTimes++;
            mainHandler.postDelayed(() -> sendDockCloseMsg2Server(), 2000);
	            LogUtil.log(TAG, "关舱发送失败：mqtt未连接"  + sendDockCloseSuccessTimes);
        } else {
            LogUtil.log(TAG, "关舱发送失败：" + sendDockCloseSuccessTimes);
        }
    }

}