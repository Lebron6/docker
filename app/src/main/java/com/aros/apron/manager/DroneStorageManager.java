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

public class DroneStorageManager extends BaseManager {

    private final int maxRetries = 10;
    private int sendDroneStorageSuccessTimes;
    private boolean isSendDroneStorageSuccess;

    private DroneStorageManager() {
    }

    private static class DroneStorageHolder {
        private static final DroneStorageManager INSTANCE = new DroneStorageManager();
    }

    public static DroneStorageManager getInstance() {
        return DroneStorageManager.DroneStorageHolder.INSTANCE;
    }

    public void sendDroneStorageMsg2Server(MqttAndroidClient client,int result) {
        if (isSendDroneStorageSuccess||sendDroneStorageSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送入库");
            return;
        }
        try {
            if (client.isConnected()) {
                sendDroneStorageMessage(client,result);
            } else {
                handleNotConnected(client,result);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "入库发送异常：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendDroneStorageMessage(MqttAndroidClient client,int result) throws Exception {
        MessageReply message = new MessageReply();
        message.setMsg_type(60010);
        message.setResult(result);

        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(2);

        client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                LogUtil.log(TAG, "入库发送成功：60010---"+sendDroneStorageSuccessTimes);
                sendMissionExecuteEvents(client, "AMS通知机库入库");
                isSendDroneStorageSuccess = true;
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LogUtil.log(TAG, "入库发送回调失败：" + exception.toString());
                retrySend(client,result);
            }
        });
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(MqttAndroidClient client,int result) {
        sendDroneStorageSuccessTimes++;
        if (sendDroneStorageSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> DroneStorageManager.getInstance().sendDroneStorageMsg2Server(client,result), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，入库发送失败：" + sendDroneStorageSuccessTimes);
        }
    }

    private void handleNotConnected(MqttAndroidClient client,int result) {
        if (!isSendDroneStorageSuccess && sendDroneStorageSuccessTimes < maxRetries) {
            sendDroneStorageSuccessTimes++;
            new Handler().postDelayed(() -> DroneStorageManager.getInstance().sendDroneStorageMsg2Server(client,result), 2000);
            LogUtil.log(TAG, "入库发送失败：mqtt未连接" + "--" + sendDroneStorageSuccessTimes);
        } else {
            LogUtil.log(TAG, "入库发送失败：" + sendDroneStorageSuccessTimes);
        }
    }

}