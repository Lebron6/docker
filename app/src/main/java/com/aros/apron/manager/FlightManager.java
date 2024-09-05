package com.aros.apron.manager;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.GISNeedDataEntity;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.AlternateArucoDetect;
import com.aros.apron.tools.ApronArucoDetect;
import com.aros.apron.tools.DroneHelper;
import com.aros.apron.tools.LocationUtils;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.xclog.XcFileLog;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.common.Velocity3D;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel;
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.perception.data.PerceptionInfo;
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener;
import dji.v5.manager.diagnostic.DJIDeviceHealthInfo;
import dji.v5.manager.diagnostic.DJIDeviceHealthInfoChangeListener;
import dji.v5.manager.diagnostic.DJIDeviceStatus;
import dji.v5.manager.diagnostic.DJIDeviceStatusChangeListener;
import dji.v5.manager.interfaces.IDeviceHealthManager;
import dji.v5.manager.interfaces.IDeviceStatusManager;
import dji.v5.manager.interfaces.IPerceptionManager;

public class FlightManager extends BaseManager {


    private MqttAndroidClient mqttAndroidClient;
    private IPerceptionManager iPerceptionManager;
    private IDeviceHealthManager iDeviceHealthManager;
    private IDeviceStatusManager iDeviceStatusManager;
    private boolean isFlying;
    private boolean isMotorsOn;
    DecimalFormat decimalFormat = new DecimalFormat("#.0"); // 保留一位小数

    private FlightManager() {
    }

    private static class FlightControlHolder {
        private static final FlightManager INSTANCE = new FlightManager();
    }

    public static FlightManager getInstance() {
        return FlightControlHolder.INSTANCE;
    }


    public void initFlightInfo(MqttAndroidClient mqttAndroidClient) {
        this.mqttAndroidClient = mqttAndroidClient;
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLon())
                    && !TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLat())) {
                Movement.getInstance().setAlternatePointLon(PreferenceUtils.getInstance().getAlternatePointLon());
                Movement.getInstance().setAlternatePointLat(PreferenceUtils.getInstance().getAlternatePointLat());
            }
            Movement.getInstance().setTimestamp(System.currentTimeMillis());

            Boolean gimBalIsConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.KeyConnection, 0));
            if (gimBalIsConnect != null && gimBalIsConnect) {
                KeyManager.getInstance().listen(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, 0), this, new CommonCallbacks.KeyListener<Attitude>() {
                    @Override
                    public void onValueChange(@Nullable Attitude oldValue, @Nullable Attitude newValue) {
                        if (newValue != null) {
                            GISNeedDataEntity.getInstance().setGimbalYaw(String.valueOf(newValue.getYaw()));
                            GISNeedDataEntity.getInstance().setGimbalRoll(String.valueOf(newValue.getRoll()));
                            GISNeedDataEntity.getInstance().setGimbalPitch(String.valueOf(newValue.getPitch()));
                            Movement.getInstance().setGimbalYaw(String.valueOf(newValue.getYaw()));
                            Movement.getInstance().setGimbalRoll(String.valueOf(newValue.getRoll()));
                            Movement.getInstance().setGimbalPitch(String.valueOf(newValue.getPitch()));
                            pushFlightAttitude();
                        }
                    }
                });
            }

            iDeviceStatusManager = dji.v5.manager.diagnostic.DeviceStatusManager.getInstance();
            iDeviceStatusManager.addDJIDeviceStatusChangeListener(new DJIDeviceStatusChangeListener() {
                @Override
                public void onDeviceStatusUpdate(DJIDeviceStatus from, DJIDeviceStatus to) {
                    if (to != null && !TextUtils.isEmpty(to.description())) {
                        Movement.getInstance().setPlaneMessage(to.description());
                        pushFlightAttitude();
                    }
                }
            });
            iPerceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance();
            iPerceptionManager.addPerceptionInformationListener(new PerceptionInformationListener() {
                @Override
                public void onUpdate(@NonNull PerceptionInfo information) {
                    if (information != null) {
                        Movement.getInstance().setLevelObstacleAvoidance(information.isHorizontalObstacleAvoidanceEnabled());
                        pushFlightAttitude();
                    }
                }
            });
            iDeviceHealthManager = dji.v5.manager.diagnostic.DeviceHealthManager.getInstance();
            iDeviceHealthManager.addDJIDeviceHealthInfoChangeListener(new DJIDeviceHealthInfoChangeListener() {
                @Override
                public void onDeviceHealthInfoUpdate(List<DJIDeviceHealthInfo> infos) {
                    if (infos != null && infos.size() > 0) {
                        String warningMessage = infos.get(0).description();
                        if (!TextUtils.isEmpty(warningMessage)) {
                            Movement.getInstance().setWarningMessage(warningMessage);
                            pushFlightAttitude();
                        }
                    } else {
                        Log.e(TAG, "监听设备健康,无异常");
                        Movement.getInstance().setWarningMessage("");
                        pushFlightAttitude();
                    }
                }
            });


            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIsFlying), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        isFlying = newValue;
                        GISNeedDataEntity.getInstance().setPlaneWing(newValue);
                        Movement.getInstance().setPlaneWing(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        isMotorsOn = newValue;
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), this, new CommonCallbacks.KeyListener<LocationCoordinate3D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate3D oldValue, @Nullable LocationCoordinate3D newValue) {
                    if (newValue != null) {
                        double distance = LocationUtils.getDistance(Movement.getInstance().getHomepointLong(), Movement.getInstance().getHomepointLat(), String.valueOf(newValue.getLongitude()), String.valueOf(newValue.getLatitude()));
                        Movement.getInstance().setDistance((int) distance);
                        GISNeedDataEntity.getInstance().setFlyingHeight(newValue.getAltitude().intValue());
                        GISNeedDataEntity.getInstance().setCurrentLatitude(newValue.getLatitude() + "");
                        GISNeedDataEntity.getInstance().setCurrentLongitude(newValue.getLongitude() + "");
                        if (newValue.getAltitude() != null) {
                            Movement.getInstance().setFlyingHeight(Double.parseDouble(decimalFormat.format(newValue.getAltitude())));
                        }
                        Movement.getInstance().setCurrentLatitude(newValue.getLatitude() + "");
                        Movement.getInstance().setCurrentLongitude(newValue.getLongitude() + "");
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), this, new CommonCallbacks.KeyListener<Velocity3D>() {
                @Override
                public void onValueChange(@Nullable Velocity3D oldValue, @Nullable Velocity3D newValue) {
                    if (newValue != null) {
                        if (newValue.getZ() != null) {
                            Movement.getInstance().setVerticalSpeed(String.format("%.1f", Math.abs(newValue.getZ())));
                        }
                        if (newValue.getY() != null && newValue.getX() != null) {
                            Movement.getInstance().setHorizontalSpeed(String.format("%.1f", Math.abs(Math.sqrt((newValue.getX() * newValue.getX()) + (newValue.getY() * newValue.getY())))));
                        }
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setSatelliteNumber(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel), this, new CommonCallbacks.KeyListener<GPSSignalLevel>() {
                @Override
                public void onValueChange(@Nullable GPSSignalLevel gpsSignalLevel, @Nullable GPSSignalLevel t1) {
                    if (t1 != null) {
                        Movement.getInstance().setGPSSignalLevel(t1.name());
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), this, new CommonCallbacks.KeyListener<LocationCoordinate2D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate2D oldValue, @Nullable LocationCoordinate2D newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setHomepointLat(String.valueOf(newValue.getLatitude()));
                        Movement.getInstance().setHomepointLong(String.valueOf(newValue.getLongitude()));
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setDistanceLimitEnabled(t1);
                        pushFlightAttitude();
                    }
                }
            });


            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyWindSpeed), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setWindSpeed(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyCompassHeading), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double oldValue, @Nullable Double newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setAngleYaw(newValue.intValue());
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyFlightMode), this, new CommonCallbacks.KeyListener<FlightMode>() {
                @Override
                public void onValueChange(@Nullable FlightMode oldValue, @Nullable FlightMode newValue) {
                    if (newValue != null) {
                        if (newValue == FlightMode.MOTOR_START) {
                            //刚起飞时，重置保存的状态
                            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
                            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
                            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                        }
                        Log.e(TAG, "飞行模式:" + newValue.name());
                        Movement.getInstance().setPlaneMode(newValue.name());
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), this, new CommonCallbacks.KeyListener<Attitude>() {
                @Override
                public void onValueChange(@Nullable Attitude attitude, @Nullable Attitude t1) {
                    if (t1 != null) {
//                        LogUtil.log(TAG,"偏航:"+t1.getYaw());

                        GISNeedDataEntity.getInstance().setPitch(String.valueOf(t1.getPitch()));
                        GISNeedDataEntity.getInstance().setYaw(String.valueOf(t1.getYaw()));
                        GISNeedDataEntity.getInstance().setRoll(String.valueOf(t1.getRoll()));
                        Movement.getInstance().setPitch(String.valueOf(t1.getPitch()));
                        Movement.getInstance().setYaw(String.valueOf(t1.getYaw().intValue()));
                        Movement.getInstance().setRoll(String.valueOf(t1.getRoll()));
                    }
                    pushFlightAttitude();
                }
            });
            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyUpLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setRemoteControlSignal(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyDownLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setPictureBiographySignal(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            GoHomeState goHomeState = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyGoHomeStatus));
            if (goHomeState != null) {
                Movement.getInstance().setGoHomeState(goHomeState.value());
            }

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyGoHomeStatus), this, new CommonCallbacks.KeyListener<GoHomeState>() {
                @Override
                public void onValueChange(@Nullable GoHomeState oldValue, @Nullable GoHomeState newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setGoHomeState(newValue.value());
                        LogUtil.log(TAG, "GoHomeStatus:" + newValue.name());
                        goHomeExecutionState = newValue.value();
                        //返航后触发可入库条件
                        if (newValue.value() == 2) {
                            triggerLandOrGoHome = true;
                        }
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyUltrasonicHeight), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setUltrasonicHeight(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftTotalFlightDistance), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightDistance(t1.toString());
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftTotalFlightDuration), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightDuration(t1.toString());
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftTotalFlightTimes), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightTimes(t1.toString());
                    }
                }
            });
        } else {
            Log.e(TAG, "初始化飞控失败" + "flight controller is null");
        }
    }

    //标识是否降落后触发关舱门
    private boolean triggerLandOrGoHome;
    //标识飞机是否执行完任务处于landing或gohome，避免刚飞出去就执行精准降落
    private int goHomeExecutionState;
    //确保每次流程只发送触发降落一次，降落完成后设置为false，而不是最后开始landing时触发，如果不再landing时设置为false，那么在landing的途中，也可能再次触发landing
    private boolean isSendDetect;
    //飞机飞走后是否发送关舱门(确保只发送一次)
    private boolean sendCloseCabinDoorMsg;
    //飞机飞回后是否发送开舱门(确保只发送一次)
    private boolean sendOpenCabinDoorMsg;
    //飞机是否在返航,处理云台归中逻辑(确保只发送一次)
    public boolean isGimbalReset;
    //飞机是否在降落,处理云台朝下逻辑(确保只发送一次)
    public boolean isGimbalDownwards;
    //(决定飞机触发最后landing的重要因素)是否触发最后一步Landing，如果触发过，确保landing时不再触发landing
    public boolean isTriggerLanding;

    public boolean isSendDetect() {
        return isSendDetect;
    }

    public void setSendDetect(boolean sendDetect) {
        isSendDetect = sendDetect;
    }

    private void pushFlightAttitude() {

        //关仓门
        closeCabinDoor();
        //开舱门
        openCabinDoor();
        //降落时将云台朝下
        gimbalDownwards();
        //返航时将云台归中,曝光ISO降低
        gimbalAndCameraReset();
        //开始视觉识别降落
        checkAndStartVisionLanding();
        //触发入库
        droneStorage();

        if (isFlyClickTime()) {

            XcFileLog.getInstace().e(TAG, "position:" + Movement.getInstance().getCurrentLongitude() + ","
                    + Movement.getInstance().getCurrentLatitude()
                    + "--altitude:" + Movement.getInstance().getFlyingHeight()
                    + "--uAltitude:" + Movement.getInstance().getUltrasonicHeight()
                    + "--heath:" + Movement.getInstance().getWarningMessage()
                    + "--status:" + Movement.getInstance().getPlaneMessage());
            Movement.getInstance().setTimestamp(System.currentTimeMillis());

            //推送飞行状态
            MqttMessage flightMessage = null;
            try {
                flightMessage = new MqttMessage(new Gson().toJson(Movement.getInstance()).getBytes("UTF-8"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            flightMessage.setQos(1);
            publish(mqttAndroidClient, AMSConfig.getInstance().getMqttMsdkPushMessage2ServerTopic(), flightMessage);
        }

        if (isGisFlyClickTime()) {
            //推送飞行状态
            MqttMessage flightMessage = null;
            try {
                flightMessage = new MqttMessage(new Gson().toJson(GISNeedDataEntity.getInstance()).getBytes("UTF-8"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            flightMessage.setQos(1);
            publish(mqttAndroidClient, AMSConfig.getInstance().getMqttMsdkPushGisMessage2ServerTopic(), flightMessage);
        }
    }

    private void closeCabinDoor() {
        // 获取飞行状态和航线状态
        boolean isFlyingAndHeightOk = isFlying && Movement.getInstance().getFlyingHeight() > 10;
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        String missionState = Movement.getInstance().getWaypointMissionExecuteState();
        boolean isMissionExecuting = !TextUtils.isEmpty(missionState) &&
                (missionState.equals("EXECUTING") || missionState.equals("ENTER_WAYLINE"));
//        Log.e(TAG,"isFlyingAndHeightOk:"+isFlyingAndHeightOk+"---isDebugMode:"+isDebugMode
//        +"---missionState:"+missionState+"---isMissionExecuting:"+isMissionExecuting+"---sendCloseCabinDoorMsg:"+
//                sendOpenCabinDoorMsg);
        // 当飞机在飞行，高度足够，且航线状态为EXECUTING或ENTER_WAYLINE时，触发关舱门，开启避障
        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint()&&isFlyingAndHeightOk && !isDebugMode && isMissionExecuting && !sendCloseCabinDoorMsg) {
            sendCloseCabinDoorMsg = true;
            DockCloseManager.getInstance().sendDockCloseMsg2Server(mqttAndroidClient);
            PerceptionManager.getInstance().setPerceptionEnable(true);
        }
    }


    private static final int FLYING_HEIGHT_THRESHOLD = 10; // 开舱门飞行高度阈值
    private static final int DISTANCE_THRESHOLD = 10000; // 返航距离阈值

    private void openCabinDoor() {
        boolean isReturningHome = goHomeExecutionState == GoHomeState.RETURNING_TO_HOME.value() ||
                goHomeExecutionState == GoHomeState.LANDING.value();
        float distance = Movement.getInstance().getDistance();
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        boolean isDistanceAndHeightValid = distance < DISTANCE_THRESHOLD &&
                flyingHeight > FLYING_HEIGHT_THRESHOLD && !sendOpenCabinDoorMsg;

        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint()&&
                isReturningHome && isDistanceAndHeightValid && !isDebugMode) {
            LogUtil.log(TAG, "返航距离:" + distance + "---当前高度:" + flyingHeight);
            sendOpenCabinDoorMsg = true;
            DockOpenManager.getInstance().sendDockOpenMsg2Server(mqttAndroidClient);
            PerceptionManager.getInstance().setPerceptionEnable(false);

        }
    }

    //降落时将云台朝下
    private void gimbalDownwards(){
        if (goHomeExecutionState == GoHomeState.LANDING.value()
                && Movement.getInstance().getFlyingHeight() > 15
                && !isGimbalDownwards){
            DroneHelper.getInstance().setGimbalPitchDegree();
            isGimbalDownwards=true;
        }
    }

    // 提取条件判断到一个单独的方法中
    private boolean shouldResetGimbalAndCamera() {
        return goHomeExecutionState == GoHomeState.RETURNING_TO_HOME.value()
                && Movement.getInstance().getFlyingHeight() > 20
                && !isGimbalReset;
    }


    private void gimbalAndCameraReset() {
        if (shouldResetGimbalAndCamera()) {
            GimbalManager.getInstance().gimbalReset();
            CameraManager.getInstance().resumeLensToWideISOManual();
            isGimbalReset = true;
            sendMissionExecuteEvents(mqttAndroidClient, "降落重置云台");

        }
    }

    // 检查是否满足开始视觉识别降落的条件
    private void checkAndStartVisionLanding() {
//        boolean shouldStartVisionLanding = (PreferenceUtils.getInstance().getLandType() == 2 || !Movement.getInstance().isRtkSign()) ;
        boolean shouldStartVisionLanding = (PreferenceUtils.getInstance().getLandType() == 2 ) ;
//                && !PreferenceUtils.getInstance().getTriggerToAlternatePoint();
        if (shouldStartVisionLanding) {
            startVisionLanding();

            // 检查是否满足降落条件
            checkLandingConditions();

        }
    }


    private static final double FLYING_HEIGHT_THRESHOLD_MAX = 9.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MAX_ALTERNATE = 15.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MIN = 0.1;
    private static final double FLYING_HEIGHT_THRESHOLD_MIN_ALTERNATE = 2.0;

    private void startVisionLanding() {
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        boolean triggerToAlternatePoint = PreferenceUtils.getInstance().getTriggerToAlternatePoint();
        boolean needTriggerApronArucoLand = PreferenceUtils.getInstance().getNeedTriggerApronArucoLand();
        boolean needTriggerAlterArucoLand = PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand();
        double thresholdMax = triggerToAlternatePoint ? FLYING_HEIGHT_THRESHOLD_MAX_ALTERNATE : FLYING_HEIGHT_THRESHOLD_MAX;

        if (isFlying && Movement.getInstance().getFlyingHeight() < thresholdMax && !isSendDetect) {
            double flyingHeight = Movement.getInstance().getFlyingHeight();
            double thresholdMin = triggerToAlternatePoint ? FLYING_HEIGHT_THRESHOLD_MIN_ALTERNATE : FLYING_HEIGHT_THRESHOLD_MIN;

            if (flyingHeight > thresholdMin) {
                boolean shouldTriggerDetection;

                if (isDebugMode) {
                    shouldTriggerDetection = goHomeExecutionState == 2;
                } else {
                    shouldTriggerDetection = goHomeExecutionState == 2 || needTriggerApronArucoLand || needTriggerAlterArucoLand;
                }

                if (shouldTriggerDetection) {
                    triggerArucoDetection();
                }
            }
        }
    }


    private void triggerArucoDetection() {

        if (PreferenceUtils.getInstance().getTriggerToAlternatePoint()) {
            LogUtil.log(TAG, "识别AlterTag:" + PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand());
            EventBus.getDefault().post(FLAG_START_DETECT_ARUCO_ALTERNATE);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(true);
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            LogUtil.log(TAG, "开始识别备降点二维码,椭球高度:" + Movement.getInstance().getFlyingHeight() + "米" + "--超声波高度:" + Movement.getInstance().getUltrasonicHeight() + "分米");
            sendMissionExecuteEvents(mqttAndroidClient, "开始备降点视觉降落");
        } else {
            LogUtil.log(TAG, "识别ApronTag:" + PreferenceUtils.getInstance().getNeedTriggerApronArucoLand());
            EventBus.getDefault().post(FLAG_START_DETECT_ARUCO_APRON);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(true);
            LogUtil.log(TAG, "开始识别机库二维码,椭球高度:" + Movement.getInstance().getFlyingHeight() + "米" + "--超声波高度:" + Movement.getInstance().getUltrasonicHeight() + "分米");
            sendMissionExecuteEvents(mqttAndroidClient, "开始机库视觉降落");
        }
        isSendDetect = true;
        PerceptionManager.getInstance().setPerceptionEnable(false);

    }

    // 定义常量用于EventBus
    public static final String FLAG_DOWN_LAND = "FLAG_DOWN_LAND";
    public static final String FLAG_START_DETECT_ARUCO_APRON = "FLAG_START_DETECT_ARUCO_APRON";
    public static final String FLAG_START_DETECT_ARUCO_ALTERNATE = "FLAG_START_DETECT_ARUCO_ALTERNATE";
    public static final String FLAG_STOP_ARUCO = "FLAG_STOP_ARUCO";


    // 检查是否满足降落条件，并触发相应的降落逻辑
    public void checkLandingConditions() {
        //到达备降点触发了直接降落高度或备降点未识别到二维码
        if (PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand() && shouldStopVisionAndLanding()) {
            stopArucoDetectAndLanding(3);
            LogUtil.log(TAG, "备降点直接降落");
        } else {
             if (shouldStopVisionAndLanding()) {
                stopArucoDetectAndLanding(2);
            }
        }
    }

    public void stopArucoDetectAndLanding(int i) {
        logLandingHeight(i);
        DroneHelper.getInstance().exitVirtualStickMode();
        EventBus.getDefault().post(FLAG_DOWN_LAND);
        PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
        PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
        PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
        isGimbalReset = false;
        isTriggerLanding = true;
    }

    private boolean shouldStopVisionAndLanding() {
        if (PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()){
            return !isTriggerLanding && isFlying && isMotorsOn && AlternateArucoDetect.getInstance().isCanLanding();
        }else{
            return !isTriggerLanding && isFlying && isMotorsOn && ApronArucoDetect.getInstance().isCanLanding();
        }
    }

    private void logLandingHeight(int i) {
        LogUtil.log(TAG, "降落高度" + Movement.getInstance().getFlyingHeight() + "米---"
                + Movement.getInstance().getUltrasonicHeight() + "分米");
    }




    private void droneStorage() {
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        // 检查无人机是否满足降落和入库的条件
        if (triggerLandOrGoHome && !isMotorsOn && !isFlying && Movement.getInstance().getFlyingHeight() <= 0.0) {
            // 重置降落或返航的触发标志
            triggerLandOrGoHome = false;
            // 禁用触发和检测标志
            isSendDetect = false;
            isTriggerLanding = false;
            sendCloseCabinDoorMsg = false;
            ApronArucoDetect.getInstance().setCanLanding(false);

            // 发布事件，通知其他组件停止Aruco检测
            EventBus.getDefault().post(FLAG_STOP_ARUCO);
            if (!isDebugMode) {
                //这里可能也会触发备降点关舱门的逻辑
                if (!PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()){
                    // 发送无人机入库消息到服务器********************待修改************************
                    DroneStorageManager.getInstance().sendDroneStorageMsg2Server(mqttAndroidClient, 1);
                    sendMissionExecuteEvents(mqttAndroidClient, "降落完成:执行入库");
                }
                // 上传媒体文件
                SystemManager.getInstance().upLoadMedia(mqttAndroidClient);
            }
            // 避免在下次起飞时触发视觉识别
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);

        }
    }

    //起飞
    public void startTakeoff(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "起飞失败:" + error.description());
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //返航
    public void startGoHome(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    if (mqttAndroidClient != null && message != null) {
                        sendMsg2Server(mqttAndroidClient, message);
                    }
                    LogUtil.log(TAG, "返航调用成功");

                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (mqttAndroidClient != null && message != null) {
                        sendMsg2Server(mqttAndroidClient, message, "返航执行失败:" + new Gson().toJson(error));
                    }
                    LogUtil.log(TAG, "返航执行失败：" + new Gson().toJson(error));
                }
            });
        } else {
            if (mqttAndroidClient != null && message != null) {
                sendMsg2Server(mqttAndroidClient, message, "返航执行失败：飞控未连接");
            }
            LogUtil.log(TAG, "返航执行失败：飞控未连接");

        }
    }

    //取消返航
    public void stopGoHome(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "取消返航执行失败:" + error.description());
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }


    //降落
    public void startAutoLanding(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "降落失败:" + error.description());
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //取消降落
    public void stopAutoLanding(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "取消降落失败:" + error.description());
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //    //飞机失联后的自动操作
//    public void failsafeAction(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            if (message != null) {
//
//                String type = message.getData().getFailsafeAction();
//                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyFailsafeAction),
//                        FailsafeAction.find(Integer.parseInt(type)), new CommonCallbacks.CompletionCallback() {
//                            @Override
//                            public void onSuccess() {
//                                sendMsg2Server(mqttAndroidClient, message);
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull IDJIError error) {
//                                sendMsg2Server(mqttAndroidClient, message, "失控执行动作更新失败:" + error.description());
//                            }
//                        });
//            }
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }
//
//    //设置返航高度
//    public void setGoHomeHeight(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            if (message != null) {
//
//                String type = message.getData().getGoHomeHeight();
//                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), Integer.parseInt(type), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "返航高度更新失败:" + error.description());
//                    }
//                });
//            }
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }
//
    //设置限高
    public void setHeightLimit(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyHeightLimit), message.getHeightLimit(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "限高更新失败:" + error.description());
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置限远
    public void setDistanceLimit(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyDistanceLimit), message.getDistanceLimit(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "限远设置失败:" + error.description());
                }
            });

        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置是否启用限远
    public void setDistanceLimitEnabled(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyDistanceLimitEnabled), message.getDistanceLimitEnabled() == 0 ? false : true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendMsg2Server(mqttAndroidClient, message, "限远开关设置失败:" + error.description());
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //获取总里程
    public void getAircraftTotalFlightDistance(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Double value = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftTotalFlightDistance));
            if (value != null) {
                sendAircraftTotalFlightDistance2Server(mqttAndroidClient, message,value);
            }else{
                sendMsg2Server(mqttAndroidClient, message, "获取里程数为空");
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }


//
//    //设置低电量阈值【15-50】
//    public void setLowBatteryWarningThreshold(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
//                KeyConnection));
//        if (isConnect != null && isConnect) {
//            if (message != null) {
//                String type = message.getData().getLowBatteryWarningThreshold();
//                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), Integer.parseInt(type), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "低电量阈值设置失败:" + error.description());
//                    }
//                });
//            }
//
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }
//
//    //设置严重低电量阈值(该值默认为10%，Matrice 30 Series不可设置。)
//    public void setSeriousLowBatteryWarningThreshold(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
//                KeyConnection));
//        if (isConnect != null && isConnect) {
//            if (message != null) {
//                String type = message.getData().getSeriousLowBatteryWarningThreshold();
//                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), Integer.parseInt(type), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "严重低电量阈值设置失败:" + error.description());
//                    }
//                });
//            }
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }
//
//    //设置只能低电量返航
//    public void setLowBatteryRTHEnabled(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
//                KeyConnection));
//        if (isConnect != null && isConnect) {
//            String type = message.getData().getLowBatteryRTHEnabled();
//            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled), type.equals("1") ? true : false, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    sendMsg2Server(mqttAndroidClient, message);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    sendMsg2Server(mqttAndroidClient, message, "智能低电量返航更新失败:" + error.description());
//                }
//            });
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }


//    //获取飞行状态
//    public void getHomeLocation(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
//                KeyConnection));
//        if (isConnect != null && isConnect) {
//            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyHomeLocation),
//                    new CommonCallbacks.CompletionCallbackWithParam<LocationCoordinate2D>() {
//                        @Override
//                        public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
//                            sendMsg2Server(mqttAndroidClient, message, locationCoordinate2D.getLatitude() + "," + locationCoordinate2D.getLongitude());
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            sendMsg2Server(mqttAndroidClient, message, "获取返航点位置失败:" + error.description());
//                        }
//                    });
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
//        }
//    }

    public void releaseFlightStateKey() {
        KeyManager.getInstance().cancelListen(this);
    }


}
