package com.jby.apron.manager;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jby.apron.base.BaseManager;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

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
import dji.v5.manager.datacenter.livestream.LiveStreamSettings;
import dji.v5.manager.datacenter.livestream.LiveStreamStatus;
import dji.v5.manager.datacenter.livestream.LiveStreamStatusListener;
import dji.v5.manager.datacenter.livestream.LiveStreamType;
import dji.v5.manager.datacenter.livestream.LiveVideoBitrateMode;
import dji.v5.manager.datacenter.livestream.StreamQuality;
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings;
import dji.v5.manager.datacenter.livestream.settings.RtspSettings;
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


    public void initStreamManager() {
        ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
        if (liveStreamManager != null) {
            liveStreamManager.addLiveStreamStatusListener(new LiveStreamStatusListener() {
                @Override
                public void onLiveStreamStatusUpdate(LiveStreamStatus status) {
                    if (status != null) {
                        Movement.getInstance().setLiveStatus(status.isStreaming() ? 1 : 0);
                            Log.d(TAG, "推流状态"+status.isStreaming()+"帧率:" + status.getFps() + "--" + "码率:" + status.getVbps()+"---"+"延迟:"+status.getRtt());
                    }
                }

                @Override
                public void onError(IDJIError error) {

                }
            });
        }
    }

    public void startLive(MessageDown message) {
sendMsg2Server(message);

    }

    public void setLiveStreamQuality(MessageDown message) {
        Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
        if (isAircraftConnected == null || !isAircraftConnected) {
            LogUtil.log(TAG, "飞行器未连接");
            sendFailMsg2Server(message, "飞行器未连接");
        } else {
            ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
            if (liveStreamManager.isStreaming()) {
                if (message.getData().getVideo_quality()==0||message.getData().getVideo_quality()==4){
                    liveStreamManager.setLiveStreamQuality(StreamQuality.ORIGINAL);
                }else {
                    liveStreamManager.setLiveStreamQuality(StreamQuality.find(message.getData().getVideo_quality()));
                }
                sendMsg2Server(message);
            } else {
                sendFailMsg2Server(message, "推流未开启");
            }
        }
    }


    private int startLiveFailTimes;
    private boolean isLiveStreamAlreadyStart;

    //知眸测试
    public void startLiveWithCustom() {

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
            CameraType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraType, ComponentIndexType.PORT_1));
//            if (value != null && (value == CameraType.ZENMUSE_H20T ||
//                    value == CameraType.ZENMUSE_H20N || value == CameraType.ZENMUSE_H20)
//                    || value == CameraType.ZENMUSE_H30 || value == CameraType.ZENMUSE_H30T) {
                liveStreamManager.setCameraIndex(ComponentIndexType.PORT_1);
//            } else {
//                liveStreamManager.setCameraIndex(ComponentIndexType.FPV);
//            }
            liveStreamManager.setLiveStreamQuality(StreamQuality.FULL_HD);
            liveStreamManager.setLiveVideoBitrateMode(LiveVideoBitrateMode.AUTO);
            if (!liveStreamManager.isStreaming()) {
                liveStreamManager.startStream(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "自定义推流启动成功");
                        isLiveStreamAlreadyStart=true;
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "第"+startLiveFailTimes+"次开始推流失败:"+new Gson().toJson(error));
                        if (!isLiveStreamAlreadyStart){
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (startLiveFailTimes < 10) {
                                        startLiveFailTimes++;
                                        startLiveWithCustom();
                                    }
                                }
                            }, 3000);
                        }
                    }
                });
            }
        }
    }


    public void startLiveWithRTSP() {

        Boolean isAircraftConnected = KeyManager.getInstance().getValue(DJIKey.create(ProductKey.KeyConnection));
        if (isAircraftConnected == null || !isAircraftConnected) {
            LogUtil.log(TAG, "飞行器未连接");

        } else {
            if (PreferenceUtils.getInstance().getRtspUserName()!=null&&
                    PreferenceUtils.getInstance().getRtspPort()!=null&&
            PreferenceUtils.getInstance().getRtspPassWord()!=null
            ){
                ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
                LogUtil.log(TAG, "自定义RTSP推流:" + PreferenceUtils.getInstance().getRtspUserName()
                        +"--"+PreferenceUtils.getInstance().getRtspPort()+"--"+PreferenceUtils.getInstance().getRtspPassWord());
                LiveStreamSettings.Builder streamSettingBuilder = new LiveStreamSettings.Builder();
                LiveStreamSettings streamSettings = streamSettingBuilder.setLiveStreamType(LiveStreamType.RTSP)
                        .setRtspSettings(new RtspSettings.Builder().setPassWord(PreferenceUtils.getInstance().getRtspPassWord()).
                                setPort(Integer.parseInt(PreferenceUtils.getInstance().getRtspPort())).
                                setUserName(PreferenceUtils.getInstance().getRtspUserName()).build()).build();

                liveStreamManager.setLiveStreamSettings(streamSettings);
                CameraType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraType, ComponentIndexType.PORT_1));
//                if (value != null && (value == CameraType.ZENMUSE_H20T ||
//                        value == CameraType.ZENMUSE_H20N || value == CameraType.ZENMUSE_H20)
//                        || value == CameraType.ZENMUSE_H30 || value == CameraType.ZENMUSE_H30T) {
                    liveStreamManager.setCameraIndex(ComponentIndexType.PORT_1);
//                } else {
//                    liveStreamManager.setCameraIndex(ComponentIndexType.FPV);
//                }
                liveStreamManager.setLiveStreamQuality(StreamQuality.FULL_HD);
                liveStreamManager.setLiveVideoBitrateMode(LiveVideoBitrateMode.AUTO);
                if (!liveStreamManager.isStreaming()) {
                    liveStreamManager.startStream(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "自定义RTSP推流启动成功");
                            isLiveStreamAlreadyStart=true;
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "第"+startLiveFailTimes+"次开始RTSP推流失败:"+new Gson().toJson(error));
                            if (!isLiveStreamAlreadyStart){
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (startLiveFailTimes < 10) {
                                            startLiveFailTimes++;
                                            startLiveWithRTSP();
                                        }
                                    }
                                }, 3000);
                            }
                        }
                    });
                }else{
                    liveStreamManager.stopStream(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    liveStreamManager.startStream(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onSuccess() {
                                            LogUtil.log(TAG, "自定义RTSP推流启动成功");
                                            isLiveStreamAlreadyStart=true;
                                        }

                                        @Override
                                        public void onFailure(@NonNull IDJIError error) {
                                            LogUtil.log(TAG, "第"+startLiveFailTimes+"次开始RTSP推流失败:"+new Gson().toJson(error));
                                            if (!isLiveStreamAlreadyStart){
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (startLiveFailTimes < 10) {
                                                            startLiveFailTimes++;
                                                            startLiveWithRTSP();
                                                        }
                                                    }
                                                }, 3000);
                                            }
                                        }
                                    });
                                }
                            },2000);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "第"+startLiveFailTimes+"次开始RTSP推流失败:"+new Gson().toJson(error));
                            if (!isLiveStreamAlreadyStart){
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (startLiveFailTimes < 10) {
                                            startLiveFailTimes++;
                                            startLiveWithRTSP();
                                        }
                                    }
                                }, 3000);
                            }
                        }
                    });
                }
            }else{
                LogUtil.log(TAG,"RTSP配置参数有误");
            }
        }
    }

}
