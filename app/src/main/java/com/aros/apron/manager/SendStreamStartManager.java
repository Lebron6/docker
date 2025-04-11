package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
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


    public void sendStreamStartMsg2Server(MqttAndroidClient client) {
//        if (isSendStreamStartSuccess||sendStreamStartSuccessTimes >= maxRetries) {
        if (sendStreamStartSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开始推流"+isSendStreamStartSuccess+sendStreamStartSuccessTimes);
            return;
        }
        try {
            if (client.isConnected()) {
                sendStreamStartMessage(client);
            } else {
                handleNotConnected(client);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开始推流发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendStreamStartMessage(MqttAndroidClient client){
        MessageReply message = new MessageReply();
        message.setMsg_type(60030);
        message.setResult(1);
        if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getTaskId())){
            message.setTask_id(PreferenceUtils.getInstance().getTaskId());
        }
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(0);
        try {
            client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "开始推流发送成功：60030---"+sendStreamStartSuccessTimes+"clientId:"+client.getClientId());
                    sendMissionExecuteEvents(client, "AMS通知服务器开始推流");
                    isSendStreamStartSuccess = true;
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "通知服务器开始推流发送回调失败：" + exception.toString());
                    retrySend(client);
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "通知服务器开始推流发送异常：" + e.toString());
            e.printStackTrace();
        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client) {
        sendStreamStartSuccessTimes++;
        if (sendStreamStartSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendStreamStartMsg2Server(client), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，开始推流发送失败：" + sendStreamStartSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client) {
        if (!isSendStreamStartSuccess && sendStreamStartSuccessTimes < maxRetries) {
            sendStreamStartSuccessTimes++;
            mainHandler.postDelayed(() -> sendStreamStartMsg2Server(client), 2000);
            LogUtil.log(TAG, "开始推流发送失败：mqtt未连接" + "--" + sendStreamStartSuccessTimes);
        } else {
            LogUtil.log(TAG, "开始推流发送失败：" + sendStreamStartSuccessTimes);
        }
    }

}