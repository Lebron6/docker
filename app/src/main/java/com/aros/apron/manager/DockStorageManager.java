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

public class DockStorageManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendDockStorageSuccessTimes;
    private boolean isSendDockStorageSuccess;
    private DockStorageManager() {
    }

    private static class DockStorageHolder {
        private static final DockStorageManager INSTANCE = new DockStorageManager();
    }

    public static DockStorageManager getInstance() {
        return DockStorageHolder.INSTANCE;
    }

    public void sendDockStorageMsg2Server() {
        if (sendDockStorageSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送入库"+isSendDockStorageSuccess+sendDockStorageSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendDockStorageMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "入库发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDockStorageMessage(){
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(Constant.INBOUND);
                MessageEvent.Data data=new MessageEvent.Data();
                data.setResult(1);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        sendEvent2Server("AMS通知机库入库");
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (ApronExecutionStatus.getInstance().isServerReplyDockIn()) {
                                    isSendDockStorageSuccess = true;
                                    LogUtil.log(TAG, "已经收到服务端响应入库");
                                } else {
                                    if (!Movement.getInstance().isPlaneWing()){
                                        LogUtil.log(TAG, "未收到收到服务端响应入库,重新发送");
                                        retrySend();
                                    }else{
                                        LogUtil.log(TAG,"飞机状态不满足入库门条件:"+Movement.getInstance().isPlaneWing());
                                    }
                                }
                            }
                        }, 2000);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        LogUtil.log(TAG, "入库发送回调失败：" + exception.toString());
                        retrySend();
                    }
                });
            } else {
                LogUtil.log(TAG, "入库发送回调失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "入库发送异常：" + e.toString());

        }


    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend() {
        sendDockStorageSuccessTimes++;
        if (sendDockStorageSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDockStorageMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，入库发送失败：" + sendDockStorageSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendDockStorageSuccess && sendDockStorageSuccessTimes < maxRetries) {
            sendDockStorageSuccessTimes++;
            mainHandler.postDelayed(() -> sendDockStorageMsg2Server(), 2000);
            LogUtil.log(TAG, "入库发送失败：mqtt未连接" + sendDockStorageSuccessTimes);
        } else {
            LogUtil.log(TAG, "入库发送失败：" + sendDockStorageSuccessTimes);
        }
    }

}