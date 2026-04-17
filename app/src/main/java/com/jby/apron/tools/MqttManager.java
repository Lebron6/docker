package com.jby.apron.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.google.gson.Gson;
import com.jby.apron.R;
import com.jby.apron.app.ApronApp;
import com.jby.apron.callback.MqttActionCallBack;
import com.jby.apron.callback.MqttCallBack;
import com.jby.apron.constant.AMSConfig;
import com.jby.apron.constant.Constant;
import com.jby.apron.entity.MessageEvent;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Random;
import java.util.UUID;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.KeyManager;

public class MqttManager {

    public MqttAndroidClient mqttAndroidClient; //ltz change
    public MqttConnectOptions mMqttConnectOptions;
    String TAG = getClass().getSimpleName();

    private MqttManager() {
    }

    private static class MqttHolder {
        private static final MqttManager INSTANCE = new MqttManager();
    }

    public static MqttManager getInstance() {
        return MqttHolder.INSTANCE;
    }

    public void needConnect(Context context) throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
        initMqttClientParams(context);
    }

    private void initMqttClientParams(Context context) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream ksInput = context.getResources().openRawResource(R.raw.client); // client.p12
        keyStore.load(ksInput, "123456".toCharArray()); // p12 密码
        ksInput.close();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
        kmf.init(keyStore, "123456".toCharArray());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = context.getResources().openRawResource(R.raw.ca_cert);
        Certificate ca = cf.generateCertificate(caInput);
        caInput.close();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        SocketFactory socketFactory = sslContext.getSocketFactory();


        mqttAndroidClient = new MqttAndroidClient(ApronApp.Companion.getApplication(),
                "ssl://117.140.203.9:8883",
                generateRandomString(10));
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setSocketFactory(socketFactory);
        mMqttConnectOptions.setAutomaticReconnect(true); //ltz add
        mMqttConnectOptions.setMaxInflight(1000);// 增加最大并发未确认消息数量
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(30); //设置超时时间，单位：秒 ltz denote
        mMqttConnectOptions.setKeepAliveInterval(20); //设置心跳包发送间隔，单位：秒 ltz denote
        mMqttConnectOptions.setUserName("admin"); //设置用户名
        mMqttConnectOptions.setPassword("J100y@ups.2025".toCharArray()); //设置密码
        mqttAndroidClient.setCallback(new MqttCallBack()); //设置监听订阅消息的回调
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
        ConnectivityManager connectivityManager = (ConnectivityManager) ApronApp.Companion.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            LogUtil.log(TAG, "当前网络名称：" + name);
            return true;
        } else {
            LogUtil.log(TAG, "没有可用Mqtt网络,延迟三秒后重连");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
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
                doClientConnection();
            }
        }, 3000);
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

    public void publishStatus(String method) {
        String remoteSn = KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.
                KeySerialNumber));
        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(method);
                MessageEvent.Data data = new MessageEvent.Data();
                data.setSn(remoteSn);
                messageEvent.setData(data);
                MqttMessage mqttMessage =
                        new MqttMessage(new Gson().toJson(messageEvent).getBytes("UTF-8"));
                mqttMessage.setQos(1);
                mqttAndroidClient.publish(AMSConfig.REGISTER, mqttMessage);
            } else {
                LogUtil.log(TAG, method + "失败：MQtt未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, method + "异常：" + e.toString());
        }
    }
}
