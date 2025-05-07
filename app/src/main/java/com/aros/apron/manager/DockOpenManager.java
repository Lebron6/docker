package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class DockOpenManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendDockOpenSuccessTimes;
    private boolean isSendDockOpenSuccess;
    private DockOpenManager() {
    }

    private static class DockOpenHolder {
        private static final DockOpenManager INSTANCE = new DockOpenManager();
    }

    public static DockOpenManager getInstance() {
        return DockOpenHolder.INSTANCE;
    }


    public void sendDockOpenMsg2Server(MqttAndroidClient client) {
//        if (isSendDockOpenSuccess||sendDockOpenSuccessTimes >= maxRetries) {
        if (sendDockOpenSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开舱"+isSendDockOpenSuccess+sendDockOpenSuccessTimes);
            return;
        }
        try {
            if (client.isConnected()) {
                sendDockOpenMessage(client);
            } else {
                handleNotConnected(client);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开舱发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDockOpenMessage(MqttAndroidClient client){

        try {
            if (client.isConnected()) {

                MessageReply message = new MessageReply();
                message.setMsg_type(60108);
                message.setResult(1);
                message.setMsg("开门");

                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        LogUtil.log(TAG, "开舱发送成功：60108---" + sendDockOpenSuccessTimes + "clientId:" + client.getClientId());
                        sendMissionExecuteEvents(client, "AMS通知机库开舱");
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (ApronExecutionStatus.getInstance().isServerReplyDockIn()) {
                                    isSendDockOpenSuccess = true;
                                    LogUtil.log(TAG, "已经收到服务端响应开门");
                                } else {
                                    if (Movement.getInstance().isPlaneWing()&&Movement.getInstance().getFlyingHeight()>40){
                                        LogUtil.log(TAG, "未收到收到服务端响应开门,重新发送");
                                        retrySend(client);
                                    }else{
                                        LogUtil.log(TAG,"飞机状态不满足开舱门条件:"+Movement.getInstance().isPlaneWing()+"--"+Movement.getInstance().getFlyingHeight());
                                    }
                                }
                            }
                        }, 2000);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        LogUtil.log(TAG, "开舱发送回调失败：" + exception.toString());
                        retrySend(client);
                    }
                });
            } else {
                LogUtil.log(TAG, "开舱发送回调失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "开舱发送异常：" + e.toString());

        }


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
            mainHandler.postDelayed(() -> sendDockOpenMsg2Server(client), 2000);
            LogUtil.log(TAG, "开舱发送失败：mqtt未连接" + "--" + sendDockOpenSuccessTimes);
        } else {
            LogUtil.log(TAG, "开舱发送失败：" + sendDockOpenSuccessTimes);
        }
    }

}