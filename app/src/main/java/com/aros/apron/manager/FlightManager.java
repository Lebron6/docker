package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.callback.MqttCallBack;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.AlternateArucoDetect;
import com.aros.apron.tools.ApronArucoDetect;
import com.aros.apron.tools.DroneHelper;
import com.aros.apron.tools.LocationUtils;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.xclog.XcFileLog;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
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
import dji.sdk.keyvalue.value.flightcontroller.LEDsSettings;
import dji.sdk.keyvalue.value.flightcontroller.PropellerRotationCommand;
import dji.sdk.keyvalue.value.flightcontroller.PropellerRotationCommandResult;
import dji.sdk.keyvalue.value.flightcontroller.PropellerRotationStatus;
import dji.sdk.keyvalue.value.flightcontroller.RemoteControllerFlightMode;
import dji.sdk.keyvalue.value.flightcontroller.RidWorkingStatusPushMsg;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.sdk.keyvalue.value.rtkmobilestation.RTKTakeoffAltitudeInfo;
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint;
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


    private IPerceptionManager iPerceptionManager;
    private IDeviceHealthManager iDeviceHealthManager;
    private IDeviceStatusManager iDeviceStatusManager;
    private boolean isFlying;
    private boolean isMotorsOn;
    private int waypointIndexAlreadySend=-1;
    DecimalFormat decimalFormat = new DecimalFormat("#.0"); // 保留一位小数

    private FlightManager() {
    }

    private static class FlightControlHolder {
        private static final FlightManager INSTANCE = new FlightManager();
    }

    public static FlightManager getInstance() {
        return FlightControlHolder.INSTANCE;
    }


    public void initFlightInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLon())
                    && !TextUtils.isEmpty(PreferenceUtils.getInstance().getAlternatePointLat())) {
                Movement.getInstance().setAlternatePointLon(PreferenceUtils.getInstance().getAlternatePointLon());
                Movement.getInstance().setAlternatePointLat(PreferenceUtils.getInstance().getAlternatePointLat());
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

            //避障状态
            IPerceptionManager perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance();
            perceptionManager.addPerceptionInformationListener(new PerceptionInformationListener() {
                @Override
                public void onUpdate(@NonNull PerceptionInfo information) {
                    if (information != null) {
                        if (information.getDownwardObstacleAvoidanceWorking()!=null){
                            Movement.getInstance().setDownside(information.getDownwardObstacleAvoidanceWorking() ? 1 : 0);
                        }
                        Movement.getInstance().setHorizon(information.isHorizontalObstacleAvoidanceEnabled() ? 1 : 0);
                        if (information.getUpwardObstacleAvoidanceWorking()!=null){
                            Movement.getInstance().setUpside(information.getUpwardObstacleAvoidanceWorking() ? 1 : 0);
                        }
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
                        if (newValue) {
                            Movement.getInstance().setTaskFail(false);
                            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(false);
                        }
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
                        pushFlightAttitude();
                        Movement.getInstance().setMotorsOn(newValue);
                    }
                }
            });


            //挡位
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyRemoteControllerFlightMode), this, new CommonCallbacks.KeyListener<RemoteControllerFlightMode>() {
                @Override
                public void onValueChange(@Nullable RemoteControllerFlightMode remoteControllerFlightMode, @Nullable RemoteControllerFlightMode t1) {
                    if (remoteControllerFlightMode != null) {
                        Movement.getInstance().setGear(remoteControllerFlightMode.value());
                        pushFlightAttitude();

                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(ProductKey.KeyProductType), this, new CommonCallbacks.KeyListener<ProductType>() {
                @Override
                public void onValueChange(@Nullable ProductType productType, @Nullable ProductType t1) {
                    if (t1 != null) {
                        Movement.getInstance().setProductName(t1.name());
                        pushFlightAttitude();

                    }
                }
            });

            //飞机SN号
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeySerialNumber), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setFirmware_version(t1);
                        pushFlightAttitude();

                    }
                }
            });

            //RID状态
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyRidWorkingStatusPush), this, new CommonCallbacks.KeyListener<RidWorkingStatusPushMsg>() {
                @Override
                public void onValueChange(@Nullable RidWorkingStatusPushMsg ridWorkingStatusPushMsg, @Nullable RidWorkingStatusPushMsg t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRid_state(t1.getIsRidSupport());
                        pushFlightAttitude();

                    }
                }
            });

            //rtk起飞高度(计算海拔高度使用)
            KeyManager.getInstance().listen(createKey(RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo), this, new CommonCallbacks.KeyListener<RTKTakeoffAltitudeInfo>() {
                @Override
                public void onValueChange(@Nullable RTKTakeoffAltitudeInfo rtkTakeoffAltitudeInfo, @Nullable RTKTakeoffAltitudeInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRtk_takeoff_altitude(t1.getAltitude());
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftLocation3D), this, new CommonCallbacks.KeyListener<LocationCoordinate3D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate3D oldValue, @Nullable LocationCoordinate3D newValue) {
                    if (newValue != null) {
                        //更新前端按钮状态
                        updataButtonStatus();
                        if (newValue.getAltitude() != null) {
                            Movement.getInstance().setElevation(newValue.getAltitude());
                            Movement.getInstance().setTask_height(newValue.getAltitude());
                        }

                        double distance = LocationUtils.getDistance(String.valueOf(Movement.getInstance().getHomepoint_longitude()),
                                String.valueOf(Movement.getInstance().getHomepoint_latitude()),
                                String.valueOf(newValue.getLongitude()),
                                String.valueOf(newValue.getLatitude()));
                        Movement.getInstance().setHome_distance(distance);

                        Movement.getInstance().setHeight(GpsUtils.egm96Altitude((Movement.getInstance().getRtk_takeoff_altitude() +
                                        Movement.getInstance().getElevation()),
                                newValue.getLatitude(), newValue.getLongitude()));

                        Movement.getInstance().setLatitude(newValue.getLatitude());
                        Movement.getInstance().setLongitude(newValue.getLongitude());
                        pushFlightAttitude();
                    }
                }
            });

            //水平&垂直速度
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftVelocity), this, new CommonCallbacks.KeyListener<Velocity3D>() {
                @Override
                public void onValueChange(@Nullable Velocity3D oldValue, @Nullable Velocity3D newValue) {
                    if (newValue != null) {
                        if (newValue.getZ() != null) {
                            Movement.getInstance().setVertical_speed(newValue.getZ());
                        }
                        if (newValue.getY() != null && newValue.getX() != null) {
                            Movement.getInstance().setHorizontal_speed(Math.abs(Math.sqrt((newValue.getX() * newValue.getX()) + (newValue.getY() * newValue.getY()))));
                        }
                        pushFlightAttitude();

                    }
                }
            });

            //卫星数
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGPSSatelliteCount), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setGps_number(newValue);
                        Movement.getInstance().setRtk_number(newValue);
                        pushFlightAttitude();
                    }
                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGPSSignalLevel), this, new CommonCallbacks.KeyListener<GPSSignalLevel>() {
                @Override
                public void onValueChange(@Nullable GPSSignalLevel gpsSignalLevel, @Nullable GPSSignalLevel t1) {
                    if (t1 != null) {
                        Movement.getInstance().setQuality(t1.value());
                        pushFlightAttitude();

                    }
                }
            });

            //返航位置
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyHomeLocation), this, new CommonCallbacks.KeyListener<LocationCoordinate2D>() {
                @Override
                public void onValueChange(@Nullable LocationCoordinate2D oldValue, @Nullable LocationCoordinate2D newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setHomepoint_latitude(newValue.getLatitude());
                        Movement.getInstance().setHomepoint_longitude(newValue.getLongitude());
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

            //飞行器夜航灯状态
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyLEDsSettings), this, new CommonCallbacks.KeyListener<LEDsSettings>() {
                @Override
                public void onValueChange(@Nullable LEDsSettings leDsSettings, @Nullable LEDsSettings t1) {
                    if (t1 != null) {
                        Movement.getInstance().setNight_lights_state(t1.getNavigationLEDsOn() ? 1 : 0);
                        pushFlightAttitude();

                    }
                }
            });

            //飞行器状态	(未找到类似上云api的枚举格式，先用FlightMode替代)
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyFlightMode), this, new CommonCallbacks.KeyListener<FlightMode>() {
                @Override
                public void onValueChange(@Nullable FlightMode oldValue, @Nullable FlightMode newValue) {
                    if (newValue != null) {
                        if (newValue == FlightMode.MOTOR_START) {
                            //刚起飞时，重置保存的状态
                            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
                            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
                            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);

                        }
                        Movement.getInstance().setMode_code(newValue.value());
                        Movement.getInstance().setPlaneMode(newValue.name());
                        pushFlightAttitude();
                    }
                }
            });


            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftAttitude), this, new CommonCallbacks.KeyListener<Attitude>() {
                @Override
                public void onValueChange(@Nullable Attitude attitude, @Nullable Attitude t1) {
                    if (t1 != null) {
                        Movement.getInstance().setAttitude_pitch(t1.getPitch());
                        Movement.getInstance().setAttitude_head(t1.getYaw());
                        Movement.getInstance().setTask_attitude_head(t1.getYaw());
                        Movement.getInstance().setAttitude_roll(t1.getRoll());
                    }
                    pushFlightAttitude();
                }
            });
            KeyManager.getInstance().listen(createKey(AirLinkKey.KeyUpLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setRemoteControlSignal(newValue);
                    }
                    pushFlightAttitude();

                }
            });

            KeyManager.getInstance().listen(createKey(AirLinkKey.KeyDownLinkQuality), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setPictureBiographySignal(newValue);
                    }
                    pushFlightAttitude();

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
                        //返航或降落中不允许恢复断点航线
                        if (newValue.value() == 1 || newValue.value() == 2 || newValue.value() == 3) {
                            Movement.getInstance().setWaylineCanResume(false);
                            Movement.getInstance().setCurrentWaypointIndex(0);
                        }
                        //返航后触发可入库条件
                        if (newValue.value() == 2) {
                            PreferenceUtils.getInstance().setIsNewRoute(false);
                            triggerLandOrGoHome = true;
                            Movement.getInstance().setVirtualStickQuitMission(false);

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
                    }
                    pushFlightAttitude();

                }
            });

            //总里程(单位m)
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightDistance), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTotal_flight_distance(t1);
                    }
                    pushFlightAttitude();

                }
            });

            //航迹 ID
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyTrackId), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTrack_id(t1);
                    }
                    pushFlightAttitude();
                }
            });

            //总航时
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightDuration), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTotal_flight_time(t1);
                    }
                    pushFlightAttitude();
                }
            });

            //总架次
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyAircraftTotalFlightTimes), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setTotal_flight_sorties(t1);
                    }
                    pushFlightAttitude();

                }
            });

            //失联动作
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyFailsafeAction), this, new CommonCallbacks.KeyListener<FailsafeAction>() {
                @Override
                public void onValueChange(@Nullable FailsafeAction failsafeAction, @Nullable FailsafeAction t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRc_lost_action(t1.value());
                    }
                    pushFlightAttitude();

                }
            });


            //返航高度
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyGoHomeHeight), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRth_altitude(t1);
                    }
                    pushFlightAttitude();

                }
            });

            //限高
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyHeightLimit), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setHeight_limit(t1);
                    }
                    pushFlightAttitude();

                }
            });

            //是否接近限高
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyIsNearHeightLimit), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setIs_near_height_limit(t1 ? 1 : 0);
                    }
                    pushFlightAttitude();

                }
            });

            //是否接近设定的限制距离
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyIsNearDistanceLimit), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setIs_near_distance_limit(t1 ? 1 : 0);
                        Movement.getInstance().setIs_near_area_limit(t1 ? 1 : 0);
                    }
                    pushFlightAttitude();

                }
            });

            //是否开启限远
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyDistanceLimitEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setIs_near_distance_limit(t1 ? 1 : 0);
                    }
                    pushFlightAttitude();

                }
            });


            //限远
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyDistanceLimit), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setDistance_limit(t1);
                    }
                    pushFlightAttitude();

                }
            });

            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyLowBatteryWarningThreshold), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLowBatteryWarningThreshold(t1);
                        pushFlightAttitude();

                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeySeriousLowBatteryWarningThreshold), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setSeriousLowBatteryWarningThreshold(t1);
                        pushFlightAttitude();

                    }
                }
            });
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyLowBatteryRTHEnabled), this, new CommonCallbacks.KeyListener<Boolean>() {
                @Override
                public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLowBatteryRTHEnabled(t1 ? 1 : 0);
                        pushFlightAttitude();

                    }
                }
            });
        } else {
            LogUtil.log(TAG, "初始化飞控失败" + "flight controller is null");
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
    }

    private void updataButtonStatus(){
        //手控显示条件
        if (Movement.getInstance().getGoHomeState() != 1
                && Movement.getInstance().getGoHomeState() != 2 &&
                ((!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState()) &&
                        (Movement.getInstance().getMissionType() == 3
                                || PreferenceUtils.getInstance().getMissionType() == 1)
                        && Movement.getInstance().getWaypointMissionExecuteState().equals("READY")
                        && Movement.getInstance().getElevation() > 10
                        && Movement.getInstance().getIsVirtualStickEnable() == 0)
                        || (!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState())
                        && Movement.getInstance().getWaypointMissionExecuteState().equals("INTERRUPTED")
                        && Movement.getInstance().getIsVirtualStickEnable() == 0)
                        || (!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState())
                        && Movement.getInstance().getWaypointMissionExecuteState().equals("READY") &&
                        isFlying && Movement.getInstance().getIsVirtualStickEnable() == 0))
        ) {
            Movement.getInstance().setVirtualStickStatus(true);
        } else {
            Movement.getInstance().setVirtualStickStatus(false);
        }
        //取消手控显示条件
        if (Movement.getInstance().getGoHomeState()!=1&&
                Movement.getInstance().getGoHomeState()!=2&&
                Movement.getInstance().getIsVirtualStickEnable() == 1
                && Movement.getInstance().getVirtualStickEnableReason() != 2
                && Movement.getInstance().getVirtualStickEnableReason() != 1) {
            Movement.getInstance().setCancelVirtualStickStatus(true);
        }else{
            Movement.getInstance().setCancelVirtualStickStatus(false);
        }
        //暂停按钮显示条件
        if (!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState())
                && (Movement.getInstance().getWaypointMissionExecuteState().equals("EXECUTING")
                || Movement.getInstance().getWaypointMissionExecuteState().equals("RETURN_TO_START_POINT"))
                && (PreferenceUtils.getInstance().getMissionType() == 0
                //续飞航线要到主航线才能暂停
                || (PreferenceUtils.getInstance().getMissionType() == 2&&Movement.getInstance().getCurrentWaypointIndex()>0))) {
            Movement.getInstance().setPauseMissionStautus(true);
        } else {
            Movement.getInstance().setPauseMissionStautus(false);
        }
        //继续按钮显示条件
        if ((!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState()) &&
                Movement.getInstance().getMissionType() != 3 && Movement.getInstance().isWaylineCanResume()
        )
                || (!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState())
                && Movement.getInstance().getWaypointMissionExecuteState().equals("INTERRUPTED"))
                || (!TextUtils.isEmpty(Movement.getInstance().getWaypointMissionExecuteState())
                && Movement.getInstance().getWaypointMissionExecuteState().equals("READY")
                && Movement.getInstance().isVirtualStickQuitMission())) {
            Movement.getInstance().setResumeMissionStatus(true);
        } else {
            Movement.getInstance().setResumeMissionStatus(false);
        }
    }

    //(决定飞机触发电量低的返航)
    public boolean isTriggerRoomBattrryLanding;
    private void batteryLowLanding() {
        if (isTriggerRoomBattrryLanding){
            return;
        }
        int forcedBattery = Integer.valueOf(PreferenceUtils.getInstance().getForcedBattery());
        Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                KeyBatteryPowerPercent, 0));
        String missionState = Movement.getInstance().getWaypointMissionExecuteState();
        boolean isMissionExecuting = (!TextUtils.isEmpty(missionState) &&
                (missionState.equals("EXECUTING") || missionState.equals("ENTER_WAYLINE"))
                ||(!TextUtils.isEmpty(Movement.getInstance().getPlaneMode())
                &&Movement.getInstance().getPlaneMode().equals("WAYPOINT")));
        // 获取飞行状态和航线状态
        boolean isFlyingAndBatteryOk = isFlying &&value!=null&&value< forcedBattery&&isMissionExecuting;
        boolean isAlterLandStatus=(PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()||PreferenceUtils.getInstance().getTriggerToAlternatePoint());

        if (isFlyingAndBatteryOk&& !isTriggerRoomBattrryLanding&&!isAlterLandStatus) {
            isTriggerRoomBattrryLanding = true;
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    LogUtil.log(TAG,"电量低于阈值，直接返航");
                    sendEvent2Server("电量低于阈值，强制返航");

                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendEvent2Server("电量低于阈值，返航失败");
                }
            });

        }
    }

    private void closeCabinDoor() {
        // 获取飞行状态和航线状态
        boolean isFlyingAndHeightOk = isFlying && Movement.getInstance().getElevation() > 10;
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        String missionState = Movement.getInstance().getWaypointMissionExecuteState();
        boolean isMissionExecuting = (!TextUtils.isEmpty(missionState) &&
                (missionState.equals("EXECUTING") || missionState.equals("ENTER_WAYLINE"))
                || (!TextUtils.isEmpty(Movement.getInstance().getPlaneMode()) && Movement.getInstance().getPlaneMode().equals("WAYPOINT")));

        // 当飞机在飞行，高度足够，且航线状态为EXECUTING或ENTER_WAYLINE时，触发关舱门，开启水平避障
        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint() && isFlyingAndHeightOk && !isDebugMode && isMissionExecuting && !sendCloseCabinDoorMsg) {
            sendCloseCabinDoorMsg = true;
            DockCloseManager.getInstance().sendDockCloseMsg2Server();
            PerceptionManager.getInstance().setPerceptionEnable(true);
        }
    }


    private static final int FLYING_HEIGHT_THRESHOLD = 10; // 开舱门飞行高度阈值
    private static final int DISTANCE_THRESHOLD = 500; // 返航距离阈值

    private void openCabinDoor() {
        boolean isReturningHome = goHomeExecutionState == GoHomeState.RETURNING_TO_HOME.value() ||
                goHomeExecutionState == GoHomeState.LANDING.value();
        double distance = Movement.getInstance().getHome_distance();
        double flyingHeight = Movement.getInstance().getElevation();
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        boolean isDistanceAndHeightValid = distance < DISTANCE_THRESHOLD &&
                flyingHeight > FLYING_HEIGHT_THRESHOLD && !sendOpenCabinDoorMsg;

        if (!PreferenceUtils.getInstance().getTriggerToAlternatePoint() &&
                isReturningHome && isDistanceAndHeightValid && !isDebugMode) {
            LogUtil.log(TAG, "返航距离:" + distance + "---当前高度:" + flyingHeight);
            sendOpenCabinDoorMsg = true;
            DockOpenManager.getInstance().sendDockOpenMsg2Server();

        }
    }

    //降落时将云台朝下
    private void gimbalDownwards() {
        if (goHomeExecutionState == GoHomeState.LANDING.value()
                && Movement.getInstance().getElevation() > 11
                && !isGimbalDownwards) {
            DroneHelper.getInstance().setGimbalPitchDegree();
            //将镜头设置为自动对焦
            DroneHelper.getInstance().setCameraFocusMode();
            isGimbalDownwards = true;
            PerceptionManager.getInstance().setPerceptionEnable(false);
            if (Movement.getInstance().getIsRecording()==1){
                CameraManager.getInstance().stopRecordVideo(null);
            }
        }
    }

    private boolean shouldResetGimbalAndCamera() {
        return goHomeExecutionState == GoHomeState.RETURNING_TO_HOME.value()
                && Movement.getInstance().getElevation() > 20
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


    private static final double FLYING_HEIGHT_THRESHOLD_MAX = 13.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MAX_ALTERNATE = 15.0;
    private static final double FLYING_HEIGHT_THRESHOLD_MIN = -2;
    private static final double FLYING_HEIGHT_THRESHOLD_MIN_ALTERNATE = 2.0;

    private void startVisionLanding() {
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        boolean triggerToAlternatePoint = PreferenceUtils.getInstance().getTriggerToAlternatePoint();
        boolean needTriggerApronArucoLand = PreferenceUtils.getInstance().getNeedTriggerApronArucoLand();
        boolean needTriggerAlterArucoLand = PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand();
        double thresholdMax = triggerToAlternatePoint ? FLYING_HEIGHT_THRESHOLD_MAX_ALTERNATE : FLYING_HEIGHT_THRESHOLD_MAX;

        if (isFlying && Movement.getInstance().getElevation() < thresholdMax && !isSendDetect) {
            double flyingHeight = Movement.getInstance().getElevation();
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
        //当电池电量大于40时，允许复降次数设置为10次
        if (Movement.getInstance().getBattery_a_capacity_percent() > 40) {
            AMSConfig.getInstance().setAlternateLandingTimes(10 + "");
        }

        if (PreferenceUtils.getInstance().getTriggerToAlternatePoint()) {
            LogUtil.log(TAG, "识别AlterTag:" + PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand());
            EventBus.getDefault().post(FLAG_START_DETECT_ARUCO_ALTERNATE);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(true);
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            LogUtil.log(TAG, "开始识别备降点二维码,椭球高度:" + Movement.getInstance().getElevation() + "米" + "--超声波高度:" + Movement.getInstance().getUltrasonicHeight() + "分米");
            sendEvent2Server("开始备降点视觉降落");
        } else {
            LogUtil.log(TAG, "识别ApronTag:" + PreferenceUtils.getInstance().getNeedTriggerApronArucoLand());
            EventBus.getDefault().post(FLAG_START_DETECT_ARUCO_APRON);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(true);
            LogUtil.log(TAG, "开始识别机库二维码,椭球高度:" + Movement.getInstance().getElevation() + "米" + "--超声波高度:" + Movement.getInstance().getUltrasonicHeight() + "分米");
            sendEvent2Server("开始视觉降落");
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


    /**
     * 打印这里的条件，判断是否满足降落停浆
     *
     * @return
     */
    private boolean shouldStopVisionAndLanding() {

        //记录isFlying || isMotorsOn可能桨叶在转但是飞机不在飞的情况，可能导致不触发landing
        if (PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()) {
            return !isTriggerLanding && (isFlying || isMotorsOn) && AlternateArucoDetect.getInstance().isCanLanding();
        } else {
            return !isTriggerLanding && (isFlying || isMotorsOn) && (
                    ApronArucoDetect.getInstance().isCanLanding()|| (ApronArucoDetect.getInstance().isStartFastStick()
                    && ApronArucoDetect.getInstance().getCheckThrowingErrorsTimes() > 100));
        }
    }

    private void logLandingHeight(int i) {
        LogUtil.log(TAG, "降落高度" + Movement.getInstance().getElevation() + "米---"
                + Movement.getInstance().getUltrasonicHeight() + "分米");
    }


    private void droneStorage() {
        boolean isDebugMode = PreferenceUtils.getInstance().getIsDebugMode();
        // 检查无人机是否满足降落和入库的条件
        if (triggerLandOrGoHome && !isMotorsOn && !isFlying && Movement.getInstance().getElevation() <= 0.0) {
            // 重置降落或返航的触发标志
            triggerLandOrGoHome = false;
            // 禁用触发和检测标志
            isSendDetect = false;
            isTriggerLanding = false;
            sendCloseCabinDoorMsg = false;
            ApronArucoDetect.getInstance().setCanLanding(false);
            // 发布事件，通知其他组件停止Aruco检测
            EventBus.getDefault().post(FLAG_STOP_ARUCO);
            //调试模式下不上传媒体文件 不入库
            if (!isDebugMode) {
                //这里可能也会触发备降点关舱门的逻辑
                if (!PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()) {
                    // 发送无人机入库消息到服务器********************待修改************************
                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(false);
                    DockStorageManager.getInstance().sendDockStorageMsg2Server( );
                }
                // 上传媒体文件
                SystemManager.getInstance().upLoadMedia(MqttManager.getInstance().mqttAndroidClient);
            }
            // 避免在下次起飞时触发视觉识别
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);

        }
    }

    //返航
    public void startGoHome(MessageDown message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyFlightMode));

        if (Movement.getInstance().isPlaneWing() && Movement.getInstance().getHome_distance() < 20
                && Movement.getInstance().getElevation() < 88
                && Movement.getInstance().getBattery_a_capacity_percent() > 25
                && ((flightMode != null && flightMode == FlightMode.WAYPOINT) ||
                (flightMode != null && flightMode == FlightMode.AUTO_TAKE_OFF))) {
            WayLineExecutingInterruptManager.getInstance().onExecutingInterruptToDo();
        } else {
            Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
            if (isConnect != null && isConnect) {
                KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                    @Override
                    public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server( message);
                    }
                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                            sendFailMsg2Server( message, "返航执行失败:" + getIDJIErrorMsg(error));
                    }
                });
            } else {
                sendFailMsg2Server( message, "返航执行失败:飞控未连接");
            }
        }

    }

    //取消返航
    public void stopGoHome(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    sendMsg2Server( message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server( message, "取消返航执行失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            sendFailMsg2Server( message, "飞控未连接");
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


    //开启低速转浆
    public void startPropellerRotation(MessageDown message) {
        KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyPropellerRotation), PropellerRotationCommand.LOW_SPEED_ROTATION, new CommonCallbacks.CompletionCallbackWithParam<PropellerRotationCommandResult>() {
            @Override
            public void onSuccess(PropellerRotationCommandResult propellerRotationCommandResult) {
                sendMsg2Server(message);
                if (propellerRotationCommandResult.getStatus() == PropellerRotationStatus.ALL_MOTOR_IN_LOW_SPEED_ROTATION) {
                    Movement.getInstance().setPropellerRotation(true);
                }
                LogUtil.log(TAG, "开始低速转浆结果:" + new Gson().toJson(propellerRotationCommandResult));
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "开始低速转浆失败:" + getIDJIErrorMsg(error));
                sendFailMsg2Server(message, "开始低速转浆失败:" + getIDJIErrorMsg(error));
            }
        });
    }

    //停止低速转浆
    public void stopPropellerRotation(MessageDown message) {
        KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyPropellerRotation),
                PropellerRotationCommand.EXIT_LOW_SPEED_ROTATION, new CommonCallbacks.CompletionCallbackWithParam<PropellerRotationCommandResult>() {
                    @Override
                    public void onSuccess(PropellerRotationCommandResult propellerRotationCommandResult) {
                        sendMsg2Server(message);
                        if (propellerRotationCommandResult.getStatus() == PropellerRotationStatus.NONE_MOTOR_IN_LOW_SPEED_ROTATION) {
                            Movement.getInstance().setPropellerRotation(false);
                        }
                        LogUtil.log(TAG, "停止低速转浆结果:" + new Gson().toJson(propellerRotationCommandResult));
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "停止低速转浆失败:" + new Gson().toJson(error));
                        sendFailMsg2Server(message, "停止低速转浆失败:" + getIDJIErrorMsg(error));
                    }
                });
    }

    public int findIndexInWaypoints(int indexInRouteWaypoints) {
        // 边界检查
        if (indexInRouteWaypoints < 0 || indexInRouteWaypoints >= CurrentWayline.getInstance().getRouteWaypoints().size()) {
            return -1;
        }
        // 获取 routeWaypoints 中的指定元素
        WaylineExecuteWaypoint waylineExecuteWaypoint = CurrentWayline.getInstance().getRouteWaypoints().get(indexInRouteWaypoints);
        // 在 waypoints 中查找该元素的索引
        return CurrentWayline.getInstance().getWaypoints().indexOf(waylineExecuteWaypoint);
    }

}
