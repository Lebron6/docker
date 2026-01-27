package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;
import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.TaskFailEvent;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 任务失败入库
 */
public class TaskFailManager extends BaseManager {

    private final int maxRetries = 20;
    private int sendTaskFailSuccessTimes;
    private boolean isSendTaskFailSuccess;

    private TaskFailManager() {
    }

    private static class TaskFailHolder {
        private static final TaskFailManager INSTANCE = new TaskFailManager();
    }

    public static TaskFailManager getInstance() {
        return TaskFailHolder.INSTANCE;
    }


    public void sendTaskFailMsg2Server(int reason_code) {
        if (sendTaskFailSuccessTimes >= maxRetries) {
            LogUtil.log(TAG, "达到最大重试次数或已发送TaskFail" + isSendTaskFailSuccess + sendTaskFailSuccessTimes);
            return;
        }
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                sendDockCloseMessage(reason_code);
            } else {
                handleNotConnected(reason_code);
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "TaskFail异常：" + e.toString());
            e.printStackTrace();
        }
    }

    private void sendDockCloseMessage(int reason_code) {
        TaskFailEvent messageEvent = new TaskFailEvent();
        messageEvent.setBid(UUID.randomUUID().toString());
        messageEvent.setTid(UUID.randomUUID().toString());
        messageEvent.setTimestamp(System.currentTimeMillis());
        messageEvent.setMethod(Constant.TASK_FAIL);
        TaskFailEvent.Data data = new TaskFailEvent.Data();
        data.setReason_code(reason_code);
        MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        try {
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtil.log(TAG, "TaskFail发送成功：" + sendTaskFailSuccessTimes + "clientId:" + MqttManager.getInstance().mqttAndroidClient.getClientId());
                    sendEvent2Server("AMS通知机库TaskFail", 1);
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (ApronExecutionStatus.getInstance().isServerReplyTaskFail()) {
                                isSendTaskFailSuccess = true;
                                LogUtil.log(TAG, "已经收到服务端响应TaskFail");
                            } else {
                                LogUtil.log(TAG, "未收到收到服务端响应TaskFail,重新发送");
                                retrySend(reason_code);
                            }
                        }
                    }, 2000);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtil.log(TAG, "TaskFail发送回调失败：" + exception.toString());
                    retrySend(reason_code);
                }
            });
        } catch (Exception e) {
            LogUtil.log(TAG, "TaskFail发送异常：" + e.toString());
            e.printStackTrace();
        }


    }

    final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void retrySend(int reason_code) {
        sendTaskFailSuccessTimes++;
        if (sendTaskFailSuccessTimes < maxRetries) {
            mainHandler.postDelayed(() -> sendTaskFailMsg2Server(reason_code), 2000);
        } else {
            LogUtil.log(TAG, "达到最大重试次数，TaskFail发送失败：" + sendTaskFailSuccessTimes);
        }
    }

    private void handleNotConnected(int reason_code) {
        if (!isSendTaskFailSuccess && sendTaskFailSuccessTimes < maxRetries) {
            sendTaskFailSuccessTimes++;
            mainHandler.postDelayed(() -> sendTaskFailMsg2Server(reason_code), 2000);
            LogUtil.log(TAG, "TaskFail发送失败：mqtt未连接" + sendTaskFailSuccessTimes);
        } else {
            LogUtil.log(TAG, "TaskFail发送失败：" + sendTaskFailSuccessTimes);
        }
    }

}