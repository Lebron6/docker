package com.jby.apron.callback;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.jby.apron.constant.AMSConfig;
import com.jby.apron.constant.Constant;
import com.jby.apron.entity.MessageEvent;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.MqttManager;
import com.jby.apron.tools.ToastUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;

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
        ToastUtil.showToast("MQTT连接成功");
        LogUtil.log(TAG, "MQTT连接成功：-------");
        try {
            //飞机SN号
            MqttManager.getInstance().publishStatus(Constant.REMOTE_ONLINE);
            mqttAndroidClient.subscribe(AMSConfig.DOWN_UAV_SERVICES, 1);//订阅主题:注册
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String sn) {
        try {
            if (mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(Constant.REMOTE_ONLINE);
                MessageEvent.Data data = new MessageEvent.Data();
                data.setSn(sn);
                messageEvent.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.REGISTER, mqttMessage);
            } else {
                LogUtil.log(TAG, "注册失败：MQtt未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "注册异常：" + e.toString());
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        LogUtil.log(TAG, "MQtt连接失败:" + exception.toString());
        try {
            if (!mqttAndroidClient.isConnected()) {
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
