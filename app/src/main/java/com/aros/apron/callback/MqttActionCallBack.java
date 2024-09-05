package com.aros.apron.callback;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.ToastUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttActionCallBack implements IMqttActionListener {

    private final String TAG = "MqttActionCallBack";
    private MqttAndroidClient mqttAndroidClient;

    public MqttActionCallBack(MqttAndroidClient mqttAndroidClient) {
        this.mqttAndroidClient = mqttAndroidClient;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        ToastUtil.showToast("MQtt连接成功");
        LogUtil.log(TAG, "MQtt连接成功：-------");
        try {
            mqttAndroidClient.subscribe(AMSConfig.getInstance().getMqttServer2MsdkTopic(), 2);//订阅主题:注册
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        LogUtil.log(TAG, "MQtt连接失败:" + exception.toString());
    }
}
