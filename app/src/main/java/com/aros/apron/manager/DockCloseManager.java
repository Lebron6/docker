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
        return DockCloseManager.DockCloseHolder.INSTANCE;
    }


    public void sendDockCloseMsg2Server(MqttAndroidClient client) {
        if (isSendDockCloseSuccess||sendDockCloseSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送关舱");
            return;
        }
        try {
            if (client.isConnected()) {
                sendDockCloseMessage(client);
            } else {
                handleNotConnected(client);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关舱发送异常：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendDockCloseMessage(MqttAndroidClient client) throws Exception {
        MessageReply message = new MessageReply();
        message.setMsg_type(60107);
        message.setResult(1);

        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(2);

        client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                LogUtil.log(TAG, "关舱发送成功：60107---"+sendDockCloseSuccessTimes);
                sendMissionExecuteEvents(client, "AMS通知机库关舱");
                isSendDockCloseSuccess = true;
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LogUtil.log(TAG, "关舱发送回调失败：" + exception.toString());
                retrySend(client);
            }
        });
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client) {
        sendDockCloseSuccessTimes++;
        if (sendDockCloseSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDockCloseMsg2Server(client), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，关舱发送失败：" + sendDockCloseSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client) {
        if (!isSendDockCloseSuccess && sendDockCloseSuccessTimes < maxRetries) {
            sendDockCloseSuccessTimes++;
            new Handler().postDelayed(() -> sendDockCloseMsg2Server(client), 2000);
            LogUtil.log(TAG, "关舱发送失败：mqtt未连接" + "--" + sendDockCloseSuccessTimes);
        } else {
            LogUtil.log(TAG, "关舱发送失败：" + sendDockCloseSuccessTimes);
        }
    }

}