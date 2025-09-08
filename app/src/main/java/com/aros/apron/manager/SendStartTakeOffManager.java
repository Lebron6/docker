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

public class SendStartTakeOffManager extends BaseManager {

    private final int maxRetries = 5;
    private int sendStartTakeOffSuccessTimes;
    private boolean isSendStartTakeOffSuccess;

    private SendStartTakeOffManager() {
    }

    private static class DockCloseHolder {
        private static final SendStartTakeOffManager INSTANCE = new SendStartTakeOffManager();
    }

    public static SendStartTakeOffManager getInstance() {
        return DockCloseHolder.INSTANCE;
    }


    public void sendStartTakeOff2Server() {
//        if (isSendStartTakeOffSuccess||sendStartTakeOffSuccessTimes >= maxRetries) {
        if (sendStartTakeOffSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开始起飞"+isSendStartTakeOffSuccess+sendStartTakeOffSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendStartTakeOffMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开始起飞发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendStartTakeOffMessage(){
        MessageReply message = new MessageReply();
        message.setMsg_type(60031);
        message.setResult(1);
        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getTaskId())){
            message.setTask_id(PreferenceUtils.getInstance().getTaskId());
        }
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        LogUtil.log(TAG,"sendStartTakeOffMessage:"+new Gson().toJson(message));

        mqttMessage.setQos(0);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "开始起飞发送成功：60031---"+sendStartTakeOffSuccessTimes+"clientId:"+MqttManager.getInstance().mqttAndroidClient.getClientId());
                    sendMissionExecuteEvents( "AMS通知服务器开始起飞");
                    isSendStartTakeOffSuccess = true;
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "通知服务器开始起飞发送回调失败：" + exception.toString());
                    retrySend(MqttManager.getInstance().mqttAndroidClient);
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "通知服务器开始起飞发送异常：" + e.toString());
            e.printStackTrace();
        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client) {
        sendStartTakeOffSuccessTimes++;
        if (sendStartTakeOffSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendStartTakeOff2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，开始起飞发送失败：" + sendStartTakeOffSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendStartTakeOffSuccess && sendStartTakeOffSuccessTimes < maxRetries) {
            sendStartTakeOffSuccessTimes++;
            mainHandler.postDelayed(() -> sendStartTakeOff2Server(), 2000);
            LogUtil.log(TAG, "开始起飞发送失败：mqtt未连接" + "--" + sendStartTakeOffSuccessTimes);
        } else {
            LogUtil.log(TAG, "开始起飞发送失败：" + sendStartTakeOffSuccessTimes);
        }
    }

}