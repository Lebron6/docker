package com.aros.apron.callback;

import android.os.Handler;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.ToastUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttActionCallBack implements IMqttActionListener {

    private final String TAG = "MqttActionCallBack";
    private MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions options;

    public MqttActionCallBack(MqttAndroidClient mqttAndroidClient, MqttConnectOptions options) {
        this.mqttAndroidClient = mqttAndroidClient;
        this.options = options;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        ToastUtil.showToast("MQtt连接成功");
        LogUtil.log(TAG, "MQtt连接成功：-------");
        try {
            mqttAndroidClient.subscribe(AMSConfig.DOWN_UAV_SERVICES, 1);//订阅主题:注册
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        LogUtil.log(TAG, "MQtt连接失败:" + exception.toString());
        try {
            if (!mqttAndroidClient.isConnected()){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mqttAndroidClient.connect(options, null, MqttActionCallBack.this); // 再次尝试连接
                        } catch (MqttException e) {
                            LogUtil.log(TAG,"mqtt重连异常:"+e.toString());
                            e.printStackTrace();
                        }

                    }
                },1500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
