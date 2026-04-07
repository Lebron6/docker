package com.jby.apron.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.jby.apron.callback.MqttActionCallBack;
import com.jby.apron.callback.MqttCallBack;
import com.jby.apron.constant.AMSConfig;
import com.jby.apron.tools.AppManager;
import com.jby.apron.tools.LogUtil;

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

    }

}
