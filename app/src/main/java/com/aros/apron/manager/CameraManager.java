package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.camera.CameraExposureMode;
import dji.sdk.keyvalue.value.camera.CameraFlatMode;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.CustomExpandNameSettings;
import dji.sdk.keyvalue.value.camera.PhotoIntervalShootSettings;
import dji.sdk.keyvalue.value.camera.ThermalAreaMetersureTemperature;
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode;
import dji.sdk.keyvalue.value.camera.ThermalPIPPosition;
import dji.sdk.keyvalue.value.camera.ThermalTemperatureMeasureMode;
import dji.sdk.keyvalue.value.camera.ZoomRatiosRange;
import dji.sdk.keyvalue.value.camera.ZoomTargetPointInfo;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoublePoint2D;
import dji.sdk.keyvalue.value.common.DoubleRect;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.EnCodingType;
import dji.sdk.keyvalue.value.common.RelativePosition;
import dji.sdk.keyvalue.value.product.ProductType;
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
            ProductType productType = KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType));
            if (productType != null) {
                if (productType == ProductType.M300_RTK) {
                    KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                            KeyCameraFlatMode, 0), this, new CommonCallbacks.KeyListener<CameraFlatMode>() {
                        @Override
                        public void onValueChange(@Nullable CameraFlatMode cameraFlatMode, @Nullable CameraFlatMode t1) {
                            if (t1 != null) {
                                switch (t1.value()) {
                                    case 5:
                                        Movement.getInstance().setCameraMode(0);
                                        break;
                                    case 1:
                                        Movement.getInstance().setCameraMode(1);
                                        break;
                                    case 8:
                                        Movement.getInstance().setCameraMode(8);
                                        break;
                                    case 12:
                                        Movement.getInstance().setCameraMode(12);
                                        break;
                                    default:
                                        Movement.getInstance().setCameraMode(t1.value());
                                        break;
                                }
                            }
                        }
                    });
                } else {
                    KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                            KeyCameraMode, 0), this, new CommonCallbacks.KeyListener<CameraMode>() {
                        @Override
                        public void onValueChange(@Nullable CameraMode oldValue, @Nullable CameraMode newValue) {
                            if (newValue != null) {
                                Movement.getInstance().setCameraMode(newValue.value());
                            }
                        }
                    });
                }
            }

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyPhotoIntervalShootSettings, 0), this, new CommonCallbacks.KeyListener<PhotoIntervalShootSettings>() {
                @Override
                public void onValueChange(@Nullable PhotoIntervalShootSettings photoIntervalShootSettings, @Nullable PhotoIntervalShootSettings t1) {
                    if (t1 != null) {
                        Movement.getInstance().setPhotoInterval(t1.getInterval());
                        Movement.getInstance().setPhotoIntervalCount(t1.getCount());
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyPhotoPanoramaProgress, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1!=null){
                        Movement.getInstance().setPhotoPanoramaProgress(t1);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyIsShootingPhotoPanorama, 0), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1!=null){
                        Movement.getInstance().setShootingPhotoPanorama(t1);
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

            //当前测温模式
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalTemperatureMeasureMode,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<ThermalTemperatureMeasureMode>() {
                @Override
                public void onValueChange(@Nullable ThermalTemperatureMeasureMode thermalTemperatureMeasureMode, @Nullable ThermalTemperatureMeasureMode t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermalTemperatureMeasureMode(t1.value());
                    }
                }
            });

            //获取当前测温点的温度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalSpotMetersureTemperature,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setSpotMetersureTemperature(t1.toString());
                    }

                }
            });

            //获取当前测温区域的温度信息。包括测温区域的平均温度、最小温度和最大温度。
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalRegionMetersureTemperature,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<ThermalAreaMetersureTemperature>() {
                @Override
                public void onValueChange(@Nullable ThermalAreaMetersureTemperature thermalAreaMetersureTemperature,
                                          @Nullable ThermalAreaMetersureTemperature t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAverageAreaTemperature(t1.getAverageAreaTemperature().toString());
                        Movement.getInstance().setMinAreaTemperature(t1.getMinAreaTemperature().toString());
                        Movement.getInstance().setMaxAreaTemperature(t1.getMaxAreaTemperature().toString());
                        Movement.getInstance().setMinTemperaturePointX(t1.getMinTemperaturePoint().getX().toString());
                        Movement.getInstance().setMinTemperaturePointY(t1.getMinTemperaturePoint().getY().toString());
                        Movement.getInstance().setMaxTemperaturePointX(t1.getMaxTemperaturePoint().getX().toString());
                        Movement.getInstance().setMaxTemperaturePointY(t1.getMaxTemperaturePoint().getY().toString());
                    }
                }
            });
        }
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
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingValue,
                                ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM),
                        message.getCameraFocusRingValue(), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                sendMsg2Server(mqttAndroidClient, message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG,"设置对焦值失败:"+new Gson().toJson(error));
                                sendMsg2Server(mqttAndroidClient, message, "设置对焦值失败:" + getIDJIErrorMsg(error));
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
                ProductType productType = KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType));
                if (productType!=null){
                    if (productType==ProductType.M300_RTK){
                        if (cameraMode==0){
                            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraFlatMode), CameraFlatMode.PHOTO_NORMAL, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    sendMsg2Server(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    LogUtil.log(TAG, "相机模式切换拍照失败:" + new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "相机模式切换拍照失败:" + getIDJIErrorMsg(error));
                                }
                            });
                        } else if (cameraMode == 1) {
                            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraFlatMode), CameraFlatMode.VIDEO_NORMAL, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    sendMsg2Server(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    LogUtil.log(TAG, "相机模式切换录像失败:" + new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "相机模式切换录像失败:" + getIDJIErrorMsg(error));
                                }
                            });
                        } else if (cameraMode == 8) {
                            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraFlatMode), CameraFlatMode.PHOTO_INTERVAL, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    sendMsg2Server(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    LogUtil.log(TAG, "相机模式切换定时拍照失败:" + new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "相机模式切换定时拍照失败:" + getIDJIErrorMsg(error));
                                }
                            });
                        } else if (cameraMode == 12) {
                            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraFlatMode), CameraFlatMode.PHOTO_PANO, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    sendMsg2Server(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    LogUtil.log(TAG, "相机模式切换全景拍照失败:" + new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "相机模式切换全景拍照失败:" + getIDJIErrorMsg(error));
                                }
                            });
                        } else {
                            LogUtil.log(TAG, "相机模式切换失败:暂不支持" + cameraMode);
                            sendMsg2Server(mqttAndroidClient, message, "相机模式切换失败:暂不支持");
                        }

                    }else{
                        KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCameraMode), CameraMode.find(cameraMode), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                sendMsg2Server(mqttAndroidClient, message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG,"相机模式切换失败:"+new Gson().toJson(error));
                                sendMsg2Server(mqttAndroidClient, message, "相机模式切换失败:" + getIDJIErrorMsg(error));                            }
                        });

                    }
                }else{
                    sendMsg2Server(mqttAndroidClient, message, "切换失败:相机未连接");

                }

            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "相机未连接");
        }
    }

    //设置定时拍照参数
    public void startTakePhotoWithInterval(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            PhotoIntervalShootSettings shootSettings = new PhotoIntervalShootSettings();
            shootSettings.setInterval(message.getShootInterval());
            shootSettings.setCount(message.getShootCount());
            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyPhotoIntervalShootSettings), shootSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            KeyManager.getInstance().performAction(DJIKey.create(CameraKey.KeyStartShootPhoto), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                                @Override
                                public void onSuccess(EmptyMsg emptyMsg) {
                                    sendMsg2Server(mqttAndroidClient, message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    LogUtil.log(TAG, "定时拍照失败:" + new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "定时拍照失败:" + getIDJIErrorMsg(error));
                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "设置定时拍照参数失败:" + new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "设置定时拍照参数失败:" + getIDJIErrorMsg(error));

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
                    LogUtil.log(TAG,"拍照失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "拍照失败:" + getIDJIErrorMsg(error));
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
                    LogUtil.log(TAG,"停止拍照失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "停止拍照失败:" + getIDJIErrorMsg(error));
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
                    LogUtil.log(TAG,"开始录像失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "开始录像失败:" + getIDJIErrorMsg(error));
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
                    LogUtil.log(TAG,"停止录像失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "停止录像失败:" + getIDJIErrorMsg(error));
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
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                        ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), Double.valueOf(cameraZoomRatios), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG,"设置变焦倍率失败:"+new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "设置变焦倍率失败:" + getIDJIErrorMsg(error));
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
                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios,
                        ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), Double.valueOf(type), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG,"设置红外变焦倍率失败:"+new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "设置红外变焦倍率失败:" + getIDJIErrorMsg(error));
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
                        LogUtil.log(TAG,"切换相机视频流失败:"+new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "切换相机视频流失败:" + getIDJIErrorMsg(error));
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
                                    LogUtil.log(TAG,"红外镜头的显示模式设置失败:"+new Gson().toJson(error));
                                    sendMsg2Server(mqttAndroidClient, message, "红外镜头的显示模式设置失败:" + getIDJIErrorMsg(error));
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
                            LogUtil.log(TAG,"红外镜头的显示模式设置失败:"+new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "红外镜头的显示模式设置失败:" + getIDJIErrorMsg(error));
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
                            LogUtil.log(TAG,"分屏的显示位置设置失败:"+new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "分屏的显示位置设置失败:" + getIDJIErrorMsg(error));
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
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,
                            ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM),
                    CameraFocusMode.find(message.getCameraFocusMode()), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMsg2Server(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG,"设置对焦模式失败:"+new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "设置对焦模式失败:" + getIDJIErrorMsg(error));
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
                        sendMsg2Server(mqttAndroidClient, message, "SD卡格式化失败:" + getIDJIErrorMsg(error));
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
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyExposureMode,
                            ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM),
                    CameraExposureMode.find(message.getCameraExposureMode()), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "曝光模式切换成功");
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"切换曝光模式失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "切换曝光模式失败:" + getIDJIErrorMsg(error));
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
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation,
                            ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM),
                    CameraExposureCompensation.find(message.getCameraExposureCompensation()), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMsg2Server(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG,"设置曝光补偿数值失败:"+new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "设置曝光补偿数值失败:" + getIDJIErrorMsg(error));
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
                LogUtil.log(TAG,"重置相机参数失败:"+new Gson().toJson(error));
                sendMsg2Server(mqttAndroidClient, message, "重置相机参数失败:" + getIDJIErrorMsg(error));
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
            //默认视频源
            CameraVideoStreamSourceType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                    KeyCameraVideoStreamSource));
            int cameraLensType=0;
            switch (value.value()){
                case 0:
                    cameraLensType=1;//0默认，这里当作广角处理
                    break;
                case 1:
                    cameraLensType=1;//1广角，这里当作广角处理
                    break;
                case 2:
                    cameraLensType=0;//视频源是变焦，对应镜头0
                    break;
                case 3:
                    cameraLensType=2;//视频源是红外，对应镜头2
                    break;
            }
            ZoomTargetPointInfo zoomTargetPointInfo=new ZoomTargetPointInfo();
            zoomTargetPointInfo.setX(message.getZoomTargetX());
            zoomTargetPointInfo.setY(message.getZoomTargetY());
            KeyManager.getInstance().performAction(KeyTools.createCameraKey(CameraKey.KeyTapZoomAtTarget,
                    ComponentIndexType.LEFT_OR_MAIN,
                    CameraLensType.find(cameraLensType)),zoomTargetPointInfo, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                    if (value.value()==2){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                                        ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_ZOOM), message.getZoom(), new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        sendMsg2Server(mqttAndroidClient, message);
                                    }

                                    @Override
                                    public void onFailure(@NonNull IDJIError error) {
                                        LogUtil.log(TAG,"指点变焦失败:"+new Gson().toJson(error));
                                        sendMsg2Server(mqttAndroidClient, message, "指点变焦失败:" + getIDJIErrorMsg(error));
                                    }
                                });
                            }
                        },300);
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"指点对焦失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "指点对焦失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            LogUtil.log(TAG, "指点对焦失败：相机未连接");
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
                    LogUtil.log(TAG, "返航时将镜头切为广角");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "返航切换广角失败：" + new Gson().toJson(error));
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

            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureMode),
                    CameraExposureMode.PROGRAM, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "降落后切换曝光模式为自动成功");

                    KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureCompensation),
                            CameraExposureCompensation.POS_1P0EV, new CommonCallbacks.CompletionCallback() {
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

    //设置测温模式
    public void setThermalTemperatureMeasureMode(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalTemperatureMeasureMode,
                            ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL),
                    ThermalTemperatureMeasureMode.find(message.getThermalTemperatureMeasureMode()), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMsg2Server(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG,"设置测温模式:"+new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "设置测温模式:" + getIDJIErrorMsg(error));
                        }
                    });
        } else {
            LogUtil.log(TAG, "设置测温模式失败：相机未连接");
        }
    }

    //设置需要测温的点的位置
    public void setThermalSpotMetersurePoint(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            DoublePoint2D doublePoint2D = new DoublePoint2D();
            doublePoint2D.setX(Double.parseDouble(message.getMetersurePointX()));
            doublePoint2D.setY(Double.parseDouble(message.getMetersurePointY()));
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalSpotMetersurePoint,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), doublePoint2D, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"设置点测温失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "设置点测温失败:" + getIDJIErrorMsg(error));

                }
            });

        } else {
            LogUtil.log(TAG, "测温点设置失败：相机未连接");
        }
    }

    //设置需要测温的区域位置
    public void setThermalRegionMetersureArea(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            DoubleRect doubleRect = new DoubleRect();
            doubleRect.setX(Double.parseDouble(message.getMetersureAreaX()));
            doubleRect.setY(Double.parseDouble(message.getMetersureAreaY()));
            doubleRect.setHeight(Double.parseDouble(message.getMetersureAreaHeight()));
            doubleRect.setWidth(Double.parseDouble(message.getMetersureAreaWidth()));
            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalRegionMetersureArea,
                    ComponentIndexType.LEFT_OR_MAIN, CameraLensType.CAMERA_LENS_THERMAL), doubleRect, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"设置区域测温失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "设置区域测温失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            LogUtil.log(TAG, "测温区域设置失败：相机未连接");
        }
    }


}
