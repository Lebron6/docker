package com.jby.apron.manager;

import static com.jby.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.wpmzsdk.common.data.KMZInfo;
import com.dji.wpmzsdk.manager.WPMZManager;
import com.google.gson.Gson;
import com.jby.apron.app.ApronApp;
import com.jby.apron.base.BaseManager;
import com.jby.apron.constant.ErrorCode;
import com.jby.apron.entity.CurrentWayline;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.PreferenceUtils;
import com.jby.apron.tools.RestartAPPTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener;
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo;
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo;
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;
import dji.v5.manager.interfaces.IWaypointMissionManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MissionV3Manager extends BaseManager {

    final Handler mainHandler = new Handler(Looper.getMainLooper());


    private MissionV3Manager() {
    }

    private static class MissionHolder {
        private static final MissionV3Manager INSTANCE = new MissionV3Manager();
    }

    public static MissionV3Manager getInstance() {
        return MissionHolder.INSTANCE;
    }

    public boolean isReceiverMission = false;
    //在ENTER_WAYLINE后10秒，航线状态变为FINISH，此时无人机不起飞/悬停
    private long enterWayLineTime;
    private long finishWayLineTime;

    public void initMissionManager() {

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            WaypointMissionManager waypointMissionManager = WaypointMissionManager.getInstance();
            waypointMissionManager.addWaypointMissionExecuteStateListener(new WaypointMissionExecuteStateListener() {
                @Override
                public void onMissionStateUpdate(WaypointMissionExecuteState missionState) {
                    if (missionState != null) {
                        switch (missionState) {
                            case DISCONNECTED:
                                sendEvent2Server("任务状态:未连接", 1);
                                break;
                            case IDLE:
                                sendEvent2Server("任务状态:初始化", 1);
                                break;
                            case NOT_SUPPORTED:
                                sendEvent2Server("任务状态:此机型不支持航线任务3.0", 1);
                                Movement.getInstance().setTask_status("rejected");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case READY:
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                sendEvent2Server("任务状态:准备中", 1);
                                break;
                            case UPLOADING:
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("sent");
                                }
                                sendEvent2Server("任务状态:上传中", 1);
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case PREPARING:
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                sendEvent2Server("任务状态:执行准备中", 1);
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case ENTER_WAYLINE:
                                enterWayLineTime = System.currentTimeMillis();
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:进入航线飞行,飞往指定航线的第一个航点", 1);
                                sendFlightTaskProgress2Server();
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case EXECUTING:
                                sendEvent2Server("任务状态:航线任务执行中", 1);
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case INTERRUPTED:
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:航线任务执行中断", 1);
                                Movement.getInstance().setTask_status("paused");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case RECOVERING:
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:航线任务恢复中", 1);
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case FINISHED:
                                finishWayLineTime = System.currentTimeMillis();
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finishWayLineTime - enterWayLineTime <= 11000 && !Movement.getInstance().isPlaneWing()
                                        ) {
                                            sendEvent2Server("任务非正常结束", 2);
                                            TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                                        }
                                    }
                                }, 5000);
                                //需要在这里保存下来机头角度，index等断点，在降落后若查询到断点，在拼接起来
                                PreferenceUtils.getInstance().setAttitudeHead(Movement.getInstance().getAttitude_head()+"");
                                PreferenceUtils.getInstance().setWaypointIndex(Movement.getInstance().getCurrentWaypointIndex()+"");
                                queryBreakPoint();
                                sendFlightTaskProgress2Server();
                                break;
                            case RETURN_TO_START_POINT:
                                if (PreferenceUtils.getInstance().getMissionType()==2
                                ) {
                                    Movement.getInstance().setTask_status("paused");
                                } else {
                                    Movement.getInstance().setTask_status("in_progress");
                                }
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                        }
                        LogUtil.log(TAG, "WaypointMissionExecuteState:" + missionState.name());
                        Movement.getInstance().setWaypointMissionExecuteState(missionState.name());
                        Movement.getInstance().setTask_wayline_mission_state(missionState.value());
                        Movement.getInstance().setMissionStateCode(missionState.value());
                    }
                }
            });
            waypointMissionManager.addWaylineExecutingInfoListener(new WaylineExecutingInfoListener() {
                @Override
                public void onWaylineExecutingInfoUpdate(WaylineExecutingInfo excutingWaylineInfo) {
                    if (excutingWaylineInfo != null) {
                        //状态等执行完发送再更新(测试暂停时不改变index为0)
                        if (excutingWaylineInfo.getCurrentWaypointIndex() != 0) {
                            Movement.getInstance().setCurrentWaypointIndex(excutingWaylineInfo.getCurrentWaypointIndex());
                            Movement.getInstance().setTask_current_waypoint_index(excutingWaylineInfo.getCurrentWaypointIndex());
                        }
                    }
                }

                @Override
                public void onWaylineExecutingInterruptReasonUpdate(IDJIError error) {
                }
            });
            waypointMissionManager.addWaypointActionListener(new WaypointActionListener() {
                @Override
                public void onExecutionStart(int actionId) {

                }

                @Override
                public void onExecutionFinish(int actionId, @Nullable IDJIError error) {

                }

                @Override
                public void onExecutionStart(int actionGroup, int actionId) {

                }

                @Override
                public void onExecutionFinish(int actionGroup, int actionId, @Nullable IDJIError error) {

                }
            });

        }
    }

    //航线下发
    public void flightTaskPrepare(MessageDown message) {

        //1.若当前处于虚拟摇杆状态，取消虚拟摇杆
        if (Movement.getInstance().getIsVirtualStickEnable() == 1) {
            StickManager.getInstance().disableVirtualStick(null);
        }
        //2.关闭避障
        PerceptionManager.getInstance().setPerceptionEnable(false);

        //3.检查电池电量
        if (message.getData().getTask_type() == 2) {
            Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                    KeyBatteryPowerPercent, 0));
            if (value != null && value < message.getData().getReady_conditions().getBattery_capacity()) {
                sendFailMsg2Server(message, "任务执行失败,电量过低");
                return;
            }
        }
        //4.返航或降落状态无法执行航线
        if (Movement.getInstance().getGoHomeState() == 1 || Movement.getInstance().getGoHomeState() == 2) {
            sendFailMsg2Server(message, "返航中,无法执行航线任务");
            return;
        }
        //5.检查航线参数
        if (message.getData().getFile() == null) {
            sendFailMsg2Server(message,"航线参数异常");
            return;
        }
        //6.检查遥控器档位
        RemoteControllerFlightMode remoteControllerFlightMode =
                KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
        if (remoteControllerFlightMode != null && remoteControllerFlightMode != RemoteControllerFlightMode.P) {
            sendFailMsg2Server(message,"任务执行失败,请将遥控器切换为P/N挡");
            return;
        }
        //7.下载航线
        downLoadKMZFile(message);
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
            if (Movement.getInstance().getGoHomeState() == 1 || Movement.getInstance().getGoHomeState() == 2) {
                sendFailMsg2Server(message, "返航中,无法执行航线任务");
                return false;
            }
            //5.检查航线备降点参数
//            if (message.getData().getAlternate_land_point() == null) {
//                sendEvent2Server("备降点参数异常", 2);
//                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
//                return false;
//            }
            //6.检查航线参数
            if (message.getData().getFile() == null) {
                sendEvent2Server("航线参数异常", 2);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                return false;
            }
            //7.检查电池电量
            Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                    KeyBatteryPowerPercent, 0));
            if (value != null && value < Integer.parseInt(PreferenceUtils.getInstance().getMinumumBattery())) {
                sendEvent2Server("任务执行失败,电量过低", 2);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(ErrorCode.DEVICE_BATTERY_LEVEL_TOO_LOW);
                return false;
            }
            RemoteControllerFlightMode remoteControllerFlightMode =
                    KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
            if (remoteControllerFlightMode != null && remoteControllerFlightMode != RemoteControllerFlightMode.P) {
                sendEvent2Server("任务执行失败,请将遥控器切换为P/N挡", 2);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
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
            //5.下载航线
            downLoadKMZFile(message);
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
                    },2000);

                }else {
                    sendEvent2Server("飞行器自检异常:" + WaypointMissionExecuteState.find(missionStateCode).name() +
                            "-RTK:" + Movement.getInstance().getIs_fixed() + "-飞行器状态:" +
                            Movement.getInstance().getPlaneMessage() +
                            "-GPS信号等级:" + Movement.getInstance().getQuality(), 2);
                    TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                }
            }
        }
    }

    /**
     * 5.下载航线
     * @param message
     */
    public void downLoadKMZFile(MessageDown message) {
        Movement.getInstance().setTask_current_step(16);
        Request request = new Request.Builder().url(message.getData().getFile().getUrl()).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendEvent2Server("任务下载失败:" + e.toString(), 2);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    // 储存下载文件的目录
                    File dir = new File(Environment.getExternalStorageDirectory().getPath());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File file = new File(dir, "jby.kmz");
                    try {
                        is = response.body().byteStream();
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        sendEvent2Server("航线下载成功 ", 1);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                pushKMZFileToAircraft(message);
                            }
                        });
                    } catch (Exception e) {
                        sendEvent2Server("任务下载失败,网络异常:" + e.toString(), 2);
                        TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                    }
                }
            }
        });

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
     * 6.上传航线
     * @param message
     */
    private void pushKMZFileToAircraft(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            KMZInfo kmzInfo = WPMZManager.getInstance().getKMZInfo(
                    Environment.getExternalStorageDirectory().getPath() + "/" + "aros.kmz");
            if (kmzInfo != null) {
//                Utils.printJson(TAG,"航点详情:"+new Gson().toJson(kmzInfo));
                WaylineWaylinesParseInfo waylineWaylinesParseInfo = kmzInfo.getWaylineWaylinesParseInfo();
                if (waylineWaylinesParseInfo != null) {
                    List<Wayline> waylines = waylineWaylinesParseInfo.getWaylines();
                    if (waylines != null && waylines.size() > 0) {
                        List<WaylineExecuteWaypoint> waypoints = waylines.get(0).getWaypoints();
                        if (waypoints != null&&waypoints.size()>0) {

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
                            sendEvent2Server("航线第" + pushKMZFileTimes + "次上传失败,直接关机", 2);
                            TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
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
            CommonCallbacks.CompletionCallback callback= new CommonCallbacks.CompletionCallback() {
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
                                        sendEvent2Server("航线第" + startMissionFailTimes + "次开始失败" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality(), 2);
                                        startMissionFailTimes++;
                                    }
                                }, 2000);
                            } else {
                                if (!Movement.getInstance().isPlaneWing()) {
                                    sendEvent2Server("航线第" + startMissionFailTimes + "次开始失败,直接关机:" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality(), 2);
                                    TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                                }
                            }
                        } else {
                            sendEvent2Server("航线已经执行:" + WaypointMissionExecuteState.find(missionStateCode).name(), 1);
                        }
                    }
                }
            };

            if (message.getData()!=null&&message.getData().getBreak_point()!=null&&message.getData().getBreak_point().getIndex()>0){
                BreakPointInfo breakPointInfo=new BreakPointInfo
                        (message.getData().getBreak_point().getWayline_id(),
                                message.getData().getBreak_point().getIndex(),
                                message.getData().getBreak_point().getProgress());
                WaypointMissionManager.getInstance().startMission("aros",breakPointInfo,
                        callback);
            }else{
                WaypointMissionManager.getInstance().startMission("aros",callback);
            }

        } else {
            sendEvent2Server("任务开始失败,设备未连接", 2);
        }
    }

    /**
     * 暂停航线
     * @param message
     */
    public void pauseMission(MessageDown message) {

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.pauseMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(message);
                    Movement.getInstance().setTask_status("paused");
                    sendFlightTaskProgress2Server();

                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server( message, "航线任务暂停失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            LogUtil.log(TAG, "航线任务暂停失败:飞控未连接");
        }
    }

    //恢复航线
    public void resumeMission(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            //如果航线是暂停状态,直接恢复航线，否则查询断点信息
            if (Movement.getInstance().getMissionStateCode() == 7) {
                missionManager.resumeMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(message);
                        Movement.getInstance().setTask_status("in_progress");
                        sendFlightTaskProgress2Server();
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        sendFailMsg2Server(message, "航线继续失败:" + getIDJIErrorMsg(error));
                    }
                });
            } else {
                missionManager.queryBreakPointInfoFromAircraft("aros", new CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo>() {
                    @Override
                    public void onSuccess(BreakPointInfo breakPointInfo) {
                        if (breakPointInfo != null) {
                            LogUtil.log(TAG, "查询断点成功:" + new Gson().toJson(breakPointInfo));
                            mainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    WaypointMissionManager.getInstance().startMission("aros", breakPointInfo,
                                            new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    sendMsg2Server(message);
                                                    PreferenceUtils.getInstance().setIsNewRoute(false);
                                                    LogUtil.log(TAG, "恢复断点航线成功");
                                                    Movement.getInstance().setTask_status("in_progress");
                                                    sendFlightTaskProgress2Server();
                                                }

                                                @Override
                                                public void onFailure(@NonNull IDJIError idjiError) {
                                                    LogUtil.log(TAG, "恢复断点航线失败:" + getIDJIErrorMsg(idjiError));
                                                    sendFailMsg2Server(message, "恢复断点航线失败:" + getIDJIErrorMsg(idjiError));
                                                }
                                            });
                                }
                            }, 500);
                        } else {
                            LogUtil.log(TAG, "未查询到断点信息");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG, "查询断点失败:" + getIDJIErrorMsg(idjiError));
                    }
                });
            }

        } else {
            LogUtil.log(TAG, "设备未连接");
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
