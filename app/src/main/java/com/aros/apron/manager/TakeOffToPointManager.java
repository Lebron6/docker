package com.aros.apron.manager;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.aros.apron.app.ApronApp;
import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.ErrorCode;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.FlightMission;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.MissionPoint;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DomParserKML;
import com.aros.apron.tools.DomParserWPML;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.RestartAPPTool;
import com.aros.apron.tools.ZipUtil;
import com.dji.wpmzsdk.common.data.KMZInfo;
import com.dji.wpmzsdk.manager.WPMZManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.RemoteControllerFlightMode;
import dji.sdk.wpmz.value.mission.Wayline;
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint;
import dji.sdk.wpmz.value.mission.WaylineWaylinesParseInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;


public class TakeOffToPointManager extends BaseManager {

    final Handler mainHandler = new Handler(Looper.getMainLooper());


    private TakeOffToPointManager() {
    }

    private static class TakeOffToPointHolder {
        private static final TakeOffToPointManager INSTANCE = new TakeOffToPointManager();
    }

    public static TakeOffToPointManager getInstance() {
        return TakeOffToPointHolder.INSTANCE;
    }

    public boolean isReceiverMission = false;

    //收到一键起飞航线
    public void taskExecute(MessageDown message) {
        PreferenceUtils.getInstance().setMissionType(1);
        PreferenceUtils.getInstance().setFlightId(message.getData().getFlight_id());
        PreferenceUtils.getInstance().setAlternatePointLon(message.getData().getAlternate_land_point().getLongitude() + "");
        PreferenceUtils.getInstance().setAlternatePointLat(message.getData().getAlternate_land_point().getLatitude() + "");
        PreferenceUtils.getInstance().setAlternatePointSecurityHeight(
                message.getData().getAlternate_land_point().getSafe_land_height() + "");
        Movement.getInstance().setTask_current_step(5);


        //1.检查图传是否连接
        checkVtxWithDelay(() -> {
            //避免重复执行
            if (isReceiverMission == false) {
                isReceiverMission = true;
            }
            //2.回复收到指令
            sendMsg2Server(message);
            //3.检查飞机状态（不满足条件直接taskFail入库）
            boolean statusOk = verifyAircraftStatus(message);
            //4.信号收敛(等待GPS搜星)
            if (statusOk) {
                verifyGpsAndMissionState(message);
            }
        });
    }


    /**
     * 1.校验飞机状态
     */
    private boolean verifyAircraftStatus(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            //1.若当前处于虚拟摇杆状态，取消虚拟摇杆
            if (Movement.getInstance().getIsVirtualStickEnable() == 1) {
                StickManager.getInstance().disableVirtualStick(null);
            }
            //2.关闭避障
            PerceptionManager.getInstance().setPerceptionEnable(false);
            //3.清空sd卡
            CameraManager.getInstance().formatStorage(null);
            //4.返航或降落状态无法执行航线
            if (Movement.getInstance().getGoHomeState() == 1
                    || Movement.getInstance().getGoHomeState() == 2) {
                sendFailMsg2Server(message, "返航中,无法执行航线任务");
                return false;
            }
            //5.检查航线备降点参数
            if (message.getData().getAlternate_land_point() == null) {
                sendEvent2Server("备降点参数异常",2);
                DockStorageManager.getInstance().sendDockStorageMsg2Server();
                return false;
            }
            //6.检查航线参数
            if (message.getData().getFile() == null) {
                sendEvent2Server("航线参数异常",2);
                DockStorageManager.getInstance().sendDockStorageMsg2Server();
                return false;
            }
            //7.检查电池电量
            Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                    KeyBatteryPowerPercent, 0));
            if (value != null && value < Integer.parseInt(PreferenceUtils.getInstance().getMinumumBattery())) {
                sendEvent2Server("任务执行失败,电量过低",2);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(ErrorCode.DEVICE_BATTERY_LEVEL_TOO_LOW);
                return false;
            }
            RemoteControllerFlightMode remoteControllerFlightMode =
                    KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
            if (remoteControllerFlightMode != null && remoteControllerFlightMode != RemoteControllerFlightMode.P) {
                sendEvent2Server("任务执行失败,请将遥控器切换为P/N挡",2);
                DockStorageManager.getInstance().sendDockStorageMsg2Server();
                return false;
            }
            return true;
        } else {
            sendFailMsg2Server(message, "无人机未连接");
            return false;

        }
    }

    private int verifyGpsAndMissionStateTimes;
    private boolean verifyGpsAndMissionStateSuccess;
    private final int maxRetries = 100;

    /**
     * 3.GPS信号校验
     */
    private void verifyGpsAndMissionState(MessageDown message) {
        int missionStateCode = Movement.getInstance().getMissionStateCode();
        String planeMessage = Movement.getInstance().getPlaneMessage();
        int quality = Movement.getInstance().getQuality();
        boolean isMissionStateValid = (missionStateCode == 2 || missionStateCode == 0 || missionStateCode == 7);
        boolean isPlaneMessageValid = !TextUtils.isEmpty(planeMessage) && !planeMessage.equals("无法起飞");
        boolean isGpsQualityValid = (quality == 4 || quality == 5 || quality == 10);
        if (isMissionStateValid && isPlaneMessageValid && isGpsQualityValid) {
            //5.生成航线
            toGenerateKMZFile(message);
            verifyGpsAndMissionStateSuccess = true;
        } else {
            if (!verifyGpsAndMissionStateSuccess) {
                if (verifyGpsAndMissionStateTimes < maxRetries) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            verifyGpsAndMissionStateTimes++;
                            verifyGpsAndMissionState(message);
                            sendEvent2Server("航线状态第" + verifyGpsAndMissionStateTimes + "次检索失败:" +
                                    WaypointMissionExecuteState.find(missionStateCode).name() +
                                    "-RTK:" + Movement.getInstance().getIs_fixed() + "-飞行器状态:" +
                                    Movement.getInstance().getPlaneMessage() +
                                    "-GPS信号等级:" + Movement.getInstance().getQuality(), 1);
                        }
                    }, 2000);

                } else {
                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                    sendEvent2Server("飞行器自检异常:" + WaypointMissionExecuteState.find(missionStateCode).name() +
                            "-RTK:" + Movement.getInstance().getIs_fixed() + "-飞行器状态:" +
                            Movement.getInstance().getPlaneMessage() +
                            "-GPS信号等级:" + Movement.getInstance().getQuality(),2);
                    DockStorageManager.getInstance().sendDockStorageMsg2Server();
                }
            }
        }
    }

    /**
     * 5.生成航线
     *
     * @param message
     */
    public void toGenerateKMZFile(MessageDown message) {
        Movement.getInstance().setTask_current_step(16);
        // 创建第一个 MissionPoint 对象
        MissionPoint missionPoint = new MissionPoint();
        missionPoint.setLat(String.valueOf(Movement.getInstance().getLatitude()));
        missionPoint.setLng(String.valueOf(Movement.getInstance().getLongitude()));
        missionPoint.setSpeed(Double.parseDouble(message.getData().getMax_speed()));
        missionPoint.setExecuteHeight(message.getData().getTarget_height() - 1);

        // 创建第二个 MissionPoint 对象
        MissionPoint missionPoint1 = new MissionPoint();
        missionPoint1.setLat(message.getData().getTarget_latitude() + "");
        missionPoint1.setLng(message.getData().getTarget_longitude() + "");
        missionPoint1.setSpeed(Double.parseDouble(message.getData().getMax_speed()));
        missionPoint1.setExecuteHeight(message.getData().getTarget_height());

        // 创建一个 MissionPoint 列表
        List<MissionPoint> missionPoints = new ArrayList<>();
        missionPoints.add(missionPoint);
        missionPoints.add(missionPoint1);

        // 创建 FlightMission 对象并设置其属性
        FlightMission flightMission = new FlightMission();
        flightMission.setPoints(missionPoints);
        flightMission.setMissionId(2);
        flightMission.setFinishAction("noAction");
        flightMission.setTakeOffSecurityHeight(
                Float.parseFloat(message.getData().getSecurity_takeoff_height()));
        flightMission.setSpeed(Double.parseDouble(message.getData().getMax_speed()));

        LogUtil.log(TAG, "当前高度:" + Movement.getInstance().getElevation()
                + "-一键起飞安全起飞高度:" + message.getData().getSecurity_takeoff_height());

        sendEvent2Server("开始生成一键起飞航线", 1);

        // 生成xml文件
        File file1 = new File(
                getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz");
        if (!file1.exists()) {
            if (file1.mkdirs()) {
                sendEvent2Server("一键起飞航线文件生成成功", 1);
            } else {
                sendEvent2Server("一键起飞航线文件生成失败", 2);
            }
        }
        DomParserKML domParserKML = new DomParserKML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/template.kml");
        domParserKML.createKml(flightMission);

        DomParserWPML domParserWPML = new DomParserWPML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/waylines.wpml");
        domParserWPML.createWpml(flightMission);

        File kmzFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "ToPoint.kmz");
        kmzFile.getParentFile().mkdirs();

        try {
            ZipUtil.zip(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + "/wpmz",
                    getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "ToPoint.kmz");
        } catch (IOException e) {
            sendEvent2Server("一键起飞任务生成异常", 2);
            throw new RuntimeException(e);
        }
        pushKMZFileToAircraft(message);
    }

    private int pushKMZFileTimes = 0;
    public static long pushKMZFailTimeMillis;
    public boolean isPushKMZSuccess;

    public boolean isPushKMZFailTimes() {
        long time = System.currentTimeMillis();
        if (time - pushKMZFailTimeMillis > 3000) {
            pushKMZFailTimeMillis = time;
            return true;
        }
        return false;
    }

    /**
     * 6.上传指点航线
     * @param message
     */
    private void pushKMZFileToAircraft(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            KMZInfo kmzInfo = WPMZManager.getInstance().getKMZInfo(
                    Environment.getExternalStorageDirectory().getPath() + "/" + "ToPoint.kmz");
            if (kmzInfo != null) {
//                Utils.printJson(TAG,"航点详情:"+new Gson().toJson(kmzInfo));
                WaylineWaylinesParseInfo waylineWaylinesParseInfo = kmzInfo.getWaylineWaylinesParseInfo();
                if (waylineWaylinesParseInfo != null) {
                    List<Wayline> waylines = waylineWaylinesParseInfo.getWaylines();
                    if (waylines != null && waylines.size() > 0) {
                        List<WaylineExecuteWaypoint> waypoints = waylines.get(0).getWaypoints();
                        if (waypoints != null && waypoints.size() > 0) {
                            //将航点列表保存在本地
                                CurrentWayline.getInstance().setWaypoints(waypoints);
                            LogUtil.log(TAG, "该航线有" + waypoints.size() + "个航点");
                        } else {
                            LogUtil.log(TAG, "WPMZManager getWaypointInfo有误");
                        }
                    } else {
                        LogUtil.log(TAG, "WPMZManager getTemplates有误");
                    }

                } else {
                    LogUtil.log(TAG, "WPMZManager getKMZInfo有误");
                }
            } else {
                LogUtil.log(TAG, "WPMZManager getKMZInfo有误");

            }


            WaypointMissionManager.getInstance().pushKMZFileToAircraft(Environment.getExternalStorageDirectory().getPath() + "/" + "aros.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
                @Override
                public void onProgressUpdate(Double progress) {
                    sendEvent2Server("航线上传进度:" + progress, 1);
                    Movement.getInstance().setTask_current_step(17);
                }

                @Override
                public void onSuccess() {
                    sendEvent2Server("航线上传成功,准备执行任务", 1);
                    isPushKMZSuccess = true;
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            /**
                             * 7.开始任务
                             */
                            Movement.getInstance().setTask_current_step(22);

                            startMission(message);
                            pushKMZFileTimes = 0;
                        }
                    }, 2000);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (!isPushKMZSuccess) {
                        if (pushKMZFileTimes < 10) {
                            if (isPushKMZFailTimes()) {
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        LogUtil.log(TAG, "上传航线第" + pushKMZFileTimes + "次失败,重新上传" + ":" + new Gson().toJson(error));
                                        pushKMZFileTimes++;
                                        pushKMZFileToAircraft(message);
                                    }
                                }, 3000);
                            } else {
                                LogUtil.log(TAG, "上传航线只处理一次回调" + ":" + new Gson().toJson(error));
                            }
                        } else {
                            sendEvent2Server("航线第" + pushKMZFileTimes + "次上传失败,直接关机",2);
                            DockStorageManager.getInstance().sendDockStorageMsg2Server();
                        }
                    } else {
                        sendEvent2Server("航线上传已经执行onSuccess回调:" + WaypointMissionExecuteState.find(Movement.getInstance().getMissionStateCode()).name(), 1);
                    }
                }
            });
        }
    }

    private int startMissionFailTimes = 0;
    private boolean isMissionStart = false;

    /**
     * 6.开始航线
     *
     * @param message
     */
    public void startMission(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            int missionStateCode = Movement.getInstance().getMissionStateCode();
            //每次航线开始时，重置是否需要识别二维码状态，避免刚起飞就识别二维码/并确保不是飞向备降点的航线
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
            PreferenceUtils.getInstance().setFlightId(message.getData().getFlight_id());
            CommonCallbacks.CompletionCallback callback = new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    isMissionStart = true;
                    LogUtil.log(TAG, "航线第" + startMissionFailTimes + "次开始成功");
                    Movement.getInstance().setTask_status("in_progress");
                    startMissionFailTimes = 0;
                    sendEvent2Server("任务开始执行", 1);
                    Movement.getInstance().setTask_current_step(23);
                    sendFlightTaskProgress2Server();
//                    Movement.getInstance().setMode_code(5);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "航线执行失败:" + new Gson().toJson(error));
                    if (!isMissionStart) {
                        if (missionStateCode != 3 && missionStateCode != 4 && missionStateCode != 5 && missionStateCode != 6
                                && missionStateCode != 7 && missionStateCode != 8 && missionStateCode != 9 && missionStateCode != 10) {
                            if (startMissionFailTimes < 50) {
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startMission(message);
                                        sendEvent2Server("航线第" + startMissionFailTimes + "次开始失败" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality(), 1);
                                        startMissionFailTimes++;
                                    }
                                }, 2000);
                            } else {
                                if (!Movement.getInstance().isPlaneWing()) {
                                    sendEvent2Server("航线第" + startMissionFailTimes + "次开始失败,直接关机:" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality(),2);
                                    DockStorageManager.getInstance().sendDockStorageMsg2Server();
                                }
                            }
                        } else {
                            sendEvent2Server("航线已经执行:" + WaypointMissionExecuteState.find(missionStateCode).name(), 1);
                        }
                    }
                }
            };

            WaypointMissionManager.getInstance().startMission("ToPoint", callback);
        } else {
            sendEvent2Server("任务开始失败,设备未连接", 2);
        }
    }

    private void checkVtxWithDelay(Runnable onVtxReady) {

        if (Movement.getInstance().isVtx()) {
            // 图传正常，直接继续后面的流程
            PreferenceUtils.getInstance().setRestartAMSTimes(0);
            LogUtil.log(TAG, "图传正常，继续执行任务流程");
            onVtxReady.run();
            return;
        }

        LogUtil.log(TAG, "未检测到图传，5 秒后再次确认...");

        mainHandler.postDelayed(() -> {
            if (!Movement.getInstance().isVtx()) {
                // 图传仍未恢复 → 执行重启逻辑
                int times = PreferenceUtils.getInstance().getRestartAMSTimes();
                if (times < 5) {
                    PreferenceUtils.getInstance().setRestartAMSTimes(times + 1);
                    LogUtil.log(TAG, "图传仍未恢复，重启 AMS 第 " + (times + 1) + " 次");
                    RestartAPPTool.INSTANCE.restartApp(ApronApp.Companion.getContext());
                }
            } else {
                // 图传恢复 → 执行后续逻辑
                PreferenceUtils.getInstance().setRestartAMSTimes(0);
                LogUtil.log(TAG, "图传在延迟期间恢复，继续执行任务流程");
                onVtxReady.run();
            }
        }, 5000);
    }

}
