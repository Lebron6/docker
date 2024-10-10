package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class DockOpenManager extends BaseManager {

    private final int maxRetries = 15;
    private int sendDockOpenSuccessTimes;
    private boolean isSendDockOpenSuccess;
    private DockOpenManager() {
    }

    private static class DockOpenHolder {
        private static final DockOpenManager INSTANCE = new DockOpenManager();
    }

    public static DockOpenManager getInstance() {
        return DockOpenManager.DockOpenHolder.INSTANCE;
    }


    public void sendDockOpenMsg2Server(MqttAndroidClient client) {
        if (isSendDockOpenSuccess||sendDockOpenSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开舱");
            return;
        }
        try {
            if (client.isConnected()) {
                sendDockOpenMessage(client);
            } else {
                handleNotConnected(client);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开舱发送异常：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendDockOpenMessage(MqttAndroidClient client) throws Exception {
        MessageReply message = new MessageReply();
        message.setMsg_type(60108);
        message.setResult(1);

        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(2);

        client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                LogUtil.log(TAG, "开舱发送成功：60108---"+sendDockOpenSuccessTimes);
                sendMissionExecuteEvents(client, "AMS通知机库开舱");
                isSendDockOpenSuccess = true;
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LogUtil.log(TAG, "开舱发送回调失败：" + exception.toString());
                retrySend(client);
            }
        });
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client) {
        sendDockOpenSuccessTimes++;
        if (sendDockOpenSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDockOpenMsg2Server(client), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，开舱发送失败：" + sendDockOpenSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client) {
        if (!isSendDockOpenSuccess && sendDockOpenSuccessTimes < maxRetries) {
            sendDockOpenSuccessTimes++;
            new Handler().postDelayed(() -> sendDockOpenMsg2Server(client), 2000);
            LogUtil.log(TAG, "开舱发送失败：mqtt未连接" + "--" + sendDockOpenSuccessTimes);
        } else {
            LogUtil.log(TAG, "开舱发送失败：" + sendDockOpenSuccessTimes);
        }
    }

}