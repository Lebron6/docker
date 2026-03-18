package com.aros.apron.manager;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.aros.apron.manager.FlightManager.FLAG_STOP_ARUCO;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.FlightMission;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.MissionPoint;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DomParserKML;
import com.aros.apron.tools.DomParserWPML;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.ZipUtil;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.RemoteControllerFlightMode;
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.interfaces.IWaypointMissionManager;

/**
 * 备降点
 */
public class AlternateLandingManager extends BaseManager {

    Handler handler = new Handler(Looper.getMainLooper());
    VirtualStickFlightControlParam param;
    private boolean isRemoteControllerFlightModeChange;

    private AlternateLandingManager() {
    }

    public static AlternateLandingManager getInstance() {
        return AlternateLandingHolder.INSTANCE;
    }

    public void initAlterLandingInfo() {
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode), this, new CommonCallbacks.KeyListener<RemoteControllerFlightMode>() {
            @Override
            public void onValueChange(@Nullable RemoteControllerFlightMode remoteControllerFlightMode, @Nullable RemoteControllerFlightMode t1) {
                if (t1 != null) {
                    LogUtil.log(TAG, "监听到挡位切换:" + t1.name());
                    if (t1 != RemoteControllerFlightMode.P && t1 != RemoteControllerFlightMode.F) {
                        isRemoteControllerFlightModeChange = true;
                        PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
                        PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
                        PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                    }
                }
            }
        });
    }

    public void startTaskProcess(MessageDown message) {
        //飞往备降点,关闭视觉识别
        EventBus.getDefault().post(FLAG_STOP_ARUCO);
        if (isRemoteControllerFlightModeChange) {
            LogUtil.log(TAG, "挡位切换过:不触发去备降点");
            return;
        }
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            Boolean areMotorOn = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn));
            Boolean isFlying = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyIsFlying));
            if ((areMotorOn != null && areMotorOn) && (isFlying != null && isFlying)) {
                RemoteControllerFlightMode remoteControllerFlightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
                if (remoteControllerFlightMode != null && remoteControllerFlightMode == RemoteControllerFlightMode.P) {
                    checkDroneState(message);
                } else {
                    if (message != null) {
                        sendEvent2Server("挡位不正确,不触发去备降点",2);
                    }
                }
            } else {
                if (message != null) {
                    sendEvent2Server( "飞机未起飞,不触发去备降点",2);
                }
            }
        }
    }

    private void checkDroneState(MessageDown message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
        if (flightMode != null) {
            switch (flightMode) {
                case GO_HOME:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            toAlternatePoint();
                            sendEvent2Server( "取消返航:去备降点",1);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消返航失败:" + new Gson().toJson(error));
                            toAlternatePoint();
                            sendEvent2Server( "取消返航失败:" + new Gson().toJson(error),2);

                        }
                    });
                    break;
                case WAYPOINT:
                    IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
                    missionManager.stopMission(TextUtils.isEmpty(Movement.getInstance().getMissionName())
                            ? "aros" : Movement.getInstance().getMissionName(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "终止任务成功");
                            sendEvent2Server( "终止任务成功:去备降点",1);
                            toAlternatePoint();
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "终止任务失败:" + new Gson().toJson(error));
                            sendEvent2Server( "终止任务失败:去备降点",2);
                            toAlternatePoint();
                        }
                    });
                    break;
                case AUTO_LANDING:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendEvent2Server( "取消降落成功:去备降点",1);
                            toAlternatePoint();
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendEvent2Server( "取消降落失败:去备降点",2);
                            toAlternatePoint();
                        }
                    });
                    break;
                case VIRTUAL_STICK:
//                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            LogUtil.log(TAG, "控制权取消成功");
                    toAlternatePoint();
//                        }
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG, "控制权取消失败:" + new Gson().toJson(error));
//                            toAlternatePoint();
//                        }
//                    });
                    break;
                default:
                    toAlternatePoint();
                    break;
            }
        }
    }

    public void toAlternatePoint() {
        if (Movement.getInstance().getElevation() < 10) {
            sendEvent2Server( "正在拉高去备降点",1);
            raisesDrone();
        } else {
            sendEvent2Server( "开始创建备降任务",1);
            creatMissionAndUpload();
        }
    }

    public void raisesDrone() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Boolean isVirtualStickControlModeEnabled = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyVirtualStickControlModeEnabled));
            if (isVirtualStickControlModeEnabled != null && isVirtualStickControlModeEnabled) {
                pullUp();
            } else {
                VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "备降拉高,控制权获取成功");
                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
                        pullUp();
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "备降拉高,控制权获取失败,直接上传备降航线:" + error.description());
                        creatMissionAndUpload();
                    }
                });
            }
        } else {
            LogUtil.log(TAG, "备降拉高,飞控未连接");
        }
    }

    public void pullUp() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (Movement.getInstance().getElevation() < 10) {
                    sendVirtualStickAdvancedParam();
                    handler.postDelayed(this, 200);
                } else {
                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "到达10米,开始上传备降点航线");
                            creatMissionAndUpload();
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            LogUtil.log(TAG, "到达10米,取消虚拟摇杆控制失败:" + new Gson().toJson(idjiError));
                            creatMissionAndUpload();
                        }
                    });
                    handler.removeCallbacks(this);
                }
            }
        };
        // 开始循环
        handler.post(runnable);

    }

    //飞行器虚拟摇杆
    public void sendVirtualStickAdvancedParam() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (param == null) {
                param = new VirtualStickFlightControlParam();
                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                param.setVerticalControlMode(VerticalControlMode.VELOCITY);
                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            }
            param.setPitch(0.0);//左右
            param.setRoll(0.0);//前后
            param.setYaw(0.0);//旋转
            param.setVerticalThrottle(4.0);//上下
            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
        }
    }

    public void creatMissionAndUpload() {
        // 创建第一个 MissionPoint 对象
        MissionPoint missionPoint = new MissionPoint();
        missionPoint.setLat(String.valueOf(Movement.getInstance().getLatitude()));
        missionPoint.setLng(String.valueOf(Movement.getInstance().getLongitude()));
        missionPoint.setSpeed(8.0);
        missionPoint.setExecuteHeight(Movement.getInstance().getElevation()
                > Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight())
                ? Movement.getInstance().getElevation() :
                Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));

        // 创建第二个 MissionPoint 对象
        MissionPoint missionPoint1 = new MissionPoint();
        missionPoint1.setLat(PreferenceUtils.getInstance().getAlternatePointLat());
        missionPoint1.setLng(PreferenceUtils.getInstance().getAlternatePointLon());
        LogUtil.log(TAG, "备降点经纬度:" + PreferenceUtils.getInstance().getAlternatePointLat() +
                "/" + PreferenceUtils.getInstance().getAlternatePointLon());
        missionPoint1.setSpeed(7.0);
        missionPoint1.setExecuteHeight(Movement.getInstance().getElevation()
                > Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight())
                ? Movement.getInstance().getElevation() - 1 :
                Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));

        // 创建一个 MissionPoint 列表
        List<MissionPoint> missionPoints = new ArrayList<>();
        missionPoints.add(missionPoint);
        missionPoints.add(missionPoint1);

        // 创建 FlightMission 对象并设置其属性
        FlightMission flightMission = new FlightMission();
        flightMission.setPoints(missionPoints);
        flightMission.setMissionId(2);
        flightMission.setFinishAction("autoLand");
        flightMission.setTakeOffSecurityHeight(Float.parseFloat(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));
        flightMission.setSpeed(15.0);

        LogUtil.log(TAG, "当前高度:" + Movement.getInstance().getElevation()
                + "---飞往备降点高度:" + Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight())
                + "---航线安全起飞高度:" + Float.parseFloat(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));

        sendEvent2Server( "开始生成备降点航线",1);

        // 生成xml文件
        File file1 = new File(
                getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz");
        if (!file1.exists()) {
            if (file1.mkdirs()) {
                LogUtil.log(TAG, "生成备降航线成功");
                sendEvent2Server( "生成备降路线文件成功",1);

            } else {
                LogUtil.log(TAG, "生成备降航线失败");
                sendEvent2Server( "生成备降航线失败",2);

            }
        }
        DomParserKML domParserKML = new DomParserKML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/template.kml");
        domParserKML.createKml(flightMission);

        DomParserWPML domParserWPML = new DomParserWPML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/waylines.wpml");
        domParserWPML.createWpml(flightMission);

        File kmzFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "alternate.kmz");
        kmzFile.getParentFile().mkdirs();

        try {
            ZipUtil.zip(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + "/wpmz", getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "alternate.kmz");
        } catch (IOException e) {
            sendEvent2Server( "备降任务生成异常",2);
            throw new RuntimeException(e);
        }

        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
        missionManager.pushKMZFileToAircraft(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "alternate.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
            @Override
            public void onProgressUpdate(Double aDouble) {
                LogUtil.log(TAG, "备降点航线上传进度:" + aDouble + "%");
                sendEvent2Server( "备降任务上传中:" + aDouble + "%",1);

            }

            @Override
            public void onSuccess() {
                sendEvent2Server( "备降点航线上传成功",1);
                if (PreferenceUtils.getInstance().getHaveRTK() && !(Movement.getInstance().getIs_fixed()==2)) {
                    RTKManager.getInstance().enableRtk(false);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        missionManager.startMission("alternate", new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                PreferenceUtils.getInstance().setTriggerToAlternatePoint(true);
                                PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
                                PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
                                sendEvent2Server( "开始飞往备降点",1);
                                //设置为未开始识别二维码状态
                                FlightManager.getInstance().setSendDetect(false);
                                EventBus.getDefault().post(FLAG_STOP_ARUCO);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError idjiError) {
                                PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                                LogUtil.log(TAG, "飞往备降点失败:" + new Gson().toJson(idjiError));
                                sendEvent2Server( "飞往备降点失败",2);
                            }
                        });
                    }
                }, 1000);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                sendEvent2Server( "备降航线上传失败",2);
            }
        });
    }

    private static class AlternateLandingHolder {
        private static final AlternateLandingManager INSTANCE = new AlternateLandingManager();
    }
}
