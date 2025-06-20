package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ApronExecutionStatus;
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
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RtkMobileStationKey;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.common.Velocity3D;
import dji.sdk.keyvalue.value.flightcontroller.FailsafeAction;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel;
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState;
import dji.sdk.keyvalue.value.flightcontroller.LowBatteryRTHInfo;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKTakeoffAltitudeInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.utils.GpsUtils;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.perception.data.PerceptionInfo;
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.diagnostic.DJIDeviceHealthInfo;
import dji.v5.manager.diagnostic.DJIDeviceHealthInfoChangeListener;
import dji.v5.manager.diagnostic.DJIDeviceStatus;
import dji.v5.manager.diagnostic.DJIDeviceStatusChangeListener;
import dji.v5.manager.interfaces.IDeviceHealthManager;
import dji.v5.manager.interfaces.IDeviceStatusManager;
import dji.v5.manager.interfaces.IPerceptionManager;
import dji.v5.manager.interfaces.IWaypointMissionManager;

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
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLon())
                    && !TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLat())) {
                Movement.getInstance().setAlternatePointLon(PreferenceUtils.getInstance().getAlternatePointLon());
                Movement.getInstance().setAlternatePointLat(PreferenceUtils.getInstance().getAlternatePointLat());
            }
            Movement.getInstance().setTimestamp(System.currentTimeMillis());

            Boolean gimBalIsConnect = KeyManager.getInstance().getValue(createKey(GimbalKey.KeyConnection, 0));
            if (gimBalIsConnect != null && gimBalIsConnect) {
                KeyManager.getInstance().listen(createKey(GimbalKey.KeyGimbalAttitude, 0), this, new CommonCallbacks.KeyListener<Attitude>() {
                    @Override
                    public void onValueChange(@Nullable Attitude oldValue, @Nullable Attitude newValue) {
                        if (newValue != null) {
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
                    Log.e(TAG, "监听飞机状态:" + to.name());

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


            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyIsFlying), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        isFlying = newValue;
                        Movement.getInstance().setPlaneWing(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAreMotorsOn), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                    if (newValue != null) {
                        isMotorsOn = newValue;
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyTakeoffLocationAltitude), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTakeoffLocationAltitude(t1);
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo), this, new CommonCallbacks.KeyListener<RTKTakeoffAltitudeInfo>() {
                @Override
                public void onValueChange(@Nullable RTKTakeoffAltitudeInfo rtkTakeoffAltitudeInfo, @Nullable RTKTakeoffAltitudeInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRTKTakeoffAltitude(t1.getAltitude());
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftLocation3D), this, new CommonCallbacks.KeyListener<LocationCoordinate3D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate3D oldValue, @Nullable LocationCoordinate3D newValue) {
                    if (newValue != null) {

                        double distance = LocationUtils.getDistance(Movement.getInstance().getHomepointLong(), Movement.getInstance().getHomepointLat(), String.valueOf(newValue.getLongitude()), String.valueOf(newValue.getLatitude()));
                        Movement.getInstance().setDistance((int) distance);

                        Movement.getInstance().setEgm96Altitude(GpsUtils.egm96Altitude(newValue.getAltitude(),
                                newValue.getLatitude(), newValue.getLongitude()));

                        if (newValue.getAltitude() != null) {
                            Movement.getInstance().setFlyingHeight(Double.parseDouble(decimalFormat.format(newValue.getAltitude())));
                        }
                        Movement.getInstance().setCurrentLatitude(newValue.getLatitude() + "");
                        Movement.getInstance().setCurrentLongitude(newValue.getLongitude() + "");
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftVelocity), this, new CommonCallbacks.KeyListener<Velocity3D>() {
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

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGPSSatelliteCount), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setSatelliteNumber(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGPSSignalLevel), this, new CommonCallbacks.KeyListener<GPSSignalLevel>() {
                @Override
                public void onValueChange(@Nullable GPSSignalLevel gpsSignalLevel, @Nullable GPSSignalLevel t1) {
                    if (t1 != null) {
                        Movement.getInstance().setGPSSignalLevel(t1.name());
                        pushFlightAttitude();

                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyHomeLocation), this, new CommonCallbacks.KeyListener<LocationCoordinate2D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate2D oldValue, @Nullable LocationCoordinate2D newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setHomepointLat(String.valueOf(newValue.getLatitude()));
                        Movement.getInstance().setHomepointLong(String.valueOf(newValue.getLongitude()));
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyDistanceLimitEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setDistanceLimitEnabled(t1);
                        pushFlightAttitude();
                    }
                }
            });


            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyWindSpeed), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setWindSpeed(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyCompassHeading), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double oldValue, @Nullable Double newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setAngleYaw(newValue.intValue());
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyFlightMode), this, new CommonCallbacks.KeyListener<FlightMode>() {
                @Override
                public void onValueChange(@Nullable FlightMode oldValue, @Nullable FlightMode newValue) {
                    if (newValue != null) {

                        if (newValue == FlightMode.MOTOR_START) {
                            //刚起飞时，重置保存的状态
                            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
                            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
                            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                            if (!sendStartTakeOffMsg) {
                                SendStartTakeOffManager.getInstance().sendStartTakeOff2Server(mqttAndroidClient);
                                sendStartTakeOffMsg = true;
                            }
                        }
                        Movement.getInstance().setPlaneMode(newValue.name());
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftAttitude), this, new CommonCallbacks.KeyListener<Attitude>() {
                @Override
                public void onValueChange(@Nullable Attitude attitude, @Nullable Attitude t1) {
                    if (t1 != null) {
                        Movement.getInstance().setPitch(String.valueOf(t1.getPitch()));
                        Movement.getInstance().setYaw(String.valueOf(t1.getYaw().intValue()));
                        Movement.getInstance().setRoll(String.valueOf(t1.getRoll()));
                    }
                    pushFlightAttitude();
                }
            });
            KeyManager.getInstance().listen(createKey(AirLinkKey.KeyUpLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setRemoteControlSignal(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(AirLinkKey.KeyDownLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setPictureBiographySignal(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            GoHomeState goHomeState = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyGoHomeStatus));
            if (goHomeState != null) {
                Movement.getInstance().setGoHomeState(goHomeState.value());
            }

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGoHomeStatus), this, new CommonCallbacks.KeyListener<GoHomeState>() {
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

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyUltrasonicHeight), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setUltrasonicHeight(newValue);
                        pushFlightAttitude();

                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightDistance), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightDistance(t1.toString());
                        pushFlightAttitude();

                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightDuration), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightDuration(t1.toString());
                        pushFlightAttitude();

                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightTimes), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAircraftTotalFlightTimes(t1.toString());
                        pushFlightAttitude();

                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyFailsafeAction), this, new CommonCallbacks.KeyListener<FailsafeAction>() {
                @Override
                public void onValueChange(@Nullable FailsafeAction failsafeAction, @Nullable FailsafeAction t1) {
                    if (t1 != null) {
                        Movement.getInstance().setFailsafeAction(t1.value());
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyHeightLimit), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setHeightLimit(t1);
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyDistanceLimit), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setDistanceLimit(t1);
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyDistanceLimitEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setDistanceLimitEnabled(t1 ? 1 : 0);
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLowBatteryWarningThreshold(t1);
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setSeriousLowBatteryWarningThreshold(t1);
                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyLowBatteryRTHEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLowBatteryRTHEnabled(t1 ? 1 : 0);
                    }
                }
            });
        } else {
            Log.e(TAG, "初始化飞控失败" + "flight controller is null");
        }
    }

    // 使用GsonBuilder配置Gson实例以允许序列化特殊浮点数值
    Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues() // 这是关键
            .create();
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
    //飞机电机起转后发送一次开始飞行
    private boolean sendStartTakeOffMsg;

    public boolean isSendDetect() {
        return isSendDetect;
    }

    public void setSendDetect(boolean sendDetect) {
        isSendDetect = sendDetect;
    }

    private void pushFlightAttitude() {
        //强制返航
        batteryLowLanding();
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
//            XcFileLog.getInstace().f(TAG,new Gson().toJson(Movement.getInstance()));
            XcFileLog.getInstace().f(TAG, "position:" + Movement.getInstance().getCurrentLongitude() + ","
                    + Movement.getInstance().getCurrentLatitude()
                    + "--altitude:" + Movement.getInstance().getFlyingHeight()
                    + "--uAltitude:" + Movement.getInstance().getUltrasonicHeight()
                    + "--heath:" + Movement.getInstance().getWarningMessage()
                    + "--status:" + Movement.getInstance().getPlaneMessage() + "--advancedMode" + Movement.getInstance().getIsVirtualStickAdvancedModeEnabled());
            Movement.getInstance().setEgm96Altitude(
                    GpsUtils.egm96Altitude((Movement.getInstance().getRTKTakeoffAltitude() +
                                    Movement.getInstance().getFlyingHeight()),
                            Double.parseDouble(Movement.getInstance().getCurrentLatitude()), Double.parseDouble(Movement.getInstance().getCurrentLongitude())));
            Movement.getInstance().setTimestamp(System.currentTimeMillis());
            Movement.getInstance().setSn(PreferenceUtils.getInstance().getMqttSn());
            if (isFlying) {
                Movement.getInstance().setTaskId(PreferenceUtils.getInstance().getTaskId());
            }
            //推送飞行状态
            MqttMessage flightMessage = null;
            try {
                flightMessage = new MqttMessage(gson.toJson(Movement.getInstance()).getBytes("UTF-8"));
            } catch (Exception e) {
                LogUtil.log(TAG, "推送飞机状态失败:mqtt未连接" + e);
            }
            flightMessage.setQos(0);
            publish(mqttAndroidClient, AMSConfig.getInstance().getMqttMsdkPushMessage2ServerTopic(), flightMessage);
        }

    }

    //(决定飞机触发电量低的返航)
    public boolean isTriggerRoomBattrryLanding;

    private void batteryLowLanding() {
        if (isTriggerRoomBattrryLanding) {
            return;
        }
        int forcedBattery = Integer.valueOf(PreferenceUtils.getInstance().getForcedBattery());
        Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                KeyBatteryPowerPercent, 0));
        String missionState = Movement.getInstance().getWaypointMissionExecuteState();
        boolean isMissionExecuting = (!TextUtils.isEmpty(missionState) &&
                (missionState.equals("EXECUTING") || missionState.equals("ENTER_WAYLINE"))
                || (!TextUtils.isEmpty(Movement.getInstance().getPlaneMode())
                && Movement.getInstance().getPlaneMode().equals("WAYPOINT")));
        // 获取飞行状态和航线状态
        boolean isFlyingAndBatteryOk = isFlying && value != null && value < forcedBattery && isMissionExecuting;
        boolean isAlterLandStatus = (PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand() || PreferenceUtils.getInstance().getTriggerToAlternatePoint());

        if (isFlyingAndBatteryOk && !isTriggerRoomBattrryLanding && !isAlterLandStatus) {
            isTriggerRoomBattrryLanding = true;
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    LogUtil.log(TAG, "电量低于阈值，直接返航");
                    sendMissionExecuteEvents(mqttAndroidClient, "电量低于阈值，强制返航");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "电量低于阈值，返航失败:" + new Gson().toJson(error));
                    sendMissionExecuteEvents(mqttAndroidClient, "电量低于阈值，返航失败");
                }
            });

        }
    }

    private void closeCabinDoor() {
        // 获取飞行状态和航线状态
        boolean isFlyingAndHeightOk = isFlying && Movement.getInstance().getFlyingHeight() > 10;
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        String missionState = Movement.getInstance().getWaypointMissionExecuteState();
        boolean isMissionExecuting = (!TextUtils.isEmpty(missionState) &&
                (missionState.equals("EXECUTING") || missionState.equals("ENTER_WAYLINE"))
                || (!TextUtils.isEmpty(Movement.getInstance().getPlaneMode()) && Movement.getInstance().getPlaneMode().equals("WAYPOINT")));

        // 当飞机在飞行，高度足够，且航线状态为EXECUTING或ENTER_WAYLINE时，触发关舱门，开启水平避障
        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint() && isFlyingAndHeightOk && !isDebugMode && isMissionExecuting && !sendCloseCabinDoorMsg) {
            sendCloseCabinDoorMsg = true;
            DockCloseManager.getInstance().sendDockCloseMsg2Server(mqttAndroidClient);
            PerceptionManager.getInstance().setPerceptionEnable(true);
        }
    }


    private static final int FLYING_HEIGHT_THRESHOLD = 10; // 开舱门飞行高度阈值
    private static final int DISTANCE_THRESHOLD = 500; // 返航距离阈值

    private void openCabinDoor() {
        boolean isReturningHome = goHomeExecutionState == GoHomeState.RETURNING_TO_HOME.value() ||
                goHomeExecutionState == GoHomeState.LANDING.value();
        float distance = Movement.getInstance().getDistance();
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        boolean isDistanceAndHeightValid = distance < DISTANCE_THRESHOLD &&
                flyingHeight > FLYING_HEIGHT_THRESHOLD && !sendOpenCabinDoorMsg;

        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint() &&
                isReturningHome && isDistanceAndHeightValid && !isDebugMode) {
            LogUtil.log(TAG, "返航距离:" + distance + "---当前高度:" + flyingHeight);
            sendOpenCabinDoorMsg = true;
            DockOpenManager.getInstance().sendDockOpenMsg2Server(mqttAndroidClient);

        }
    }

    //降落时将云台朝下
    private void gimbalDownwards() {
        if (goHomeExecutionState == GoHomeState.LANDING.value()
                && Movement.getInstance().getFlyingHeight() > 11
                && !isGimbalDownwards) {
            DroneHelper.getInstance().setGimbalPitchDegree();
            //将镜头设置为自动对焦
            DroneHelper.getInstance().setCameraFocusMode();
            isGimbalDownwards = true;
            PerceptionManager.getInstance().setPerceptionEnable(false);

        }
    }

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
        }
    }

    // 检查是否满足开始视觉识别降落的条件
    private void checkAndStartVisionLanding() {
//        boolean shouldStartVisionLanding = (PreferenceUtils.getInstance().getLandType() == 2 || !Movement.getInstance().isRtkSign()) ;
        boolean shouldStartVisionLanding = (PreferenceUtils.getInstance().getLandType() == 2);
//                && !PreferenceUtils.getInstance().getTriggerToAlternatePoint();
        if (shouldStartVisionLanding) {
            startVisionLanding();

            // 检查是否满足降落条件
            checkLandingConditions();

        }
    }


    private static final double FLYING_HEIGHT_THRESHOLD_MAX = 10.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MAX_ALTERNATE = 15.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MIN = -2;
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
        isGimbalDownwards = false;
    }

    private boolean shouldStopVisionAndLanding() {
        if (PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()) {
            return !isTriggerLanding && isFlying && isMotorsOn && AlternateArucoDetect.getInstance().isCanLanding();
        } else {
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
                if (!PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()) {
                    // 发送无人机入库消息到服务器********************待修改************************
                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(false);
                    DroneStorageManager.getInstance().sendDroneStorageMsg2Server(mqttAndroidClient, 1);
                }
                // 上传媒体文件
                SystemManager.getInstance().upLoadMedia(mqttAndroidClient);
            }
            // 避免在下次起飞时触发视觉识别
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
            LogUtil.log(TAG, "droneStorage:" + PreferenceUtils.getInstance().getTaskId());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SendLandingManager.getInstance().sendLandingMsg2Server(mqttAndroidClient);
                }
            }, 1000);

        }
    }

    //起飞
    public void startTakeoff(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartTakeoff), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(mqttAndroidClient, message, "起飞失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //返航
    public void startGoHome(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyFlightMode));

        if (Movement.getInstance().isPlaneWing()&&Movement.getInstance().getDistance() < 20
                && Movement.getInstance().getFlyingHeight() < 75
                && Movement.getInstance().getElectricityInfoA() > 35
                &&((flightMode != null&&flightMode==FlightMode.WAYPOINT)||
                (flightMode != null&&flightMode==FlightMode.AUTO_TAKE_OFF))) {
            WayLineExecutingInterruptManager.getInstance().onExecutingInterruptToDo();
        } else {
            Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
            if (isConnect != null && isConnect) {
                KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
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
                            sendMsg2Server(mqttAndroidClient, message, "返航执行失败:" + getIDJIErrorMsg(error));
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

    }

    //取消返航
    public void stopGoHome(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "取消返航执行失败:" + new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "取消返航执行失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }


    //降落
    public void startAutoLanding(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "降落失败:" + new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "降落失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //取消降落
    public void stopAutoLanding(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "取消降落失败:" + new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "取消降落失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //飞机失联后的自动操作
    public void setFailsafeAction(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyFailsafeAction),
                        FailsafeAction.find(message.getFailsafeAction()), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                sendMsg2Server(mqttAndroidClient, message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG, "失控执行动作更新失败:" + new Gson().toJson(error));
                                sendMsg2Server(mqttAndroidClient, message, "失控执行动作更新失败:" + getIDJIErrorMsg(error));
                            }
                        });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置返航高度
    public void setGoHomeHeight(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), message.getGoHomeHeight(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "返航高度设置失败:" + new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "返航高度设置失败:" + getIDJIErrorMsg(error));
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置限高
    public void setHeightLimit(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(createKey(FlightControllerKey.KeyHeightLimit), message.getHeightLimit(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "限高设置失败:" + new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "限高设置失败:" + getIDJIErrorMsg(error));
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置限远
    public void setDistanceLimit(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().setValue(createKey(FlightControllerKey.KeyDistanceLimit), message.getDistanceLimit(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "限远设置失败:" + new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "限远设置失败:" + getIDJIErrorMsg(error));
                }
            });

        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置是否启用限远
    public void setDistanceLimitEnabled(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(createKey(FlightControllerKey.KeyDistanceLimitEnabled), message.getDistanceLimitEnabled() == 0 ? false : true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "限远使能设置失败:" + new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "限远使能设置失败:" + getIDJIErrorMsg(error));
                    }
                });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //获取总里程
    public void getAircraftTotalFlightDistance(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Double value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyAircraftTotalFlightDistance));
            if (value != null) {
                sendAircraftTotalFlightDistance2Server(mqttAndroidClient, message, value);
            } else {
                sendMsg2Server(mqttAndroidClient, message, "获取里程数为空");
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    /**
     * 紧急悬停
     */
    public void emergencyHover(MqttAndroidClient mqttClient, MQMessage message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyFlightMode));
        if (flightMode != null) {
            switch (flightMode) {
                case GO_HOME:
                    KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "紧急悬停，取消返航成功");
                            sendMsg2Server(mqttClient, message);
                            resetAircrftLandingStatus();

                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "紧急悬停，取消返航失败:" + new Gson().toJson(error));
                            sendMsg2Server(mqttClient, message, "紧急悬停，取消返航失败:" + getIDJIErrorMsg(error));
                        }
                    });
                    break;
                case WAYPOINT:
                    IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
                    missionManager.stopMission(TextUtils.isEmpty(Movement.getInstance().getMissionName())
                            ? "aros" : Movement.getInstance().getMissionName(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "紧急悬停，终止任务成功");
                            sendMsg2Server(mqttClient, message);
                            resetAircrftLandingStatus();

                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "紧急悬停，终止任务失败:" + new Gson().toJson(error));
                            sendMsg2Server(mqttClient, message, "紧急悬停，终止任务失败:" + getIDJIErrorMsg(error));
                        }
                    });
                    break;
                case AUTO_LANDING:
                    KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "紧急悬停，取消降落成功");
                            sendMsg2Server(mqttClient, message);
                            resetAircrftLandingStatus();

                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "紧急悬停，取消降落失败:" + new Gson().toJson(error));
                            sendMsg2Server(mqttClient, message, "紧急悬停，取消降落失败:" + getIDJIErrorMsg(error));
                        }
                    });
                    break;
                case VIRTUAL_STICK:
                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "紧急悬停，控制权释放成功");
                            sendMsg2Server(mqttClient, message);
                            resetAircrftLandingStatus();
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "紧急悬停，控制权释放失败:" + new Gson().toJson(error));
                            sendMsg2Server(mqttClient, message, "紧急悬停，控制权失败:" + getIDJIErrorMsg(error));
                        }
                    });
                    break;
            }
        }
    }

    private void resetAircrftLandingStatus() {
        // 避免在下次起飞时触发视觉识别
        PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
        PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
        PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
        //设置为未触发开始识别二维码状态
        isSendDetect = false;
        EventBus.getDefault().post(FLAG_STOP_ARUCO);
    }

    //设置低电量阈值【15-50】
    public void setLowBatteryWarningThreshold(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), message.getLowBatteryWarningThreshold(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(mqttAndroidClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "低电量阈值设置失败:" + new Gson().toJson(error));
                        sendMsg2Server(mqttAndroidClient, message, "低电量阈值设置失败:" + getIDJIErrorMsg(error));
                    }
                });
            }

        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置严重低电量阈值(该值默认为10%，Matrice 30 Series不可设置。)
    public void setSeriousLowBatteryWarningThreshold(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            if (message != null) {
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold),
                        message.getSeriousLowBatteryWarningThreshold(), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                sendMsg2Server(mqttAndroidClient, message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG, "严重低电量阈值设置失败:" + new Gson().toJson(error));
                                sendMsg2Server(mqttAndroidClient, message, "严重低电量阈值设置失败:" + getIDJIErrorMsg(error));
                            }
                        });
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置智能低电量返航
    public void setLowBatteryRTHEnabled(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLowBatteryRTHEnabled),
                    message.getLowBatteryRTHEnabled() == 0 ? false : true, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMsg2Server(mqttAndroidClient, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "智能低电量返航设置失败:" + new Gson().toJson(error));
                            sendMsg2Server(mqttAndroidClient, message, "智能低电量返航设置失败:" + getIDJIErrorMsg(error));
                        }
                    });
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }


}
