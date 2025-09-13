package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class DroneStorageManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendDroneStorageSuccessTimes;
    private boolean isSendDroneStorageSuccess;

    private DroneStorageManager() {
    }

    private static class DroneStorageHolder {
        private static final DroneStorageManager INSTANCE = new DroneStorageManager();
    }

    public static DroneStorageManager getInstance() {
        return DroneStorageHolder.INSTANCE;
    }

    public void sendDroneStorageMsg2Server(int result) {
//        if (isSendDroneStorageSuccess||sendDroneStorageSuccessTimes >= maxRetries) {
        if (sendDroneStorageSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送入库"+isSendDroneStorageSuccess+sendDroneStorageSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendDroneStorageMessage(MqttManager.getInstance().mqttAndroidClient,result);
            } else {
                handleNotConnected(result);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "入库发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDroneStorageMessage(MqttAndroidClient client,int result){
        MessageReply message = new MessageReply();
        message.setMsg_type(60010);
        message.setResult(result);

        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(0);

        try {
            client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "入库已发送：60010---" + sendDroneStorageSuccessTimes + "clientId:" + client.getClientId());
                    sendMissionExecuteEvents( "AMS通知机库入库");

                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (ApronExecutionStatus.getInstance().isServerReplyDockIn()) {
                                isSendDroneStorageSuccess = true;
                                LogUtil.log(TAG, "已经收到服务端响应入库");
                            } else {
                                LogUtil.log(TAG, "未收到服务端响应入库,重新发送");
                                retrySend(result);
                            }
                        }
                    }, 2000);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "入库发送回调失败：" + exception.toString());
                    retrySend(result);
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "入库发送异常：" + e.toString());
            e.printStackTrace();
        }
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(int result) {
        sendDroneStorageSuccessTimes++;
        if (sendDroneStorageSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> DroneStorageManager.getInstance().sendDroneStorageMsg2Server(result), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，入库发送失败：" + sendDroneStorageSuccessTimes);
        }
    }

    private void handleNotConnected(int result) {
        if (!isSendDroneStorageSuccess && sendDroneStorageSuccessTimes < maxRetries) {
            sendDroneStorageSuccessTimes++;
            mainHandler.postDelayed(() -> DroneStorageManager.getInstance().sendDroneStorageMsg2Server(result), 2000);
            LogUtil.log(TAG, "入库发送失败：mqtt未连接" + "--" + sendDroneStorageSuccessTimes);
        } else {
            LogUtil.log(TAG, "入库发送失败：" + sendDroneStorageSuccessTimes);
        }
    }

}