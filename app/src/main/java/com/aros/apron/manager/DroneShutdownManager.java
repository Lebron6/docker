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

public class DroneShutdownManager extends BaseManager {

    private final int maxRetries = 10;
    private int sendDroneShutDownSuccessTimes;
    private boolean isSendDroneShutDownSuccess;

    private DroneShutdownManager() {
    }

    private static class DroneShutHolder {
        private static final DroneShutdownManager INSTANCE = new DroneShutdownManager();
    }

    public static DroneShutdownManager getInstance() {
        return DroneShutdownManager.DroneShutHolder.INSTANCE;
    }


    public void sendDroneShutDownMsg2Server(MqttAndroidClient client) {
        if (isSendDroneShutDownSuccess||sendDroneShutDownSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送关机");
            return;
        }
        try {
            if (client.isConnected()) {
                sendShutDownMessage(client);
            } else {
                handleNotConnected(client);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关机发送异常：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendShutDownMessage(MqttAndroidClient client) throws Exception {
        MessageReply message = new MessageReply();
        message.setMsg_type(60011);
        message.setResult(1);

        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(2);

        client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                LogUtil.log(TAG, "关机发送成功：60011---"+sendDroneShutDownSuccessTimes);
                sendMissionExecuteEvents(client, "AMS通知机库执行无人机关机");
                isSendDroneShutDownSuccess = true;
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LogUtil.log(TAG, "关机发送回调失败：" + exception.toString());
                retrySend(client);
            }
        });
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client) {
        sendDroneShutDownSuccessTimes++;
        if (sendDroneShutDownSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDroneShutDownMsg2Server(client), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，关机发送失败：" + sendDroneShutDownSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client) {
        if (!isSendDroneShutDownSuccess && sendDroneShutDownSuccessTimes < maxRetries) {
            sendDroneShutDownSuccessTimes++;
            new Handler().postDelayed(() -> sendDroneShutDownMsg2Server(client), 2000);
            LogUtil.log(TAG, "关机发送失败：mqtt未连接" + "--" + sendDroneShutDownSuccessTimes);
        } else {
            LogUtil.log(TAG, "关机发送失败：" + sendDroneShutDownSuccessTimes);
        }
    }

}