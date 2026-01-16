package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.camera.CameraExposureMode;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.camera.CameraFocusState;
import dji.sdk.keyvalue.value.camera.CameraISO;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.camera.CameraShutterSpeed;
import dji.sdk.keyvalue.value.camera.CameraStorageInfo;
import dji.sdk.keyvalue.value.camera.CameraStorageInfos;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraThermalPalette;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.camera.LaserMeasureInformation;
import dji.sdk.keyvalue.value.camera.PhotoState;
import dji.sdk.keyvalue.value.camera.RecordingState;
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode;
import dji.sdk.keyvalue.value.camera.ThermalGainMode;
import dji.sdk.keyvalue.value.camera.ThermalTemperatureMeasureMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoublePoint2D;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class CameraManager extends BaseManager {


    private CameraManager() {
    }

    private static class CameraHolder {
        private static final CameraManager INSTANCE = new CameraManager();
    }

    public static CameraManager getInstance() {
        return CameraHolder.INSTANCE;
    }

    public void initCameraInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {

            //全局画面中测量的最高温度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalGlobalMinTemperature,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_global_temperature_min(t1);
                    }
                }
            });
            //激光测距信息
            KeyManager.getInstance().listen(createKey(CameraKey.KeyLaserMeasureInformation, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<LaserMeasureInformation>() {
                @Override
                public void onValueChange(@Nullable LaserMeasureInformation laserMeasureInformation, @Nullable LaserMeasureInformation t1) {
                    if (t1 != null) {
                        if (t1.getLocation3D() != null) {
                            Movement.getInstance().setMeasure_target_altitude(t1.getLocation3D().getAltitude());
                            Movement.getInstance().setMeasure_target_latitude(t1.getLocation3D().getLatitude());
                            Movement.getInstance().setMeasure_target_longitude(t1.getLocation3D().getLongitude());
                            Movement.getInstance().setMeasure_target_distance(t1.getDistance().intValue());
                            Movement.getInstance().setMeasure_target_error_state(t1.getLaserMeasureState().value());
                            Movement.getInstance().setPayload_index("53-0-0");
                        }
                    }
                }
            });

            //调色盘样式。{"0":"白热","1":"黑热","2":"描红","3":"医疗","5":"彩虹 1","6":"铁红","8":"北极","11":"熔岩","12":"热铁","13":"彩虹 2"}
            KeyManager.getInstance().listen(createKey(CameraKey.KeyThermalPalette, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<CameraThermalPalette>() {
                @Override
                public void onValueChange(@Nullable CameraThermalPalette cameraThermalPalette, @Nullable CameraThermalPalette t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_current_palette_style(t1.value());
                    }
                }
            });

            //增益模式。{"0":"自动","1":"低增益, 测温范围0°C-500°C","2":"高增益, 测温范围-20°C-150°C"}
            KeyManager.getInstance().listen(createKey(CameraKey.KeyThermalGainMode, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<ThermalGainMode>() {
                @Override
                public void onValueChange(@Nullable ThermalGainMode thermalGainMode, @Nullable ThermalGainMode t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_gain_mode(t1.value());
                    }
                }
            });

            //全局画面中测量的最高温度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalGlobalMaxTemperature,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_global_temperature_max(t1);
                    }
                }
            });

            //等温线最高值。较低和中等等温线阈值之间的温度值将以调色板中的128-175色显示。
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalIsothermUpperValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_isotherm_upper_limit(t1);
                    }
                }
            });

            //等温线最低值。较低和中等等温线阈值之间的温度值将以调色板中的128-175色显示。
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalIsothermLowerValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_isotherm_lower_limit(t1);
                    }
                }
            });

            //是否开启等温线
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalIsothermEnabled,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setThermal_isotherm_state(t1 ? 1 : 0);
                    }
                }
            });

            //相机模式
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyCameraMode, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<CameraMode>() {
                @Override
                public void onValueChange(@Nullable CameraMode oldValue, @Nullable CameraMode newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setCamera_mode(newValue.value());
                    }
                }
            });

            //当前测温模式
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalTemperatureMeasureMode,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<ThermalTemperatureMeasureMode>() {
                @Override
                public void onValueChange(@Nullable ThermalTemperatureMeasureMode thermalTemperatureMeasureMode, @Nullable ThermalTemperatureMeasureMode t1) {
                    if (t1 != null&&thermalTemperatureMeasureMode!=null) {
                        Movement.getInstance().setIr_metering_mode(thermalTemperatureMeasureMode.value());
                    }
                }
            });

            //当前测温的点的位置
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalSpotMetersurePoint,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<DoublePoint2D>() {
                @Override
                public void onValueChange(@Nullable DoublePoint2D doublePoint2D, @Nullable DoublePoint2D t1) {
                    if (t1 != null) {
                        Movement.getInstance().setX(t1.getX());
                        Movement.getInstance().setX(t1.getY());
                    }
                }
            });

            //当前测温的点的温度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalSpotMetersureTemperature,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTemperature(t1.intValue());
                    }
                }
            });

            //当前红外变焦倍率
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setIr_zoom_factor(t1.intValue());
                    }
                }
            });

            //拍照状态
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.KeyPhotoState,
                    ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<PhotoState>() {
                @Override
                public void onValueChange(@Nullable PhotoState photoState, @Nullable PhotoState t1) {
                    if (t1 != null) {
                        Movement.getInstance().setPhoto_state(t1.value());
                    }
                }
            });


            //视频录制时长
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyRecordingTime, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setRecord_time(newValue);
                    }
                }
            });


            //视频录制状态
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeyRecordingState, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<RecordingState>() {
                @Override
                public void onValueChange(@Nullable RecordingState recordingState, @Nullable RecordingState t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRecording_state(t1.value()==0?0:1);
                    }
                }
            });

            //照片数
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeySDCardAvailablePhotoCount, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemain_photo_num(t1);
                    }
                }
            });

            //剩余录像时间
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeySSDAvailableRecordingTimeInSeconds, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemain_record_duration(t1);
                    }
                }
            });

            //视频录制时长
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.
                    KeySSDAvailableRecordingTimeInSeconds, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemain_record_duration(t1);
                    }
                }
            });

            //分屏是否使能
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), this, new CommonCallbacks.KeyListener<ThermalDisplayMode>() {
                @Override
                public void onValueChange(@Nullable ThermalDisplayMode thermalDisplayMode, @Nullable ThermalDisplayMode t1) {
                    if (t1 != null) {
                        Movement.getInstance().setScreen_split_enable(t1.value() == 2 ? true : false);
                    }
                }
            });

            //广角镜头曝光模式
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyExposureMode,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_WIDE), this, new CommonCallbacks.KeyListener<CameraExposureMode>() {
                @Override
                public void onValueChange(@Nullable CameraExposureMode cameraExposureMode, @Nullable CameraExposureMode t1) {
                    if (t1 != null) {
                        LogUtil.log(TAG, "监听曝光模式:" + t1.name());
                        Movement.getInstance().setWide_exposure_mode(t1.value());
                    }
                }
            });

            //广角镜头曝光值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_WIDE), this, new CommonCallbacks.KeyListener<CameraExposureCompensation>() {
                @Override
                public void onValueChange(@Nullable CameraExposureCompensation cameraExposureCompensation, @Nullable CameraExposureCompensation t1) {
                    if (cameraExposureCompensation != null) {
                        Movement.getInstance().setWide_exposure_value(cameraExposureCompensation.value());
                    }
                }
            });

            //广角镜头感光度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyISO,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_WIDE), this, new CommonCallbacks.KeyListener<CameraISO>() {
                @Override
                public void onValueChange(@Nullable CameraISO cameraISO, @Nullable CameraISO t1) {
                    if (t1 != null) {
                        Movement.getInstance().setWide_iso(t1.value());
                    }
                }
            });

            //广角镜头快门速度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyShutterSpeed,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_WIDE), this, new CommonCallbacks.KeyListener<CameraShutterSpeed>() {
                @Override
                public void onValueChange(@Nullable CameraShutterSpeed cameraShutterSpeed, @Nullable CameraShutterSpeed t1) {
                    if (t1 != null) {
                        Movement.getInstance().setWide_shutter_speed(t1.value());
                    }
                }
            });

            //变焦镜头标定的最远对焦值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingMaxValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_calibrate_farthest_focus_value(t1);
                    }
                }
            });

            //变焦镜头标定的最近对焦值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingMinValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_calibrate_farthest_focus_value(t1);
                    }
                }
            });

            //变焦镜头曝光模式
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyExposureMode,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraExposureMode>() {
                @Override
                public void onValueChange(@Nullable CameraExposureMode cameraExposureMode, @Nullable CameraExposureMode t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_exposure_mode(t1.value());
                    }
                }
            });

            //变焦镜头感光度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraExposureCompensation>() {
                @Override
                public void onValueChange(@Nullable CameraExposureCompensation cameraExposureCompensation, @Nullable CameraExposureCompensation t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_exposure_value(t1.value());
                    }
                }
            });

            //变焦镜头感光度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraExposureCompensation>() {
                @Override
                public void onValueChange(@Nullable CameraExposureCompensation cameraExposureCompensation, @Nullable CameraExposureCompensation t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_exposure_value(t1.value());
                    }
                }
            });

            //变焦倍数
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_factor(t1);
                    }
                }
            });

            //变焦镜头对焦模式
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraFocusMode>() {
                @Override
                public void onValueChange(@Nullable CameraFocusMode cameraFocusMode, @Nullable CameraFocusMode t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_focus_mode(t1.value());
                    }
                }
            });

            //变焦镜头对焦状态
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusState,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraFocusState>() {
                @Override
                public void onValueChange(@Nullable CameraFocusState cameraFocusState, @Nullable CameraFocusState t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_focus_state(t1.value());
                    }
                }
            });

            //变焦镜头对焦值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_focus_value(t1);
                    }
                }
            });

            //变焦镜头感光度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyISO,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraISO>() {
                @Override
                public void onValueChange(@Nullable CameraISO cameraISO, @Nullable CameraISO t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_iso(t1.value());
                    }
                }
            });

            //变焦镜头最大对焦值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingMaxValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_max_focus_value(t1);
                    }
                }
            });

            //变焦镜头最小对焦值
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingMinValue,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_min_focus_value(t1);
                    }
                }
            });

            //变焦镜头快门速度
            KeyManager.getInstance().listen(KeyTools.createCameraKey(CameraKey.KeyShutterSpeed,
                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), this, new CommonCallbacks.KeyListener<CameraShutterSpeed>() {
                @Override
                public void onValueChange(@Nullable CameraShutterSpeed cameraShutterSpeed, @Nullable CameraShutterSpeed t1) {
                    if (t1 != null) {
                        Movement.getInstance().setZoom_shutter_speed(t1.value());
                    }
                }
            });

            //SD卡容量
            KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.KeyCameraStorageInfos, ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<CameraStorageInfos>() {
                @Override
                public void onValueChange(@Nullable CameraStorageInfos cameraStorageInfos, @Nullable CameraStorageInfos t1) {
                    if (t1!=null) {
                        CameraStorageInfo cameraStorageInfoByLocation = t1.getCameraStorageInfoByLocation(CameraStorageLocation.SDCARD);
                        if (cameraStorageInfoByLocation != null) {
                            Movement.getInstance().setTotal(cameraStorageInfoByLocation.getStorageCapacity() * 1024);
                            Movement.getInstance().setUsed((cameraStorageInfoByLocation.getStorageCapacity() * 1024)
                                    - (cameraStorageInfoByLocation.getStorageLeftCapacity()));
                        }
                    }
                }
            });



        }
    }



//    //设置手动对焦值
//    public void setCameraFocusRingValue(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            if (message != null) {
//                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusRingValue,
//                                ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM),
//                        message.getCameraFocusRingValue(), new CommonCallbacks.CompletionCallback() {
//                            @Override
//                            public void onSuccess() {
//                                sendMsg2Server( message);
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull IDJIError error) {
//                                LogUtil.log(TAG,"设置对焦值失败:"+new Gson().toJson(error));
//                                sendMsg2Server( message, "设置对焦值失败:" + getIDJIErrorMsg(error));
//                            }
//                        });
//            } else {
//                sendMsg2Server( message, "参数有误");
//            }
//        } else {
//            sendMsg2Server( message, "当前状态相机禁止操作");
//        }
//    }
//切换相机拍照录像模式
public void setCameraMode(MessageDown message) {
    Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
            KeyConnection, ComponentIndexType.PORT_1));
    if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
        if (message != null) {
            int cameraMode = message.getData().getCamera_mode();
            ProductType productType = KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType));
            if (productType != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraMode, ComponentIndexType.PORT_1),
                        CameraMode.find(cameraMode == 4 ? CameraMode.PHOTO_PANORAMA.value() : cameraMode),
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                sendMsg2Server(message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                sendFailMsg2Server(message, "相机模式切换失败:" + getIDJIErrorMsg(error));
                            }
                        });
            } else {
                sendFailMsg2Server(message, "切换失败:当前状态相机禁止操作");
            }
        }
    } else {
        sendFailMsg2Server(message, "当前状态相机禁止操作");
    }
}

    //
//    //设置定时拍照参数
//    public void startTakePhotoWithInterval(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            PhotoIntervalShootSettings shootSettings = new PhotoIntervalShootSettings();
//            shootSettings.setInterval(message.getShootInterval());
//            shootSettings.setCount(message.getShootCount());
//            KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyPhotoIntervalShootSettings, ComponentIndexType.PORT_1),shootSettings, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyStartShootPhoto,
//                                    ComponentIndexType.PORT_1), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                                @Override
//                                public void onSuccess(EmptyMsg emptyMsg) {
//                                    sendMsg2Server( message);
//                                }
//
//                                @Override
//                                public void onFailure(@NonNull IDJIError error) {
//                                    LogUtil.log(TAG, "定时拍照失败:" + new Gson().toJson(error));
//                                    sendMsg2Server( message, "定时拍照失败:" + getIDJIErrorMsg(error));
//                                }
//                            });
//                        }
//                    }, 500);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG, "设置定时拍照参数失败:" + new Gson().toJson(error));
//                    sendMsg2Server( message, "设置定时拍照参数失败:" + getIDJIErrorMsg(error));
//
//                }
//            });
//        } else {
//            sendMsg2Server( message, "当前状态相机禁止操作");
//        }
//    }
//
//
//开始拍照
    public void startShootPhoto(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyStartShootPhoto,
                            ComponentIndexType.PORT_1),
                    new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "拍照失败:" + new Gson().toJson(error));
                            sendFailMsg2Server(message, "拍照失败:" + getIDJIErrorMsg(error));
                        }
                    });
        } else {
            sendFailMsg2Server(message, "当前状态相机禁止操作");
        }
    }


    //结束拍照
    public void stopShootPhoto(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyStopShootPhoto,
                    ComponentIndexType.PORT_1), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "停止拍照失败:" + new Gson().toJson(error));
                    sendFailMsg2Server(message, "停止拍照失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendFailMsg2Server(message, "当前状态相机禁止操作");
        }
    }

    //开始录像
    public void startRecordVideo(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyStartRecord,
                    ComponentIndexType.PORT_1), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server(message, "开始录像失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendFailMsg2Server(message, "当前状态相机禁止操作");
        }
    }

    //停止录像
    public void stopRecordVideo(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyStopRecord,
                    ComponentIndexType.PORT_1), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    LogUtil.log(TAG, "停止录像成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server(message, "停止录像失败:" + getIDJIErrorMsg(error));
                }
            });
    } else {
        sendFailMsg2Server(message, "当前状态相机禁止操作");
    }
}
//
//
//    //设置变焦倍率
//    public void setCameraZoomRatios(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            if (message != null) {
//                int cameraZoomRatios = message.getCameraZoomRatios();
//                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
//                        ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), Double.valueOf(cameraZoomRatios), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        LogUtil.log(TAG,"设置变焦倍率失败:"+new Gson().toJson(error));
//                        sendMsg2Server(message, "设置变焦倍率失败:" + getIDJIErrorMsg(error));
//                    }
//                });
//            }
//        } else {
//            sendMsg2Server(message, "当前状态相机禁止操作");
//        }
//    }
//
//    //设置红外变焦倍率(支持1x、2x、4x、8x变焦倍率)
//    public void setThermalZoomRatios(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            if (message != null) {
//                int type = message.getThermalZoomRatios();
//                KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios,
//                        ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), Double.valueOf(type),
//                        new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        LogUtil.log(TAG,"设置红外变焦倍率失败:"+new Gson().toJson(error));
//                        sendMsg2Server(message, "设置红外变焦倍率失败:" + getIDJIErrorMsg(error));
//                    }
//                });
//            }
//        } else {
//            sendMsg2Server(message, "当前状态相机禁止操作");
//        }
//    }

    //切换广角变焦红外
    public void setCameraVideoStreamSource(MessageDown message) {
        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection,ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            if (message != null) {
                int type=1;
                switch (message.getData().getVideo_type()){
                    case "ir":
                        type=3;
                        break;
                    case "normal":
                        type=0;
                        break;
                    case "wide":
                        type=1;
                        break;
                    case "zoom":
                        type=2;
                        break;
                }

                KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource,
                                ComponentIndexType.PORT_1)
                        , CameraVideoStreamSourceType.find(type),
                        new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendFailMsg2Server(message, "切换相机视频流失败:" + getIDJIErrorMsg(error));
                    }
                });
//                if (type == 3) {
//                    KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
//                                    ComponentIndexType.PORT_1,
//                                    CameraLensType.CAMERA_LENS_THERMAL),
//                            ThermalDisplayMode.THERMAL_ONLY, new CommonCallbacks.CompletionCallback() {
//                                @Override
//                                public void onSuccess() {
////                                    setThermalPIPPosition(mqttAndroidClient, message);
//                                }
//
//                                @Override
//                                public void onFailure(@NonNull IDJIError error) {
//                                    LogUtil.log(TAG,"红外镜头的显示模式设置失败:"+new Gson().toJson(error));
//                                    sendMsg2Server(message, "红外镜头的显示模式设置失败:" + getIDJIErrorMsg(error));
//                                }
//                            });
//                }
            }
        } else {
            sendFailMsg2Server(message, "当前状态相机禁止操作");
        }

    }

//    //设置红外镜头的显示模式
//    public void setThermalDisplayMode(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalDisplayMode,
//                            ComponentIndexType.PORT_1,
//                            CameraLensType.CAMERA_LENS_THERMAL),
//                    ThermalDisplayMode.PIP, new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            setThermalPIPPosition(message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG,"红外镜头的显示模式设置失败:"+new Gson().toJson(error));
//                            sendMsg2Server(message, "红外镜头的显示模式设置失败:" + getIDJIErrorMsg(error));
//                        }
//                    });
//        } else {
//            sendMsg2Server(message, "当前状态相机禁止操作");
//        }
//    }
//
//    //设置红外镜头分屏显示位置
//    public void setThermalPIPPosition(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalPIPPosition,
//                            ComponentIndexType.PORT_1,
//                            CameraLensType.CAMERA_LENS_THERMAL),
//                    ThermalPIPPosition.SIDE_BY_SIDE,
//                    new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            sendMsg2Server(message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG,"分屏的显示位置设置失败:"+new Gson().toJson(error));
//                            sendMsg2Server(message, "分屏的显示位置设置失败:" + getIDJIErrorMsg(error));
//                        }
//                    });
//        } else {
//            sendMsg2Server(message, "当前状态相机禁止操作");
//        }
//    }
//
//
////设置对焦模式
//public void setCameraFocusMode(MQMessage message) {
//    Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//            KeyConnection,ComponentIndexType.PORT_1));
//    if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//        if (message != null) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,
//                            ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM),
//                    CameraFocusMode.find(message.getCameraFocusMode()), new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            sendMsg2Server(message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG,"设置对焦模式失败:"+new Gson().toJson(error));
//                            sendMsg2Server(message, "设置对焦模式失败:" + getIDJIErrorMsg(error));
//                        }
//                    });
//        } else {
//            sendMsg2Server(message, "设置对焦模式失败:参数有误");
//        }
//    } else {
//        sendMsg2Server(message, "当前状态相机禁止操作");
//    }
//}
//
    //格式化SD卡
    public void formatStorage(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyFormatStorage, ComponentIndexType.PORT_1), CameraStorageLocation.SDCARD, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    if (message != null) {
                        sendMsg2Server(message);
                    }
                    LogUtil.log(TAG, "sd卡已格式化");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (message != null) {
                        sendFailMsg2Server(message, "SD卡格式化失败:" + getIDJIErrorMsg(error));
                    }
                    LogUtil.log(TAG, "sd卡格式化失败:" + new Gson().toJson(error));
                }
            });
        } else {
            if (message != null) {
                sendFailMsg2Server(message, "当前状态相机禁止操作");
            }
            LogUtil.log(TAG, "当前状态相机禁止操作");

        }
//
//    }
////
//
//    //设置曝光模式
//    public void setExposureMode(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyExposureMode,
//                            ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM),
//                    CameraExposureMode.find(message.getCameraExposureMode()), new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    LogUtil.log(TAG, "曝光模式切换成功");
//                    sendMsg2Server(message);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"切换曝光模式失败:"+new Gson().toJson(error));
//                    sendMsg2Server(message, "切换曝光模式失败:" + getIDJIErrorMsg(error));
//                }
//            });
//
//        } else {
//            LogUtil.log(TAG, "切换曝光失败：当前状态相机禁止操作");
//        }
//
//    }
//
//    //设置曝光补偿数值
//    public void setExposureCompensation(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation,
//                            ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM),
//                    CameraExposureCompensation.find(message.getCameraExposureCompensation()), new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            sendMsg2Server(message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG,"设置曝光补偿数值失败:"+new Gson().toJson(error));
//                            sendMsg2Server(message, "设置曝光补偿数值失败:" + getIDJIErrorMsg(error));
//                        }
//                    });
//        } else {
//            LogUtil.log(TAG, "设置曝光补偿数值失败:当前状态相机禁止操作");
//        }
//    }
////重置相机参数
//public void resetCameraSetting(MQMessage message) {
//    Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//            KeyConnection));
//    if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//        KeyManager.getInstance().performAction(KeyTools.createKey(CameraKey.KeyResetCameraSetting,
//                ComponentIndexType.PORT_1), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//            @Override
//            public void onSuccess(EmptyMsg emptyMsg) {
//                sendMsg2Server(message);
//            }
//
//            @Override
//            public void onFailure(@NonNull IDJIError error) {
//                LogUtil.log(TAG,"重置相机参数失败:"+new Gson().toJson(error));
//                sendMsg2Server(message, "重置相机参数失败:" + getIDJIErrorMsg(error));
//            }
//        });
//    } else {
//        sendMsg2Server(message, "当前状态相机禁止操作");
//    }
//}
//
//    //指点对焦
//    public void tapZoomAtTarget(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyTapZoomEnable,
//                            ComponentIndexType.PORT_1,CameraLensType.CAMERA_LENS_ZOOM), true, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    LogUtil.log(TAG,"设置使能指点成功");
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"设置使能指点失败:"+new Gson().toJson(error));
//                    sendMsg2Server(message, "设置使能指点失败:" + getIDJIErrorMsg(error));                            }
//            });
//
//            //默认视频源
//            CameraVideoStreamSourceType value = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                    KeyCameraVideoStreamSource,ComponentIndexType.PORT_1));
//            ZoomTargetPointInfo zoomTargetPointInfo=new ZoomTargetPointInfo();
//            zoomTargetPointInfo.setX(message.getZoomTargetX());
//            zoomTargetPointInfo.setY(message.getZoomTargetY());
//            zoomTargetPointInfo.setTapZoomModeEnable(true);
//            zoomTargetPointInfo.setMode(TapZoomMode.GIMBAL_FOLLOW);
//            KeyManager.getInstance().performAction(KeyTools.createCameraKey(CameraKey.KeyTapZoomAtTarget,
//                    ComponentIndexType.PORT_1,
//                    CameraLensType.CAMERA_LENS_ZOOM),zoomTargetPointInfo, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                @Override
//                public void onSuccess(EmptyMsg emptyMsg) {
//                    sendMsg2Server(message);
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (message.getZoom()!=0||message.getZoom()>0&&value!=null){
//                                switch (value.value()){
//                                    case 0|1:
//
//                                        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource,
//                                                ComponentIndexType.PORT_1), CameraVideoStreamSourceType.ZOOM_CAMERA, new CommonCallbacks.CompletionCallback() {
//                                            @Override
//                                            public void onSuccess() {
//                                                new Handler().postDelayed(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//                                                        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
//                                                                ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), message.getZoom(), new CommonCallbacks.CompletionCallback() {
//                                                            @Override
//                                                            public void onSuccess() {
//                                                                sendMsg2Server(message);
//                                                            }
//
//                                                            @Override
//                                                            public void onFailure(@NonNull IDJIError error) {
//                                                                LogUtil.log(TAG,"指点变焦失败:"+new Gson().toJson(error));
//                                                                sendMsg2Server(message, "指点变焦失败:" + getIDJIErrorMsg(error));
//                                                            }
//                                                        });
//                                                    }
//                                                },300);
//                                            }
//
//                                            @Override
//                                            public void onFailure(@NonNull IDJIError error) {
//                                                LogUtil.log(TAG,"切换相机视频流失败:"+new Gson().toJson(error));
////                                                sendMsg2Server(message, "切换相机视频流失败:" + getIDJIErrorMsg(error));
//                                            }
//                                        });
//                                        break;
//                                    case 2:
//                                        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios,
//                                                ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_ZOOM), message.getZoom(), new CommonCallbacks.CompletionCallback() {
//                                            @Override
//                                            public void onSuccess() {
//                                                sendMsg2Server(message);
//                                            }
//
//                                            @Override
//                                            public void onFailure(@NonNull IDJIError error) {
//                                                LogUtil.log(TAG,"指点变焦失败:"+new Gson().toJson(error));
//                                                sendMsg2Server(message, "指点变焦失败:" + getIDJIErrorMsg(error));
//                                            }
//                                        });
//                                        break;
//                                    case 3:
//                                        KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios,
//                                                ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL),message.getZoom(), new CommonCallbacks.CompletionCallback() {
//                                            @Override
//                                            public void onSuccess() {
//                                                sendMsg2Server(message);
//                                            }
//
//                                            @Override
//                                            public void onFailure(@NonNull IDJIError error) {
//                                                LogUtil.log(TAG,"设置红外变焦倍率失败:"+new Gson().toJson(error));
//                                                sendMsg2Server(message, "设置红外变焦倍率失败:" + getIDJIErrorMsg(error));
//                                            }
//                                        });
//                                        break;
//                                }
//                            }
//
//                        }
//                    },200);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"指点对焦失败:"+new Gson().toJson(error));
//                    sendMsg2Server(message, "指点对焦失败:" + getIDJIErrorMsg(error));
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "指点对焦失败：当前状态相机禁止操作");
//        }
    }


        //切换为广角镜头，降低曝光率
        public void resumeLensToWideISOManual () {
            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
                    KeyConnection, ComponentIndexType.PORT_1));
            if (isConnect != null && isConnect) {
                //切换成广角镜头
                KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource,
                        ComponentIndexType.PORT_1), CameraVideoStreamSourceType.WIDE_CAMERA, new CommonCallbacks.CompletionCallback() {
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
                LogUtil.log(TAG, "降落切换广角失败：当前状态相机禁止操作");
            }
        }

//    //设置自定义文件后缀
//    public void setCustomExpandNameSetting() {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect) {
//            CustomExpandNameSettings customExpandNameSettings = new CustomExpandNameSettings();
//            customExpandNameSettings.setEncodingType(EnCodingType.UTF8);
//            customExpandNameSettings.setForceCreateFolder(false);
//            customExpandNameSettings.setRelativePosition(RelativePosition.POSITION_END);
//            customExpandNameSettings.setPriority(0);
//            customExpandNameSettings.setCustomContent("flightId111" + PreferenceUtils.getInstance().getFlightId());
//            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyCustomExpandFileNameSettings), customExpandNameSettings, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    LogUtil.log(TAG, "设置文件后缀success");
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError idjiError) {
//                    LogUtil.log(TAG, "设置自定义文件后缀失败：" + new Gson().toJson(idjiError));
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "设置自定义文件后缀失败：当前状态相机禁止操作");
//        }
//    }

//    //切换为广角镜头，恢复曝光
//    public void resumeLensToWideISOProgram() {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect) {
//
//            KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureMode),
//                    CameraExposureMode.PROGRAM, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    LogUtil.log(TAG, "降落后切换曝光模式为自动成功");
//
//                    KeyManager.getInstance().setValue(DJIKey.create(CameraKey.KeyExposureCompensation),
//                            CameraExposureCompensation.POS_1P0EV, new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            LogUtil.log(TAG, "降落后设置曝光补偿数值成功");
//                        }
//                        @Override
//                        public void onFailure(@NonNull IDJIError idjiError) {
//                            LogUtil.log(TAG, "降落后设置曝光补偿数值失败");
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError idjiError) {
//                    LogUtil.log(TAG, "降落后切换曝光模式为自动失败:" + new Gson().toJson(idjiError));
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "降落后降落完成切换曝光失败：当前状态相机禁止操作");
//        }
//    }

//    //设置测温模式
//    public void setThermalTemperatureMeasureMode(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalTemperatureMeasureMode,
//                            ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL),
//                    ThermalTemperatureMeasureMode.find(message.getThermalTemperatureMeasureMode()), new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            sendMsg2Server(message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG,"设置测温模式:"+new Gson().toJson(error));
//                            sendMsg2Server(message, "设置测温模式:" + getIDJIErrorMsg(error));
//                        }
//                    });
//        } else {
//            LogUtil.log(TAG, "设置测温模式失败：当前状态相机禁止操作");
//        }
//    }
//
//    //设置需要测温的点的位置
//    public void setThermalSpotMetersurePoint(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
//            DoublePoint2D doublePoint2D = new DoublePoint2D();
//            doublePoint2D.setX(Double.parseDouble(message.getMetersurePointX()));
//            doublePoint2D.setY(Double.parseDouble(message.getMetersurePointY()));
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalSpotMetersurePoint,
//                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), doublePoint2D, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    sendMsg2Server(message);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"设置点测温失败:"+new Gson().toJson(error));
//                    sendMsg2Server(message, "设置点测温失败:" + getIDJIErrorMsg(error));
//
//                }
//            });
//
//        } else {
//            LogUtil.log(TAG, "测温点设置失败：当前状态相机禁止操作");
//        }
//    }
//
//    //设置需要测温的区域位置
//    public void setThermalRegionMetersureArea(MQMessage message) {
//        Boolean isConnect =KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.
//                KeyConnection,ComponentIndexType.PORT_1));
//        if (isConnect != null && isConnect) {
//            DoubleRect doubleRect = new DoubleRect();
//            doubleRect.setX(Double.parseDouble(message.getMetersureAreaX()));
//            doubleRect.setY(Double.parseDouble(message.getMetersureAreaY()));
//            doubleRect.setHeight(Double.parseDouble(message.getMetersureAreaHeight()));
//            doubleRect.setWidth(Double.parseDouble(message.getMetersureAreaWidth()));
//            KeyManager.getInstance().setValue(KeyTools.createCameraKey(CameraKey.KeyThermalRegionMetersureArea,
//                    ComponentIndexType.PORT_1, CameraLensType.CAMERA_LENS_THERMAL), doubleRect, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    sendMsg2Server(message);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"设置区域测温失败:"+new Gson().toJson(error));
//                    sendMsg2Server(message, "设置区域测温失败:" + getIDJIErrorMsg(error));
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "测温区域设置失败：当前状态相机禁止操作");
//        }
//    }


}
