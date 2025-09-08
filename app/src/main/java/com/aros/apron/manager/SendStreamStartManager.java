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

public class SendStreamStartManager extends BaseManager {

    private final int maxRetries = 2;
    private int sendStreamStartSuccessTimes;
    private boolean isSendStreamStartSuccess;

    private SendStreamStartManager() {
    }

    private static class DockCloseHolder {
        private static final SendStreamStartManager INSTANCE = new SendStreamStartManager();
    }

    public static SendStreamStartManager getInstance() {
        return DockCloseHolder.INSTANCE;
    }


    public void sendStreamStartMsg2Server() {
//        if (isSendStreamStartSuccess||sendStreamStartSuccessTimes >= maxRetries) {
        if (sendStreamStartSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开始推流"+isSendStreamStartSuccess+sendStreamStartSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendStreamStartMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开始推流发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendStreamStartMessage(){
        MessageReply message = new MessageReply();
        message.setMsg_type(60030);
        message.setResult(1);
        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getTaskId())){
            message.setTask_id(PreferenceUtils.getInstance().getTaskId());
        }
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        LogUtil.log(TAG,"sendStreamStartMessage:"+new Gson().toJson(message));

        mqttMessage.setQos(0);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "开始推流发送成功：60030---"+sendStreamStartSuccessTimes+"clientId:"+MqttManager.getInstance().mqttAndroidClient.getClientId());
                    sendMissionExecuteEvents( "AMS通知服务器开始推流");
                    isSendStreamStartSuccess = true;
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "通知服务器开始推流发送回调失败：" + exception.toString());
                    retrySend();
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "通知服务器开始推流发送异常：" + e.toString());
            e.printStackTrace();
        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend() {
        sendStreamStartSuccessTimes++;
        if (sendStreamStartSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendStreamStartMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，开始推流发送失败：" + sendStreamStartSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendStreamStartSuccess && sendStreamStartSuccessTimes < maxRetries) {
            sendStreamStartSuccessTimes++;
            mainHandler.postDelayed(() -> sendStreamStartMsg2Server(), 2000);
            LogUtil.log(TAG, "开始推流发送失败：mqtt未连接" + "--" + sendStreamStartSuccessTimes);
        } else {
            LogUtil.log(TAG, "开始推流发送失败：" + sendStreamStartSuccessTimes);
        }
    }

}