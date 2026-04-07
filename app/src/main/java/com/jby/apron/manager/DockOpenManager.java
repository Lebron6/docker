package com.jby.apron.manager;

import android.os.Handler;
import android.os.Looper;

import com.jby.apron.base.BaseManager;
import com.jby.apron.constant.AMSConfig;
import com.jby.apron.constant.Constant;
import com.jby.apron.entity.ApronExecutionStatus;
import com.jby.apron.entity.MessageEvent;
import com.jby.apron.entity.MessageReply;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.MqttManager;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    public void sendDockOpenMsg2Server() {
        if (sendDockOpenSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送开舱"+isSendDockOpenSuccess+sendDockOpenSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendDockOpenMessage();
            } else {
                handleNotConnected();
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开舱发送异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDockOpenMessage(){
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(Constant.OPEN_DOOR);
                MessageEvent.Data data=new MessageEvent.Data();
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        sendEvent2Server("AMS通知机库开舱",1);
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (ApronExecutionStatus.getInstance().isServerReplyDockOpen()) {
                                    isSendDockOpenSuccess = true;
                                    LogUtil.log(TAG, "已经收到服务端响应开门");
                                } else {
                                    if (Movement.getInstance().isPlaneWing()&&Movement.getInstance().getElevation()>40){
                                        LogUtil.log(TAG, "未收到收到服务端响应开门,重新发送");
                                        retrySend();
                                    }else{
                                        LogUtil.log(TAG,"飞机状态不满足开舱门条件:"+Movement.getInstance().isPlaneWing()+"--"+Movement.getInstance().getElevation());
                                    }
                                }
                            }
                        }, 2000);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        LogUtil.log(TAG, "开舱发送回调失败：" + exception.toString());
                        retrySend();
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

    private void retrySend() {
        sendDockOpenSuccessTimes++;
        if (sendDockOpenSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendDockOpenMsg2Server(), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，开舱发送失败：" + sendDockOpenSuccessTimes);
        }
    }

    private void handleNotConnected() {
        if (!isSendDockOpenSuccess && sendDockOpenSuccessTimes < maxRetries) {
            sendDockOpenSuccessTimes++;
            mainHandler.postDelayed(() -> sendDockOpenMsg2Server(), 2000);
            LogUtil.log(TAG, "开舱发送失败：mqtt未连接" + sendDockOpenSuccessTimes);
        } else {
            LogUtil.log(TAG, "开舱发送失败：" + sendDockOpenSuccessTimes);
        }
    }

}