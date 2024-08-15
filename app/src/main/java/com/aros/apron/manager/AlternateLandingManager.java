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
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MissionPoint;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DomParserKML;
import com.aros.apron.tools.DomParserWPML;
import com.aros.apron.tools.LogUtil;
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
 * 备降点
 */
public class AlternateLandingManager extends BaseManager {

    MqttAndroidClient mqttClient;
    private boolean isRemoteControllerFlightModeChange;

    private AlternateLandingManager() {
    }

    private static class AlternateLandingHolder {
        private static final AlternateLandingManager INSTANCE = new AlternateLandingManager();
    }

    public static AlternateLandingManager getInstance() {
        return AlternateLandingHolder.INSTANCE;
    }

    public void initAlterLandingInfo(MqttAndroidClient mqttAndroidClient) {
        this.mqttClient = mqttAndroidClient;
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode), this, new CommonCallbacks.KeyListener<RemoteControllerFlightMode>() {
            @Override
            public void onValueChange(@Nullable RemoteControllerFlightMode remoteControllerFlightMode, @Nullable RemoteControllerFlightMode t1) {
                if (t1 != null) {
                    LogUtil.log(TAG, "监听到挡位切换:" + t1.name());
                    if (t1 != RemoteControllerFlightMode.P && t1 != RemoteControllerFlightMode.F) {
                        isRemoteControllerFlightModeChange = true;
                    }
                }
            }
        });
    }

    public void startTaskProcess(MQMessage message) {
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
                    if (message!=null){
                        sendMsg2Server(mqttClient,message,"挡位不正确,不触发去备降点");
                    }
                    sendMissionExecuteEvents(mqttClient, "挡位不正确,不触发去备降点");
                    LogUtil.log(TAG, "检测到挡位不正确,不触发去备降点");
                }
            } else {
                if (message!=null){
                    sendMsg2Server(mqttClient,message,"飞机未起飞,不触发去备降点");
                }
                sendMissionExecuteEvents(mqttClient, "飞机未起飞,不触发去备降点");
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
                            toAlternatePoint(message);
                            sendMissionExecuteEvents(mqttClient, "取消返航:去备降点");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消返航失败:" + new Gson().toJson(error));
                            toAlternatePoint(message);
                            sendMissionExecuteEvents(mqttClient, "取消返航失败:去备降点");

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
                            sendMissionExecuteEvents(mqttClient, "终止任务成功:去备降点");
                            toAlternatePoint(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "终止任务失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents(mqttClient, "终止任务失败:去备降点");
                            toAlternatePoint(message);
                        }
                    });
                    break;
                case AUTO_LANDING:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消降落成功");
                            sendMissionExecuteEvents(mqttClient, "取消降落成功:去备降点");
                            toAlternatePoint(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消降落失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents(mqttClient, "取消降落失败:去备降点");
                            toAlternatePoint(message);
                        }
                    });
                    break;
                case VIRTUAL_STICK:
//                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            LogUtil.log(TAG, "控制权取消成功");
                    toAlternatePoint(message);
//                        }
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            LogUtil.log(TAG, "控制权取消失败:" + new Gson().toJson(error));
//                            toAlternatePoint();
//                        }
//                    });
                    break;
                default:
                    toAlternatePoint(message);
                    break;
            }
        }
    }


    public void toAlternatePoint(MQMessage message) {
        if (Movement.getInstance().getFlyingHeight() < 10) {
            LogUtil.log(TAG, "toAlternatePoint:" + "高度低于10米,拉高");
            sendMissionExecuteEvents(mqttClient, "拉高...");
            raisesDrone(message);
        } else {
            sendMissionExecuteEvents(mqttClient, "开始创建备降任务");
            LogUtil.log(TAG, "toAlternatePoint:" + "高度高于10米,创建备降任务");
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
                        LogUtil.log(TAG, "备降拉高,控制权获取成功");
                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
                        pullUp(message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "备降拉高,控制权获取失败,直接上传备降航线:" + error.description());
                        creatMissionAndUpload(message);
                    }
                });
            }
        } else {
            LogUtil.log(TAG, "备降拉高,飞控未连接");
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
                            LogUtil.log(TAG, "到达10米,开始上传备降点航线");
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
        missionPoint.setLat(Double.parseDouble(Movement.getInstance().getCurrentLatitude()));
        missionPoint.setLng(Double.parseDouble(Movement.getInstance().getCurrentLongitude()));
        missionPoint.setSpeed(8.0);
        missionPoint.setExecuteHeight(Movement.getInstance().getFlyingHeight()
                > Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight())
                ? Movement.getInstance().getFlyingHeight() :
                Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));

        // 创建第二个 MissionPoint 对象
        MissionPoint missionPoint1 = new MissionPoint();
        missionPoint1.setLat(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLat()));
        missionPoint1.setLng(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLon()));
        LogUtil.log(TAG,"备降点经纬度:"+Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLat())+"/"+Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLon()));
        missionPoint1.setSpeed(7.0);
        missionPoint1.setExecuteHeight(Movement.getInstance().getFlyingHeight()
                > Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointHeight())
                ? Movement.getInstance().getFlyingHeight() - 1 :
                Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointHeight()));

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

        LogUtil.log(TAG, "当前高度:" + Movement.getInstance().getFlyingHeight()
                + "---飞往备降点高度:" + Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointHeight())
                + "---航线安全起飞高度:" + Float.parseFloat(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));

        sendMissionExecuteEvents(mqttClient, "开始生成备降点航线");

        // 生成xml文件
        File file1 = new File(
                getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz");
        if (!file1.exists()) {
            if (file1.mkdirs()) {
                LogUtil.log(TAG, "生成备降航线成功");
                sendMissionExecuteEvents(mqttClient, "生成备降路线文件成功");

            } else {
                LogUtil.log(TAG, "生成备降航线失败");
                sendMissionExecuteEvents(mqttClient, "生成备降航线失败");
if (message!=null){
    sendMsg2Server(mqttClient,message,"生成备降航线失败");
}
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
            LogUtil.log(TAG, "备降航线压缩异常：" + e.toString());
            sendMissionExecuteEvents(mqttClient, "备降航线压缩异常：" + e.toString());
            if (message!=null){
                sendMsg2Server(mqttClient,message,"备降航线压缩异常");
            }
            throw new RuntimeException(e);
        }

        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
        missionManager.pushKMZFileToAircraft(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "alternate.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
            @Override
            public void onProgressUpdate(Double aDouble) {
                LogUtil.log(TAG, "备降点航线上传进度:" + aDouble + "%");
                sendMissionExecuteEvents(mqttClient, "备降点航线上传进度:" + aDouble + "%");

            }

            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "备降点航线上传成功");
                sendMissionExecuteEvents(mqttClient, "备降点航线上传成功");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        missionManager.startMission("alternate", new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                PreferenceUtils.getInstance().setTriggerToAlternatePoint(true);
                                LogUtil.log(TAG, "开始飞往备降点");
                                sendMissionExecuteEvents(mqttClient, "开始飞往备降点");
                                //设置为未开始识别二维码状态
                                FlightManager.getInstance().setSendDetect(false);
                                EventBus.getDefault().post(FLAG_STOP_ARUCO);

                                if (message!=null){
                                    sendMsg2Server(mqttClient,message);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError idjiError) {
                                PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                                LogUtil.log(TAG, "飞往备降点失败:" + new Gson().toJson(idjiError));
                                sendMissionExecuteEvents(mqttClient, "飞往备降点失败:" + new Gson().toJson(idjiError));
                                if (message!=null){
                                    sendMsg2Server(mqttClient,message,"飞往备降点失败:" + new Gson().toJson(idjiError));
                                }
                            }
                        });
                    }
                }, 1000);
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "备降航线上传失败:" + new Gson().toJson(idjiError));
                PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
                sendMissionExecuteEvents(mqttClient, "备降航线上传失败:" + new Gson().toJson(idjiError));
                if (message!=null){
                    sendMsg2Server(mqttClient,message,"备降航线上传失败:" + new Gson().toJson(idjiError));
                }
            }
        });
    }

    public void setAlternatePoint(MqttAndroidClient client, MQMessage message){
        if (message!=null&&!TextUtils.isEmpty(message.getAlternatePointLat())&&!TextUtils.isEmpty(message.getAlternatePointLon())){
            PreferenceUtils.getInstance().setAlternatePointLat(message.getAlternatePointLat());
            PreferenceUtils.getInstance().setAlternatePointLon(message.getAlternatePointLon());
            Movement.getInstance().setAlternatePointLon(PreferenceUtils.getInstance().getAlternatePointLon());
            Movement.getInstance().setAlternatePointLat(PreferenceUtils.getInstance().getAlternatePointLat());
            sendMsg2Server(client,message);
        }else{
            sendMsg2Server(client,message,"设置备降点失败:参数有误");
        }
    }
}
