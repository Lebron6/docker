package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.app.ApronApp;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.RestartAPPTool;
import com.dji.wpmzsdk.common.data.KMZInfo;
import com.dji.wpmzsdk.manager.WPMZManager;
import com.google.gson.Gson;

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
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendEvent2Server("任务状态:未连接");
                                break;
                            case IDLE:
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendEvent2Server("任务状态:初始化");
                                break;
                            case NOT_SUPPORTED:
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendEvent2Server("任务状态:此机型不支持航线任务3.0");
                                Movement.getInstance().setTask_status("rejected");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case READY:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendEvent2Server("任务状态:准备中");
                                break;
                            case UPLOADING:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendEvent2Server("任务状态:上传中");
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setTask_status("sent");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case PREPARING:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendEvent2Server("任务状态:执行准备中");
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setTask_status("in_progress");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case ENTER_WAYLINE:
                                enterWayLineTime = System.currentTimeMillis();
                                Movement.getInstance().setAirlineFlight(true);
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:进入航线飞行,飞往指定航线的第一个航点");
                                sendFlightTaskProgress2Server();
                                Movement.getInstance().setTask_status("in_progress");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case EXECUTING:
                                Movement.getInstance().setAirlineFlight(true);
                                sendEvent2Server("任务状态:航线任务执行中");
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                Movement.getInstance().setTask_status("in_progress");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case INTERRUPTED:
                                Movement.getInstance().setAirlineFlight(true);
                                Movement.getInstance().setWaylineCanResume(false);
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:航线任务执行中断");
                                Movement.getInstance().setTask_status("paused");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case RECOVERING:
                                Movement.getInstance().setAirlineFlight(true);
                                Movement.getInstance().setWaylineCanResume(false);
                                Movement.getInstance().setVirtualStickQuitMission(false);
                                sendEvent2Server("任务状态:航线任务恢复中");
                                Movement.getInstance().setTask_status("in_progress");
                                Movement.getInstance().setMissionFinish(false);
                                sendFlightTaskProgress2Server();
                                break;
                            case FINISHED:
                                finishWayLineTime = System.currentTimeMillis();
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute() &&
                                        !(Movement.getInstance().getCurrentWaypointIndex() > 0)) {
                                    Movement.getInstance().setWaylineCanResume(true);
                                } else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finishWayLineTime - enterWayLineTime <= 11000 && !Movement.getInstance().isPlaneWing()
                                        ) {
                                            sendTaskFailEvent2Server("10s内任务非正常结束,直接入库");
                                        }
                                    }
                                }, 5000);
                                sendFlightTaskProgress2Server();
                                break;
                            case RETURN_TO_START_POINT:
                                Movement.getInstance().setAirlineFlight(true);
                                Movement.getInstance().setTask_status("in_progress");
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
                        //状态等执行完发送再更新
                        Movement.getInstance().setCurrentWaypointIndex(excutingWaylineInfo.getCurrentWaypointIndex());
                        Movement.getInstance().setTask_current_waypoint_index(excutingWaylineInfo.getCurrentWaypointIndex());
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

    //收到航线
    public void taskExecute(MessageDown message) {
        PreferenceUtils.getInstance().setFlightId(message.getData().getFlight_id());
        PreferenceUtils.getInstance().setAlternatePointLon(message.getData().getAlternate_land_point().getLongitude() + "");
        PreferenceUtils.getInstance().setAlternatePointLat(message.getData().getAlternate_land_point().getLatitude() + "");
        PreferenceUtils.getInstance().setAlternatePointSecurityHeight(message.getData().getAlternate_land_point().getSafe_land_height() + "");

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
            if (Movement.getInstance().getGoHomeState() == 1 || Movement.getInstance().getGoHomeState() == 2) {
                sendFailMsg2Server(message, "返航中,无法执行航线任务");
                return false;
            }
            //5.检查航线备降点参数
            if (message.getData().getAlternate_land_point() == null) {
                sendTaskFailEvent2Server("备降点参数异常");
                return false;
            }
            //6.检查航线参数
            if (message.getData().getFile() == null) {
                sendTaskFailEvent2Server("航线参数异常");
                return false;
            }
            //7.检查电池电量
            Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                    KeyBatteryPowerPercent, 0));
            if (value != null && value < Integer.parseInt(PreferenceUtils.getInstance().getMinumumBattery())) {
                sendTaskFailEvent2Server("任务执行失败,电量过低");
                return false;
            }
            RemoteControllerFlightMode remoteControllerFlightMode =
                    KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
            if (remoteControllerFlightMode != null && remoteControllerFlightMode != RemoteControllerFlightMode.P) {
                sendTaskFailEvent2Server("任务执行失败,请将遥控器切换为P/N挡");
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
                    verifyGpsAndMissionStateTimes++;
                    sendEvent2Server("飞行器自检中..." + verifyGpsAndMissionStateTimes);
                    verifyGpsAndMissionState(message);
                    sendEvent2Server("航线状态第" + verifyGpsAndMissionStateTimes + "次检索失败:" +
                            WaypointMissionExecuteState.find(missionStateCode).name() +
                            "-RTK:" + Movement.getInstance().getIs_fixed() + "-飞行器状态:" +
                            Movement.getInstance().getPlaneMessage() +
                            "-GPS信号等级:" + Movement.getInstance().getQuality());
                }
            }
        }
    }

    /**
     * 5.下载航线
     *
     * @param message
     */
    public void downLoadKMZFile(MessageDown message) {
        Movement.getInstance().setTask_current_step(16);
        Request request = new Request.Builder().url(message.getData().getFile().getUrl()).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendTaskFailEvent2Server("任务下载失败:" + e.toString());
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
                    File file = new File(dir, "aros.kmz");
                    try {
                        is = response.body().byteStream();
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        sendEvent2Server("航线下载成功 ");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                pushKMZFileToAircraft(message);
                            }
                        });
                    } catch (Exception e) {
                        sendTaskFailEvent2Server("任务下载失败,网络异常:" + e.toString());
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
                            //将航点列表保存在本地
                            if (!PreferenceUtils.getInstance().getIsNewRoute()) {
                                CurrentWayline.getInstance().setWaypoints(waypoints);
                            } else if (PreferenceUtils.getInstance().getMissionType() == 2) {
                                if (waylines.size()>2){
                                    //清除飞机当前坐标点和断点位置
                                    waylines.subList(0,2).clear();
                                }
                                CurrentWayline.getInstance().setRouteWaypoints(waypoints);

                            }
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
                    sendEvent2Server("航线上传进度:" + progress);
                    Movement.getInstance().setTask_current_step(17);

                }

                @Override
                public void onSuccess() {
                    sendEvent2Server("航线上传成功,准备执行任务");
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
                            sendTaskFailEvent2Server("航线第" + pushKMZFileTimes + "次上传失败,直接关机");
                        }
                    } else {
                        sendEvent2Server("航线上传已经执行onSuccess回调:" + WaypointMissionExecuteState.find(Movement.getInstance().getMissionStateCode()).name());
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
                    sendEvent2Server("任务开始执行");
                    Movement.getInstance().setTask_current_step(23);
                    sendFlightTaskProgress2Server();
                    Movement.getInstance().setMode_code(5);
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
                                        sendEvent2Server("航线第" + startMissionFailTimes + "次开始失败" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality());
                                        startMissionFailTimes++;
                                    }
                                }, 2000);
                            } else {
                                if (!Movement.getInstance().isPlaneWing()) {
                                    sendTaskFailEvent2Server("航线第" + startMissionFailTimes + "次开始失败,直接关机:" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getQuality());
                                }
                            }
                        } else {
                            sendEvent2Server("航线已经执行:" + WaypointMissionExecuteState.find(missionStateCode).name());
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
            sendEvent2Server("任务开始失败,设备未连接");
        }
    }

    /**
     * 暂停航线
     * @param message
     */
    public void pauseMission(MessageDown message) {
        //暂停前将index保存
        PreferenceUtils.getInstance().setPauseIndex(Movement.getInstance().getCurrentWaypointIndex()+"");
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
                if (times < 3) {
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
        }, 10000);
    }

}
