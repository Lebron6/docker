package com.aros.apron.manager;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.aros.apron.manager.FlightManager.FLAG_STOP_ARUCO;
import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.FlightMission;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MissionPoint;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DomParserKML;
import com.aros.apron.tools.DomParserWPML;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.ZipUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
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
 * 异地降落
 */
public class OffSiteLandingManager extends BaseManager {

    private int goHomeHeight;

    private OffSiteLandingManager() {
    }

    private static class OffSiteLandingHolder {
        private static final OffSiteLandingManager INSTANCE = new OffSiteLandingManager();
    }

    public static OffSiteLandingManager getInstance() {
        return OffSiteLandingHolder.INSTANCE;
    }

    public void initOffSiteLandingInfo() {
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), this, new CommonCallbacks.KeyListener<Integer>() {
            @Override
            public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                if (t1 != null&&t1>=0) {
                    goHomeHeight = t1;
                    LogUtil.log(TAG,"返航高度:"+goHomeHeight);
                    Movement.getInstance().setGoHomeHeight(goHomeHeight);
                }else{
                    goHomeHeight=80;
                    LogUtil.log(TAG,"返航高度监听失败:"+goHomeHeight);
                }
            }
        });
    }

    public void startTaskProcess(MQMessage message) {
        if (TextUtils.isEmpty(message.getOffSitePointLat())||TextUtils.isEmpty(message.getOffSitePointLon())){
            sendMissionExecuteEvents( "异地降落点经纬度有误");
            LogUtil.log(TAG, "异地降落点经纬度有误,不触发异地降落");
            return;
        }
        PerceptionManager.getInstance().setPerceptionEnable(false);
        DockOpenManager.getInstance().sendDockOpenMsg2Server(MqttManager.getInstance().mqttAndroidClient);
        //飞往异地降落点,关闭视觉识别
        EventBus.getDefault().post(FLAG_STOP_ARUCO);
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
                        sendMsg2Server( message, "挡位不正确,不触发异地降落");
                    }
                    sendMissionExecuteEvents( "挡位不正确,不触发异地降落");
                    LogUtil.log(TAG, "检测到挡位不正确,不触发异地降落");
                }
            } else {
                if (message != null) {
                    sendMsg2Server( message, "飞机未起飞,不触发异地降落");
                }
                sendMissionExecuteEvents( "飞机未起飞,不触发异地降落");
            }
        }
    }

    private void checkDroneState(MQMessage message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
        if (flightMode != null) {
            switch (flightMode) {
                case GO_HOME:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消返航成功");
                            toOffsitePoint(message);
                            sendMissionExecuteEvents( "取消返航:触发异地降落");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消返航失败:" + new Gson().toJson(error));
                            toOffsitePoint(message);
                            sendMissionExecuteEvents( "取消返航失败:不触发异地降落");

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
                            sendMissionExecuteEvents( "终止任务成功:去异地降落");
                            toOffsitePoint(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "终止任务失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents( "终止任务失败:去异地降落");
                            toOffsitePoint(message);
                        }
                    });
                    break;
                case AUTO_LANDING:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消降落成功");
                            sendMissionExecuteEvents( "取消降落成功:去异地降落");
                            toOffsitePoint(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消降落失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents( "取消降落失败:去异地降落");
                            toOffsitePoint(message);
                        }
                    });
                    break;
                case VIRTUAL_STICK:
//                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            LogUtil.log(TAG, "控制权取消成功");
                    toOffsitePoint(message);
//                        }
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG, "控制权取消失败:" + new Gson().toJson(error));
//                            toOffsitePoint();
//                        }
//                    });
                    break;
                default:
                    toOffsitePoint(message);
                    break;
            }
        }
    }


    public void toOffsitePoint(MQMessage message) {
        if (Movement.getInstance().getFlyingHeight() < 10) {
            LogUtil.log(TAG, "高度低于10米,拉高");
            sendMissionExecuteEvents( "拉高去异地降落点...");
            raisesDrone(message);
        } else {
            sendMissionExecuteEvents( "开始创建异地降落任务");
            LogUtil.log(TAG, "高度高于10米,创建异地降落任务");
            creatMissionAndUpload(message);
        }
    }


    public void raisesDrone(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Boolean isVirtualStickControlModeEnabled = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyVirtualStickControlModeEnabled));
            if (isVirtualStickControlModeEnabled != null && isVirtualStickControlModeEnabled) {
                pullUp(message);
            } else {
                VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "异地降落拉高,控制权获取成功");
                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
                        pullUp(message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "异地降落拉高,控制权获取失败,直接上传异地降落航线:" + error.description());
                        creatMissionAndUpload(message);
                    }
                });
            }
        } else {
            LogUtil.log(TAG, "异地降落拉高,飞控未连接");
        }
    }

    Handler handler = new Handler(Looper.getMainLooper());

    public void pullUp(MQMessage message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (Movement.getInstance().getFlyingHeight() < 10) {
                    sendVirtualStickAdvancedParam();
                    handler.postDelayed(this, 200);
                } else {
                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "到达10米,开始上传异地降落航线");
                            creatMissionAndUpload(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            LogUtil.log(TAG, "到达10米,取消虚拟摇杆控制失败:" + new Gson().toJson(idjiError));
                            creatMissionAndUpload(message);
                        }
                    });
                    handler.removeCallbacks(this);
                }
            }
        };
        // 开始循环
        handler.post(runnable);
    }

    VirtualStickFlightControlParam param;

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

    public void creatMissionAndUpload(MQMessage message) {
        // 创建第一个 MissionPoint 对象
        MissionPoint missionPoint = new MissionPoint();
        missionPoint.setLat(Movement.getInstance().getCurrentLatitude());
        missionPoint.setLng(Movement.getInstance().getCurrentLongitude());
        missionPoint.setSpeed(8.0);
        missionPoint.setExecuteHeight(Movement.getInstance().getFlyingHeight()
                > goHomeHeight
                ? Movement.getInstance().getFlyingHeight()-1 :
                goHomeHeight);

        // 创建第二个 MissionPoint 对象
        MissionPoint missionPoint1 = new MissionPoint();
        missionPoint1.setLat(message.getOffSitePointLat());
        missionPoint1.setLng(message.getOffSitePointLon());
        missionPoint1.setSpeed(7.0);
        missionPoint1.setExecuteHeight(Movement.getInstance().getFlyingHeight()
                > goHomeHeight
                ? Movement.getInstance().getFlyingHeight() -1 :
                goHomeHeight);

        // 创建一个 MissionPoint 列表
        List<MissionPoint> missionPoints = new ArrayList<>();
        missionPoints.add(missionPoint);
        missionPoints.add(missionPoint1);

        // 创建 FlightMission 对象并设置其属性
        FlightMission flightMission = new FlightMission();
        flightMission.setPoints(missionPoints);
        flightMission.setMissionId(2);
        flightMission.setTakeOffSecurityHeight(Float.parseFloat(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));
        flightMission.setSpeed(15.0);

        sendMissionExecuteEvents( "开始生成异地降落航线");

        // 生成xml文件
        File file1 = new File(
                getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz");
        if (!file1.exists()) {
            if (file1.mkdirs()) {
                LogUtil.log(TAG, "生成异地降落航线成功");
                sendMissionExecuteEvents( "生成异地降落路线文件成功");

            } else {
                LogUtil.log(TAG, "生成异地降落航线失败");
                sendMissionExecuteEvents( "生成异地降落航线失败");
                if (message != null) {
                    sendMsg2Server( message, "生成异地降落航线失败");
                }
            }
        }
        DomParserKML domParserKML = new DomParserKML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/template.kml");
        domParserKML.createKml(flightMission);

        DomParserWPML domParserWPML = new DomParserWPML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/waylines.wpml");
        domParserWPML.createWpml(flightMission);

        File kmzFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "offsite.kmz");
        kmzFile.getParentFile().mkdirs();

        try {
            ZipUtil.zip(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + "/wpmz", getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "offsite.kmz");
        } catch (IOException e) {
            LogUtil.log(TAG, "异地降落航线压缩异常：" + e.toString());
            sendMissionExecuteEvents( "异地降落任务生成异常");
            if (message != null) {
                sendMsg2Server( message, "异地降落任务生成异常");
            }
            throw new RuntimeException(e);
        }

        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
        missionManager.pushKMZFileToAircraft(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "offsite.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
            @Override
            public void onProgressUpdate(Double aDouble) {
                LogUtil.log(TAG, "异地降落航线上传进度:" + aDouble + "%");
                sendMissionExecuteEvents( "异地降落任务上传中:" + aDouble + "%");

            }

            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "异地降落航线上传成功");
                sendMissionExecuteEvents( "异地降落航线上传成功");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        missionManager.startMission("offsite", new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {

                                LogUtil.log(TAG, "开始异地降落航线");
                                sendMissionExecuteEvents( "开始异地降落航线");
                                //设置为未开始识别二维码状态
                                FlightManager.getInstance().setSendDetect(false);
                                EventBus.getDefault().post(FLAG_STOP_ARUCO);

                                if (message != null) {
                                    sendMsg2Server( message);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError idjiError) {
                                LogUtil.log(TAG, "飞往异地降落点失败:" + new Gson().toJson(idjiError));
                                sendMissionExecuteEvents( "飞往异地降落点失败");
                                if (message != null) {
                                    sendMsg2Server( message, "飞往异地降落点失败");
                                }
                            }
                        });
                    }
                }, 1000);
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "异地降落航线上传失败:" + new Gson().toJson(idjiError));
                sendMissionExecuteEvents( "异地降落航线上传失败");
                if (message != null) {
                    sendMsg2Server( message, "异地降落航线上传失败:"  + getIDJIErrorMsg(idjiError));
                }
            }
        });
    }

}
