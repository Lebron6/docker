package com.aros.apron.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.FileUtil;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;
import com.gosuncn.lib28181agent.bean.AngleEvent;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.List;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.camera.CameraExposureMode;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.CustomExpandNameSettings;
import dji.sdk.keyvalue.value.camera.PhotoIntervalShootSettings;
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode;
import dji.sdk.keyvalue.value.camera.ThermalPIPPosition;
import dji.sdk.keyvalue.value.camera.VideoBitrateMode;
import dji.sdk.keyvalue.value.camera.VideoMimeType;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate;
import dji.sdk.keyvalue.value.camera.ZoomRatiosRange;
import dji.sdk.keyvalue.value.camera.ZoomTargetPointInfo;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.EnCodingType;
import dji.sdk.keyvalue.value.common.RelativePosition;
import dji.sdk.keyvalue.value.gimbal.GimbalAttitudeRange;
import dji.sdk.keyvalue.value.gimbal.GimbalMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class CameraManager extends BaseManager {

    private MqttAndroidClient client;

    private CameraManager() {
    }

    private static class CameraHolder {
        private static final CameraManager INSTANCE = new CameraManager();
    }

    public static CameraManager getInstance() {
        return CameraHolder.INSTANCE;
    }

    public void initCameraInfo(MqttAndroidClient client) {
        this.client = client;
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyConnection, 0));
        if (isConnect != null && isConnect) {

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyCameraMode, 0), this, new CommonCallbacks.KeyListener<CameraMode>() {
                @Override
                public void onValueChange(@Nullable CameraMode oldValue, @Nullable CameraMode newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setCameraMode(newValue.value());
                    }
                }
            });


            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyIsShootingPhoto, 0), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setIsShootingPhoto(newValue ? 1 : 0);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyIsRecording, 0), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setIsRecording(newValue ? 1 : 0);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyRecordingTime, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setRecordingTime(newValue);
                    }
                }
            });
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setCameraZoomRatios(t1);
                    }
                }
            });
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermalZoomRatios(t1);
                    }
                }
            });

            //默认视频源
            CameraVideoStreamSourceType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                    KeyCameraVideoStreamSource));
            if (value!=null){
                Movement.getInstance().setCameraVideoStreamSource(value.value());
            }

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyCameraVideoStreamSource, 0), this, new CommonCallbacks.KeyListener<CameraVideoStreamSourceType>() {
                @Override
                public void onValueChange(@Nullable CameraVideoStreamSourceType cameraVideoStreamSourceType, @Nullable CameraVideoStreamSourceType t1) {
                    if (t1 != null) {
                        Movement.getInstance().setCameraVideoStreamSource(t1.value());
                    }
                }
            });
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<ThermalDisplayMode>() {
                @Override
                public void onValueChange(@Nullable ThermalDisplayMode thermalDisplayMode, @Nullable ThermalDisplayMode t1) {
                    if (t1 != null) {
                        LogUtil.log(TAG,"监听红外模式:"+t1.name());
                        Movement.getInstance().setThermalDisplayMode(t1.value());
                    }
                }
            });
            KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraZoomRatiosRange), new CommonCallbacks.CompletionCallbackWithParam<ZoomRatiosRange>() {
                @Override
                public void onSuccess(ZoomRatiosRange zoomRatiosRange) {
                    if (zoomRatiosRange != null) {
                        Movement.getInstance().setContinuous(zoomRatiosRange.isContinuous());
                        Movement.getInstance().setGears(zoomRatiosRange.getGears());
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {

                }
            });

            KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyThermalZoomRatiosRange), new CommonCallbacks.CompletionCallbackWithParam<ZoomRatiosRange>() {
                @Override
                public void onSuccess(ZoomRatiosRange zoomRatiosRange) {
                    if (zoomRatiosRange != null) {
                        Movement.getInstance().setThermalContinuous(zoomRatiosRange.isContinuous());
                        Movement.getInstance().setThermalGears(zoomRatiosRange.getGears());
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {

                }
            });
        }

        //            获取当前镜头类型，禅思h20t之类的
        Movement.getInstance().setCurCameraType(KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraType)));
//                    获取镜头类型后监听焦距计算视场角
        KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraZoomFocalLength,
                ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
            @Override
            public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                if (t1 != null) {
                    //测试视场角
                    AngleEvent angleEvent = null;
                    if (Movement.getInstance().getCameraTypeParameter().containsKey(Movement.getInstance().getCurCameraType())) {
                        angleEvent = FileUtil.getInstance().countCmos(
                                Movement.getInstance().getCameraTypeParameter().get(Movement.getInstance().getCurCameraType())[0],
                                Movement.getInstance().getCameraTypeParameter().get(Movement.getInstance().getCurCameraType())[1],
                                // TODO: 2024/9/27 计算视场角： 焦距采用广角镜头的焦距乘当前倍率
                                Movement.getInstance().getCameraZoomRatios()
                                        * Movement.getInstance().getCameraTypeParameter().get(Movement.getInstance().getCurCameraType())[2]);
                    } else {
                        LogUtil.log(TAG, "未被记录镜头参数的相机类型，无法计算视场角");
                    }
                    if (angleEvent != null) {
                        Movement.getInstance().setAngleH(angleEvent.getAngleH());
                        Movement.getInstance().setAngleV(angleEvent.getAngleV());
                        LogUtil.log(TAG, "视场角水平垂直和焦距:" + angleEvent.getAngleH() + "==" + angleEvent.getAngleV() + "---" + t1);
                    }
                    Movement.getInstance().setFocalLenght(t1);
                }
            }
        });
        //        获取云台可变动的范围
        KeyManager.getInstance().listen(KeyTools.createKey(GimbalKey.KeyGimbalAttitudeRange), this, new CommonCallbacks.KeyListener<GimbalAttitudeRange>() {
            @Override
            public void onValueChange(@Nullable GimbalAttitudeRange gimbalAttitudeRange, @Nullable GimbalAttitudeRange t1) {
                if (t1 != null) {
                    Movement.getInstance().setGimbalPitchRange(t1.getPitch());
                    Movement.getInstance().setGimbalYawRange(t1.getYaw());
                }
            }
        });
        //        获取镜头分辨率范围
        KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyVideoResolutionFrameRateRange), new CommonCallbacks.CompletionCallbackWithParam<List<VideoResolutionFrameRate>>() {
            @Override
            public void onSuccess(List<VideoResolutionFrameRate> videoResolutionFrameRates) {
                LogUtil.log(TAG, "获取分辨率为：" + videoResolutionFrameRates);
                Movement.getInstance().setKeyVideoResolutionFrameRateRange(videoResolutionFrameRates);
                setBitRate();//设置分辨率

            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "获取分辨率失败：" + idjiError);
            }
        });
//        获取相机编码格式
        KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyVideoMimeType), new CommonCallbacks.CompletionCallbackWithParam<VideoMimeType>() {
            @Override
            public void onSuccess(VideoMimeType videoMimeType) {
                LogUtil.log(TAG, "获取相机支持的编码格式为：" + videoMimeType);
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "获取相机支持的编码格式失败：" + idjiError);
            }
        });
    }
    public void setBitRate() {
        //        设置录像模式
        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraMode), CameraMode.VIDEO_NORMAL, null);
//        设置镜头分辨率，降低分辨率
        List<VideoResolutionFrameRate> VList = Movement.getInstance().getKeyVideoResolutionFrameRateRange();
        LogUtil.log(TAG, VList + "==============");
//
        VideoResolutionFrameRate v = VList.get(0);
        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyVideoResolutionFrameRate), v, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "设置镜头分辨率和帧率成功：" + v);
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "设置镜头分辨率和帧率失败：" + idjiError);
            }
        });
//        设置码率,降低码率
        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyVideoBitrateMode), VideoBitrateMode.VBR, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "相机码率设置VBR成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "相机码率设置VBR失败");
            }
        });
    }
    public void gimbalFollow(){
        // 设置无人机的云台跟踪模式
        KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyGimbalMode,ComponentIndexType.LEFT_OR_MAIN), GimbalMode.FPV, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG,"云台设置跟踪模式成功: ");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG,"云台设置跟踪模式失败: " + error.description());
            }
        });
    }

    public void setCameraZoom(double v1){
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (!isConnect){
            LogUtil.log(TAG,"相机未连接，无法变焦");
        }
        //        监听焦距前设置相机视频源为变焦
        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource), CameraVideoStreamSourceType.ZOOM_CAMERA, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "监听焦距前设置相机视频源--成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "监听焦距前设置相机视频源--失败：" + idjiError);
            }
        });
        //        变焦
        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                        ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), v1,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "放缩成功");
                    }
                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG, "放缩失败" + idjiError);
                    }
                });
    }
//    private void publishCamera2Server() {
//        if (isFlyClickTime()) {
//            MqttMessage flightMessage = null;
//            try {
//                CameraStateEntity.getInstance().setTimeStamp(String.valueOf(System.currentTimeMillis()));
//                flightMessage = new MqttMessage(new Gson().toJson(CameraStateEntity.getInstance()).getBytes("UTF-8"));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            flightMessage.setQos(0);
//            publish(client, MqttConfig.MQTT_CAMERA_TOPIC, flightMessage);
//        }
//    }

    //设置手动对焦值
    public void setCameraFocusRingValue(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingValue, ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), message.getCameraFocusRingValue(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }
                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        sendMsg2Server(mqttAndroidClient, message, "设置对焦值失败:" + new Gson().toJson(idjiError));
                    }
                });
            } else {
                sendMsg2Server(mqttAndroidClient, message, "参数有误");
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }
    //切换相机拍照录像模式
    public void setCameraMode(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                int cameraMode = message.getCameraMode();
                KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraMode), CameraMode.find(cameraMode), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "切换失败:" + new Gson().toJson(error));
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //设置拍照模式
    public void setPhotoIntervalShootSettings(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            PhotoIntervalShootSettings shootSettings = new PhotoIntervalShootSettings();
            shootSettings.setInterval(message.getShootInterval());
            shootSettings.setCount(message.getShootCount());
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyPhotoIntervalShootSettings), shootSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    sendMsg2Server(mqttAndroidClient, message, "设置连拍配置失败:" + new Gson().toJson(idjiError));

                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }


    //开始拍照
    public void startShootPhoto(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyStartShootPhoto), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "拍照失败:" + error.description());
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }


    //结束拍照
    public void stopShootPhoto(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyStopShootPhoto), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "停止拍照失败:" + new Gson().toJson(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //开始录像
    public void startRecordVideo(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyStartRecord), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "开始录像失败:" + new Gson().toJson(error));
                    LogUtil.log(TAG, "开始录像失败:" + new Gson().toJson(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //
    //停止录像
    public void stopRecordVideo(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyStopRecord), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "停止录像失败:" + new Gson().toJson(error));
                    LogUtil.log(TAG, "停止录像失败:" +  new Gson().toJson(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //设置变焦倍率
    public void setCameraZoomRatios(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                int cameraZoomRatios = message.getCameraZoomRatios();
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios, ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), Double.valueOf(cameraZoomRatios), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "设置变焦倍率失败:" + error.description());
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //设置红外变焦倍率(支持1x、2x、4x、8x变焦倍率)
    public void setThermalZoomRatios(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                int type = message.getThermalZoomRatios();
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios, ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), Double.valueOf(type), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "设置红外变焦倍率失败:" + error.description());
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //切换广角变焦红外
    public void setCameraVideoStreamSource(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                int type = message.getCameraVideoStreamSource();

                KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraVideoStreamSource), CameraVideoStreamSourceType.find(type), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "切换失败:" + error.description());
                    }
                });
                if (type == 3) {
                    KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
                                    ComponentIndexType.LEFT_OR_MAIN,
                                    CameraLensType.CAMERA_LENS_THERMAL),
                            ThermalDisplayMode.THERMAL_ONLY, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
//                                    setThermalPIPPosition(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    sendMsg2Server(mqttAndroidClient, message, "红外镜头的显示模式模式设置失败:" + error.description());
                                }
                            });
                }
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }

    }

    //设置红外镜头的显示模式
    public void setThermalDisplayMode(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
                            ComponentIndexType.LEFT_OR_MAIN,
                            CameraLensType.CAMERA_LENS_THERMAL),
                    ThermalDisplayMode.PIP, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            setThermalPIPPosition(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendMsg2Server(mqttAndroidClient, message, "红外镜头的显示模式模式设置失败:" + error.description());
                        }
                    });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //设置红外镜头分屏显示位置
    public void setThermalPIPPosition(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalPIPPosition,
                            ComponentIndexType.LEFT_OR_MAIN,
                            CameraLensType.CAMERA_LENS_THERMAL),
                    ThermalPIPPosition.SIDE_BY_SIDE,
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMsg2Server(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendMsg2Server(mqttAndroidClient, message, "分屏的显示位置设置失败:" + error.description());
                        }
                    });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

//
//
//设置对焦模式
public void setCameraFocusMode(MqttAndroidClient mqttAndroidClient, MQMessage message) {
    Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
            KeyConnection));
    if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
        if (message != null) {
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode, ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), CameraFocusMode.find(message.getCameraFocusMode()), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "设置对焦模式失败:" +new Gson().toJson(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "设置对焦模式失败:参数有误");
        }
    } else {
        sendMsg2Server(mqttAndroidClient, message, "相机未连接");
    }
}
//
    //格式化SD卡
    public void formatStorage(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyFormatStorage), CameraStorageLocation.SDCARD, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    if (mqttAndroidClient!=null&&message!=null){
                        sendMsg2Server(mqttAndroidClient, message);
                    }
                    LogUtil.log(TAG,"sd卡已格式化");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (mqttAndroidClient!=null&&message!=null){
                        sendMsg2Server(mqttAndroidClient, message, "格式化失败:" +new Gson().toJson(error));
                    }
                    LogUtil.log(TAG,"sd卡格式化失败:"+new Gson().toJson(error));
                }
            });
        } else {
            if (mqttAndroidClient!=null&&message!=null){
                sendMsg2Server(mqttAndroidClient, message, "相机未连接");
            }
            LogUtil.log(TAG,"相机未连接");

        }

    }
//

    //设置曝光模式
    public void setExposureMode(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureMode), CameraExposureMode.find(message.getCameraExposureMode()), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "曝光模式切换成功");
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "切换曝光模式失败:" + new Gson().toJson(idjiError));
                    sendMsg2Server(mqttAndroidClient, message, "切换曝光模式失败:" + new Gson().toJson(idjiError));
                }
            });

        } else {
            LogUtil.log(TAG, "切换曝光失败：相机未连接");
        }

    }

    //设置曝光补偿数值
    public void setExposureCompensation(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureCompensation), CameraExposureCompensation.find(message.getCameraExposureCompensation()), new CommonCallbacks.CompletionCallback() {

                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "设置曝光补偿数值成功");
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "设置曝光补偿数值失败:" + new Gson().toJson(idjiError));
                    sendMsg2Server(mqttAndroidClient, message, "设置曝光补偿数值失败:" + new Gson().toJson(idjiError));
                }
            });

        } else {
            LogUtil.log(TAG, "设置曝光补偿数值失败:相机未连接");
        }
    }
//重置相机参数
public void resetCameraSetting(MqttAndroidClient mqttAndroidClient, MQMessage message) {
    Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
            KeyConnection));
    if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
        KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyResetCameraSetting), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                sendMsg2Server(mqttAndroidClient, message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendMsg2Server(mqttAndroidClient, message, "重置相机参数失败:" + new Gson().toJson(error));
            }
        });
    } else {
        sendMsg2Server(mqttAndroidClient, message, "相机未连接");
    }
}

    //指点对焦
    public void tapZoomAtTarget(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {

            ZoomTargetPointInfo zoomPointTargetMsg = new ZoomTargetPointInfo();
            zoomPointTargetMsg.setX(message.getZoomTargetX());
            zoomPointTargetMsg.setX(message.getZoomTargetY());
            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyTapZoomAtTarget),zoomPointTargetMsg, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "指点对焦失败:" + new Gson().toJson(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }


    //切换为广角镜头，降低曝光率

    /**
     * 御3T曝光ISO范围是 100-25600
     * 配合调整快门速度
     * 设置镜头曝光补偿
     */
    public void resumeLensToWideISOManual() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            //切换成广角镜头
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraVideoStreamSource), CameraVideoStreamSourceType.WIDE_CAMERA, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "降落时将镜头切为广角");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "降落切换广角失败：" + new Gson().toJson(error));
                }
            });

        } else {
            LogUtil.log(TAG, "降落切换广角失败：相机未连接");
        }
    }

    //设置自定义文件后缀
    public void setCustomExpandNameSetting() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            CustomExpandNameSettings customExpandNameSettings = new CustomExpandNameSettings();
            customExpandNameSettings.setEncodingType(EnCodingType.UTF8);
            customExpandNameSettings.setForceCreateFolder(false);
            customExpandNameSettings.setRelativePosition(RelativePosition.POSITION_END);
            customExpandNameSettings.setPriority(0);
            customExpandNameSettings.setCustomContent("flightId111" + PreferenceUtils.getInstance().getFlightId());
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCustomExpandFileNameSettings), customExpandNameSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "设置文件后缀success");
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "设置自定义文件后缀失败：" + new Gson().toJson(idjiError));
                }
            });
        } else {
            LogUtil.log(TAG, "设置自定义文件后缀失败：相机未连接");
        }
    }

    //切换为广角镜头，恢复曝光
    public void resumeLensToWideISOProgram() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureMode), CameraExposureMode.PROGRAM, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "降落后切换曝光模式为自动成功");

                    KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureCompensation), CameraExposureCompensation.POS_1P0EV, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "降落后设置曝光补偿数值成功");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            LogUtil.log(TAG, "降落后设置曝光补偿数值失败");
                        }
                    });

                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "降落后切换曝光模式为自动失败:" + new Gson().toJson(idjiError));
                }
            });


        } else {
            LogUtil.log(TAG, "降落后降落完成切换曝光失败：相机未连接");
        }

    }
}
