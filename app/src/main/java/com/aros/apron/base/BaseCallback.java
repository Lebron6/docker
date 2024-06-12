package com.aros.apron.base;

import com.aros.apron.tools.LogUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class BaseCallback {
    public void publish(MqttAndroidClient client, String topic, MqttMessage message) {

        if (client.isConnected()) {
            try {
                client.publish(topic, message);
            } catch (MqttException e) {
                e.printStackTrace();
                LogUtil.log(this.getClass().getSimpleName(), "推送失败:" + topic + e.toString());
            }
        } else {
            LogUtil.log(this.getClass().getSimpleName(), "推送失败:mqtt 未连接");
        }

    }
}
