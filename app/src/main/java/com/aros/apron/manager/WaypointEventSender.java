package com.aros.apron.manager;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WaypointEventSender {

    private static final String TAG = "WaypointEventSender";
    private static WaypointEventSender instance;

    // 单线程调度线程池，保证顺序执行
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // 记录上一次任务的预计执行时间
    private long lastScheduleTime = 0;

    private WaypointEventSender() {}

    public static WaypointEventSender getInstance() {
        if (instance == null) {
            synchronized (WaypointEventSender.class) {
                if (instance == null) {
                    instance = new WaypointEventSender();
                }
            }
        }
        return instance;
    }

    public void sendCustomReachOrLeave2Server(String data, String index) {
        synchronized (this) {
            long now = System.currentTimeMillis();

            // 计算下一次执行的时间，至少比上一次晚 1 秒
            long nextTime = Math.max(now, lastScheduleTime + 1000);
            long delay = nextTime - now;

            lastScheduleTime = nextTime;

            executor.schedule(() -> doSend(data, index), delay, TimeUnit.MILLISECONDS);

        }
    }

    private void doSend(String data, String index) {
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageReply message = new MessageReply();
                message.setMsg_type(60203);
                message.setResult(1);
                message.setWaypointActionState(data);
                message.setWaypointIndex(index);

                MqttMessage mqttMessage =
                        new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(1); // QoS=1 更合适

                MqttManager.getInstance().mqttAndroidClient.publish(
                        AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(),
                        mqttMessage
                );

                LogUtil.log(TAG, "推送成功：" + data + " index=" + index);
            } else {
                LogUtil.log(TAG, "推送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "推送异常"+e.toString());
        }
    }
}