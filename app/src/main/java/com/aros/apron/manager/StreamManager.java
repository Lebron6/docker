package com.aros.apron.manager;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.livestream.LiveStreamManager;
import dji.v5.manager.datacenter.livestream.LiveStreamSettings;
import dji.v5.manager.datacenter.livestream.LiveStreamStatus;
import dji.v5.manager.datacenter.livestream.LiveStreamStatusListener;
import dji.v5.manager.datacenter.livestream.LiveStreamType;
import dji.v5.manager.datacenter.livestream.LiveVideoBitrateMode;
import dji.v5.manager.datacenter.livestream.StreamQuality;
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings;
import dji.v5.manager.interfaces.ILiveStreamManager;


public class StreamManager extends BaseManager {


    private StreamManager() {
    }

    private static class StreamHolder {
        private static final StreamManager INSTANCE = new StreamManager();
    }

    public static StreamManager getInstance() {
        return StreamHolder.INSTANCE;
    }

    public void sendReply2Server(MqttAndroidClient client, MQMessage message) {
        sendMsg2Server(client, message);
    }

    public void initStreamManager(MqttAndroidClient client) {
        ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
        if (liveStreamManager != null) {
            liveStreamManager.addLiveStreamStatusListener(new LiveStreamStatusListener() {
                @Override
                public void onLiveStreamStatusUpdate(LiveStreamStatus status) {
                    if (status != null) {
                        Movement.getInstance().setLiveStatus(status.isStreaming() ? 1 : 0);
//                        if (isGisFlyClickTime()) {
//                            LogUtil.log(TAG, "帧率:" + status.getFps() + "--" + "码率:" + status.getVbps()+"---"+"延迟:"+status.getRtt());
//                        }
                    }
                }

                @Override
                public void onError(IDJIError error) {

                }
            });
        }
    }

    public void startLive(MqttAndroidClient client, MQMessage message) {

        Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
        if (isAircraftConnected == null || !isAircraftConnected) {
            LogUtil.log(TAG, "飞行器未连接");
        } else {
            ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
            if (TextUtils.isEmpty(message.getRtmp_push_url())) {
                LogUtil.log(TAG, "推流地址配置有误");
                sendMsg2Server(client, message, "推流地址配置有误");
            }
            LiveStreamSettings.Builder streamSettingBuilder = new LiveStreamSettings.Builder();
            LiveStreamSettings streamSettings = streamSettingBuilder.setLiveStreamType(LiveStreamType.RTMP)
                    .setRtmpSettings(new RtmpSettings.Builder().setUrl(PreferenceUtils.getInstance().getCustomStreamEnable()?PreferenceUtils.getInstance().getCustomStreamUrl() : message.getRtmp_push_url()).build()).build();
            liveStreamManager.setLiveStreamSettings(streamSettings);
//            if (message.getStreamIndex() == 0) {
                liveStreamManager.setCameraIndex(ComponentIndexType.LEFT_OR_MAIN);
//            } else {
//                liveStreamManager.setCameraIndex(ComponentIndexType.FPV);
//            }
            if (message.getMsg_type()==60003){
                liveStreamManager.setLiveStreamQuality(StreamQuality.FULL_HD);
            }
            liveStreamManager.setLiveVideoBitrateMode(LiveVideoBitrateMode.AUTO);
            if (!liveStreamManager.isStreaming()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        liveStreamManager.startStream(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                LogUtil.log(TAG, "推流成功:"+(PreferenceUtils.getInstance().getCustomStreamEnable()?PreferenceUtils.getInstance().getCustomStreamUrl() : message.getRtmp_push_url()));
                                sendMsg2Server(client, message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG, "推流失败:" + error.description() + "---");
                                sendMsg2Server(client, message, error.description());

                            }
                        });
                    }
                }, 1000);
            }
        }
    }

    public void setLiveStreamQuality(MqttAndroidClient client, MQMessage message) {
        Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
        if (isAircraftConnected == null || !isAircraftConnected) {
            LogUtil.log(TAG, "飞行器未连接");
            sendMsg2Server(client, message, "飞行器未连接");
        } else {
            ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
            if (liveStreamManager.isStreaming()) {
                liveStreamManager.setLiveStreamQuality(StreamQuality.find(message.getLiveStreamQuality()));
                sendMsg2Server(client, message);
            } else {
                sendMsg2Server(client, message, "推流未开启");
            }
        }
    }


//    public void switchCurrentView(MqttAndroidClient mqttAndroidClient, MQMessage message){
//        Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
//        if (isAircraftConnected == null || !isAircraftConnected) {
//            sendMsg2Server(mqttAndroidClient, message, "飞行器未连接");
//        } else {
//            ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
//            Movement.getInstance().setCurrentView(message.getData().getCurrentView());
//            if (message.getData().getCurrentView()==1){
//                liveStreamManager.setCameraIndex(ComponentIndexType.FPV);
//            }else{
//                liveStreamManager.setCameraIndex(ComponentIndexType.LEFT_OR_MAIN);
//            }
//
//        }
//    }


    public void stopLive(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        ILiveStreamManager iLiveStreamManager = LiveStreamManager.getInstance();
        iLiveStreamManager.stopStream(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server(mqttAndroidClient, message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendMsg2Server(mqttAndroidClient, message, "停止直播失败:" + error.description());
            }
        });
    }

    //知眸测试
    public void startLiveWithCustom() {

        if (PreferenceUtils.getInstance().getCustomStreamEnable()){
            Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
            if (isAircraftConnected == null || !isAircraftConnected) {
                LogUtil.log(TAG, "飞行器未连接");

            } else {
                ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
                LogUtil.log(TAG, "自定义推流地址:" + PreferenceUtils.getInstance().getCustomStreamUrl());
                LiveStreamSettings.Builder streamSettingBuilder = new LiveStreamSettings.Builder();
                LiveStreamSettings streamSettings = streamSettingBuilder.setLiveStreamType(LiveStreamType.RTMP)
                        .setRtmpSettings(new RtmpSettings.Builder().setUrl(PreferenceUtils.getInstance().getCustomStreamUrl()
                        ).build()).build();
                liveStreamManager.setLiveStreamSettings(streamSettings);
                CameraType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraType, 0));
                if (value != null && (value == CameraType.ZENMUSE_H20T ||
                        value == CameraType.ZENMUSE_H20N || value == CameraType.ZENMUSE_H20)
                        || value == CameraType.ZENMUSE_H30 || value == CameraType.ZENMUSE_H30T) {
                    liveStreamManager.setCameraIndex(ComponentIndexType.LEFT_OR_MAIN);
                } else {
                    liveStreamManager.setCameraIndex(ComponentIndexType.FPV);
                }
                liveStreamManager.setLiveStreamQuality(StreamQuality.FULL_HD);
                liveStreamManager.setLiveVideoBitrateMode(LiveVideoBitrateMode.AUTO);
                if (!liveStreamManager.isStreaming()) {
                    liveStreamManager.startStream(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "自定义推流启动成功:" + PreferenceUtils.getInstance().getCustomStreamUrl());
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "自定义推流启动失败:" + error.description() + "---");
                        }
                    });


                }

            }
        }

    }

}
