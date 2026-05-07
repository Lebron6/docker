package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageEvent;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ShutDownManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendShutDownSuccessTimes;
    private boolean isSendShutDownSuccess;
    private ShutDownManager() {
    }

    private static class ShutDownHolder {
        private static final ShutDownManager INSTANCE = new ShutDownManager();
    }

    public static ShutDownManager getInstance() {
        return ShutDownHolder.INSTANCE;
    }

    public void sendShutDownMsg2Server() {
        if (sendShutDownSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送关机"+
                    isSendShutDownSuccess+sendShutDownSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendShutDownMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关机发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendShutDownMessage(){
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(Constant.OPEN_DOOR);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        sendEvent2Server("AMS通知机库关机",1);
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (ApronExecutionStatus.getInstance().isAircraftWaitShutDown()) {
                                    isSendShutDownSuccess = true;
                                    LogUtil.log(TAG, "已经收到服务端响应开门");
                                } else {
                                    if (Movement.getInstance().isPlaneWing()&&Movement.getInstance().getElevation()>40){
                                        LogUtil.log(TAG, "未收到收到服务端响应开门,重新发送");
                                        retrySend();
                                    }else{
                                        LogUtil.log(TAG,"飞机状态不满足关机门条件:"+Movement.getInstance().isPlaneWing()+"--"+Movement.getInstance().getElevation());
                                    }
                                }
                            }
                        }, 2000);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        LogUtil.log(TAG, "关机发送回调失败：" + exception.toString());
                        retrySend();
                    }
                });
            } else {
                LogUtil.log(TAG, "关机发送回调失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "关机发送异常：" + e.toString());

        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend() {
        sendShutDownSuccessTimes++;
        if (sendShutDownSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendShutDownMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，关机发送失败：" + sendShutDownSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendShutDownSuccess && sendShutDownSuccessTimes < maxRetries) {
            sendShutDownSuccessTimes++;
            mainHandler.postDelayed(() -> sendShutDownMsg2Server(), 2000);
            LogUtil.log(TAG, "关机发送失败：mqtt未连接" + sendShutDownSuccessTimes);
        } else {
            LogUtil.log(TAG, "关机发送失败：" + sendShutDownSuccessTimes);
        }
    }

}