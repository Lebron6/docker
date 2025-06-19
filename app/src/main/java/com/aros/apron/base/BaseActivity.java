package com.aros.apron.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.aros.apron.callback.MqttActionCallBack;
import com.aros.apron.callback.MqttCallBack;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.tools.AppManager;
import com.aros.apron.tools.LogUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;

import java.util.Random;



public abstract class BaseActivity extends AppCompatActivity {
    /**
     * activity堆栈管理
     */
    protected AppManager appManager = AppManager.getAppManager();

    public MqttAndroidClient mqttAndroidClient; //ltz change
    public MqttConnectOptions mMqttConnectOptions;

    protected String TAG;
    protected boolean useEventBus = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loggerSimpleName();
        if (useEventBus()) {
            EventBus.getDefault().register(this);
        }
        appManager.addActivity(this);
    }

    public void needConnect() {
        initMqttClientParams();
    }

    private void initMqttClientParams() {
        if (mqttAndroidClient==null){
            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), AMSConfig.getInstance().getMqttServerUri(), generateRandomString(10));
        }
        if (mMqttConnectOptions==null){
            mMqttConnectOptions = new MqttConnectOptions();
        }
        mMqttConnectOptions.setAutomaticReconnect(true); //ltz add
        mMqttConnectOptions.setMaxInflight(100);// 增加最大并发未确认消息数量
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(10); //设置超时时间，单位：秒 ltz denote
        mMqttConnectOptions.setKeepAliveInterval(5); //设置心跳包发送间隔，单位：秒 ltz denote
        mMqttConnectOptions.setUserName(AMSConfig.getInstance().getUserName()); //设置用户名
        mMqttConnectOptions.setPassword(AMSConfig.getInstance().getPassword().toCharArray()); //设置密码
        mqttAndroidClient.setCallback(new MqttCallBack(mqttAndroidClient,mMqttConnectOptions)); //设置监听订阅消息的回调
        doClientConnection();
    }



    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, new MqttActionCallBack(mqttAndroidClient,mMqttConnectOptions));
            } catch (MqttException e) {
                LogUtil.log(TAG,"mqtt连接异常:"+e.toString());
                e.printStackTrace();
            }
        }

    }


    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            LogUtil.log(TAG, "当前网络名称：" + name);
            return true;
        } else {
            LogUtil.log(TAG, "没有可用Mqtt网络");
            /*没有可用网络的时候，延迟5秒再尝试重连*/
            doConnectionDelay();
            return false;
        }
    }


    /**
     * 没有可用网络的时候，延迟5秒再尝试重连
     */
    private void doConnectionDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LogUtil.log(TAG,"延迟3s后重连");
                doClientConnection();
            }
        }, 3000);
    }

//    private void connect() {
//        if (!NettyClient.getInstance().getConnectStatus()) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    NettyClient.getInstance().connect();//连接服务器
//                }
//            }).start();
//        }
//    }
    public abstract boolean useEventBus();

    public void loggerSimpleName() {
        TAG = getClass().getSimpleName();
//        Log.e("Aros","当前界面 ："+ TAG);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 从栈中移除activity
        appManager.finishActivity(this);
        if (useEventBus == true) {
            EventBus.getDefault().unregister(this);
        }
        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.unregisterResources();
                mqttAndroidClient.disconnect(); //断开连接
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
//        NettyClient.getInstance().setReconnectNum(0);
//        NettyClient.getInstance().disconnect();
    }
    private String generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }
}
