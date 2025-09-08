package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class SendLandingManager extends BaseManager {

    private final int maxRetries = 5;
    private int sendLandingSuccessTimes;
    private boolean isSendLandingSuccess;

    private SendLandingManager() {
    }

    private static class DockCloseHolder {
        private static final SendLandingManager INSTANCE = new SendLandingManager();
    }

    public static SendLandingManager getInstance() {
        return DockCloseHolder.INSTANCE;
    }


    public void sendLandingMsg2Server() {
//        if (isSendLandingSuccess||sendLandingSuccessTimes >= maxRetries) {
        if (sendLandingSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送已降落"+isSendLandingSuccess+sendLandingSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendLandingMessage();
            } else {
                handleNotConnected(MqttManager.getInstance().mqttAndroidClient);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "已降落发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendLandingMessage(){
        MessageReply message = new MessageReply();
        message.setMsg_type(60032);
        message.setResult(1);

        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getTaskId())){
            message.setTask_id(PreferenceUtils.getInstance().getTaskId());
        }
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        LogUtil.log(TAG,"sendLandingMessage:"+new Gson().toJson(message));

        mqttMessage.setQos(0);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "已降落发送成功：60032---"+sendLandingSuccessTimes+"clientId:"+MqttManager.getInstance().mqttAndroidClient.getClientId()+"task_id"+PreferenceUtils.getInstance().getTaskId());
                    sendMissionExecuteEvents( "AMS通知服务器已降落");
                    isSendLandingSuccess = true;

                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "通知服务器已降落发送回调失败：" + exception.toString());
                    retrySend();
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "通知服务器已降落发送异常：" + e.toString());
            e.printStackTrace();
        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend() {
        sendLandingSuccessTimes++;
        if (sendLandingSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendLandingMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，已降落发送失败：" + sendLandingSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client) {
        if (!isSendLandingSuccess && sendLandingSuccessTimes < maxRetries) {
            sendLandingSuccessTimes++;
            mainHandler.postDelayed(() -> sendLandingMsg2Server(), 2000);
            LogUtil.log(TAG, "已降落发送失败：mqtt未连接" + "--" + sendLandingSuccessTimes);
        } else {
            LogUtil.log(TAG, "已降落发送失败：" + sendLandingSuccessTimes);
        }
    }

}