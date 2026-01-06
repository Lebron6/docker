package com.aros.apron.manager;


import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LocationUtils;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.dji.wpmzsdk.common.data.KMZInfo;
import com.dji.wpmzsdk.manager.WPMZManager;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttMessage;

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
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo;
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;
import dji.v5.manager.interfaces.IWaypointMissionManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MissionManager extends BaseManager {

    private MQMessage message;
    private int missionStateCode;

    private MissionManager() {
    }

    private static class PerceptionHolder {
        private static final MissionManager INSTANCE = new MissionManager();
    }

    public static MissionManager getInstance() {
        return PerceptionHolder.INSTANCE;
    }

    //御三T有可能在ENTER_WAYLINE后10秒，航线状态变为FINISH，此时无人机不起飞
    private long enterWayLineTime;
    private long finishWayLineTime;
    private int retryPushKmzTime;
    private int mStartGroupId = 9999;//默认第一个动作组id
    private int mFinishGroupId = 0;//默认第一个动作组id
    //已经发送离开最后一个航点
    private boolean alreadySendLeaveLastPoint;

    public void initMissionManager() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            WaypointMissionManager waypointMissionManager = WaypointMissionManager.getInstance();
            waypointMissionManager.addWaypointActionListener(new WaypointActionListener() {
                @Override
                public void onExecutionStart(int actionId) {
//CameraManager.getInstance().setCustomExpandNameSetting();
                }

                @Override
                public void onExecutionFinish(int actionId, @Nullable IDJIError error) {

                }

                @Override
                public void onExecutionStart(int actionGroup, int actionId) {
//                    if (mStartGroupId != actionGroup) {
//                        mStartGroupId = actionGroup;
//                        sendMsgWaypointActionState2Server(MqttManager.getInstance().mqttAndroidClient, "0", "" + (actionGroup + 1));
//                        LogUtil.log(TAG, "动作组开始:" + "actionGroup--" + (actionGroup + 1) + "actionId--"
//                                + actionId + "waypointIndex--" + Movement.getInstance().getCurrentWaypointIndex());
//                    }

                }

                @Override
                public void onExecutionFinish(int actionGroup, int actionId, @Nullable IDJIError error) {
//                    if (mActionGroups != null && mActionGroups.size() > actionGroup) {
//                        if (mActionGroups.get(actionGroup).getActions().size() > actionId) {
//                            //判断是否是该动作组第一个动作
//                            if (actionId == mActionGroups.get(actionGroup).getActions().size() - 1) {
//                                sendMsgWaypointActionState2Server(MqttManager.getInstance().mqttAndroidClient, "1", "" + (actionGroup + 1));
//                                LogUtil.log(TAG, "航点动作组结束:" + "actionGroup=" + (actionGroup + 1) + "  actionId="
//                                        + actionId + "  waypointIndex=" + Movement.getInstance().getCurrentWaypointIndex());
//                            }
//
//                        } else {
//                            LogUtil.log(TAG, "动作下标异常:getActions().size()= " + mActionGroups.get(actionGroup).getActions().size() + "actionId=" + actionId);
//                        }
//
//                    } else {
//                        LogUtil.log(TAG, "动作组下标异常:mActionGroups.size()= " + mActionGroups.size() + "actionGroup=" + actionGroup);
//                    }
                }
            });
            waypointMissionManager.addWaylineExecutingInfoListener(waylineExecutingInfoListener);
            waypointMissionManager.addWaypointMissionExecuteStateListener(new WaypointMissionExecuteStateListener() {
                @Override
                public void onMissionStateUpdate(WaypointMissionExecuteState missionState) {
                    if (missionState != null) {
                        switch (missionState) {
                            case DISCONNECTED:
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendMissionExecuteEvents( "任务状态:未连接");
                                break;
                            case IDLE:
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendMissionExecuteEvents( "任务状态:初始化");
                                break;
                            case NOT_SUPPORTED:
                                Movement.getInstance().setAirlineFlight(false);
                                Movement.getInstance().setWaylineCanResume(false);
                                sendMissionExecuteEvents( "任务状态:此机型不支持航线任务3.0");
                                break;
                            case READY:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendMissionExecuteEvents( "任务状态:准备中");
                                break;
                            case UPLOADING:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendMissionExecuteEvents( "任务状态:上传中");
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                break;
                            case PREPARING:
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                sendMissionExecuteEvents( "任务状态:执行准备中");
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                break;
                            case ENTER_WAYLINE:
                                enterWayLineTime = System.currentTimeMillis();
                                Movement.getInstance().setAirlineFlight(true);
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                sendMissionExecuteEvents( "任务状态:进入航线飞行,飞往指定航线的第一个航点");

                                break;
                            case EXECUTING:
                                Movement.getInstance().setAirlineFlight(true);
                                sendMissionExecuteEvents( "任务状态:航线任务执行中");
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                break;
                            case INTERRUPTED:
                                Movement.getInstance().setAirlineFlight(true);
                                Movement.getInstance().setWaylineCanResume(false);
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                sendMissionExecuteEvents( "任务状态:航线任务执行中断");
                                break;
                            case RECOVERING:
                                Movement.getInstance().setAirlineFlight(true);
                                Movement.getInstance().setWaylineCanResume(false);
                                Movement.getInstance().setVirtualStickQuitMission(false);

                                sendMissionExecuteEvents( "任务状态:航线任务恢复中");
                                break;
                            case FINISHED:
                                //航线finish时检查当前飞机位置是否距离最后一个航点很近，判断最后一个航点执行完毕
                                if(CurrentWayline.getInstance().getWaypoints()!=null
                                        &&CurrentWayline.getInstance().getWaypoints().size()>0&&(
                                        !PreferenceUtils.getInstance().getIsNewRoute()||
                                                PreferenceUtils.getInstance().getMissionType()==2)){
                                    double pointDistance = LocationUtils.getDistance(
                                            CurrentWayline.getInstance().getWaypoints()
                                                    .get(CurrentWayline.getInstance().getWaypoints().size()-1)
                                                    .getLocation().getLongitude().toString(),
                                            CurrentWayline.getInstance().getWaypoints()
                                                    .get(CurrentWayline.getInstance().getWaypoints().size()-1)
                                                    .getLocation().getLatitude().toString(),
                                            String.valueOf(Movement.getInstance().getCurrentLongitude()),
                                            String.valueOf(Movement.getInstance().getCurrentLatitude()));
                                    if (pointDistance<2&&!alreadySendLeaveLastPoint) {
                                        alreadySendLeaveLastPoint =true;
                                        sendCustomReachOrLeave2Server( "1",
                                                String.valueOf(CurrentWayline.getInstance().getWaypoints().size()-1));
                                        LogUtil.log(TAG, "离开最后第" + (CurrentWayline.getInstance().getWaypoints().size()-1)
                                                + "个航点" + Movement.getInstance().getWaypointMissionExecuteState());
                                    }
                                }
                                Movement.getInstance().setCurrentWaypointIndex(0);
                                finishWayLineTime = System.currentTimeMillis();
                                Movement.getInstance().setAirlineFlight(false);
                                if (PreferenceUtils.getInstance().getIsNewRoute()&&
                                        !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                                    Movement.getInstance().setWaylineCanResume(true);
                                }else {
                                    Movement.getInstance().setWaylineCanResume(false);
                                }
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finishWayLineTime - enterWayLineTime <= 20000  && !message.isNewRoute()) {
                                            if (!Movement.getInstance().isPlaneWing()){
                                                LogUtil.log(TAG, "20s内任务非正常结束,直接入库");
                                                Movement.getInstance().setTaskFail(true);
                                                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                                                DroneStorageManager.getInstance().sendDroneStorageMsg2Server( -1);
                                                sendMissionExecuteEvents( "任务非正常结束");
                                            }else{
                                                if (Movement.getInstance().getFlyingHeight()<15){
                                                    LogUtil.log(TAG, "10s内任务异常结束,拉高返航:"+Movement.getInstance().getFlyingHeight());
                                                    WayLineExecutingInterruptManager.getInstance().onExecutingInterruptToDo();
                                                    sendMissionExecuteEvents("10s内任务异常结束,拉高返航");
                                                }else{
                                                    sendMissionExecuteEvents("10s内任务异常结束,等待手动返航");
                                                    LogUtil.log(TAG, "10s内任务异常结束,等待手动返航:"+Movement.getInstance().getFlyingHeight());
                                                }
                                            }
                                        }
                                    }
                                }, 5000);
                                break;
                            case RETURN_TO_START_POINT:
                                Movement.getInstance().setAirlineFlight(true);
                                break;
                        }
                        LogUtil.log(TAG, "WaypointMissionExecuteState:" + missionState.name());
                        Movement.getInstance().setWaypointMissionExecuteState(missionState.name());
                        missionStateCode = missionState.value();
                        publishMission2Server();
                    }
                }
            });
        } else {
            LogUtil.log(TAG, "初始化mission:设备未连接");
        }
    }

    private boolean waypointIndex0AlreadySend;

    WaylineExecutingInfoListener waylineExecutingInfoListener = new WaylineExecutingInfoListener() {
        @Override
        public void onWaylineExecutingInfoUpdate(WaylineExecutingInfo excutingWaylineInfo) {
            if (excutingWaylineInfo != null ) {
                Movement.getInstance().setMissionName(excutingWaylineInfo.getMissionFileName());
                LogUtil.log(TAG,"进入第"+excutingWaylineInfo.getCurrentWaypointIndex()+"个航点");

                //判断航线状态为EXECUTING，且当前index发生变化，且不在指点任务时，发送到达航点
                if (!PreferenceUtils.getInstance().getIsNewRoute()) {
                    if (Movement.getInstance().getWaypointMissionExecuteState() != null &&
                            Movement.getInstance().getWaypointMissionExecuteState().equals("EXECUTING")
                    ) {
                        if (excutingWaylineInfo.getCurrentWaypointIndex() == 0) {
                            if (!waypointIndex0AlreadySend) {
                                waypointIndex0AlreadySend = true;
                                //到达航点(航点下标=0)
                                WaypointEventSender.getInstance().sendCustomReachOrLeave2Server( "0", String.valueOf(excutingWaylineInfo.getCurrentWaypointIndex()));
                                LogUtil.log(TAG, "x进入第" + excutingWaylineInfo.getCurrentWaypointIndex() + "个航点" + Movement.getInstance().getWaypointMissionExecuteState());
                            }

                        } else if (Movement.getInstance().getCurrentWaypointIndex() != excutingWaylineInfo.getCurrentWaypointIndex()) {
                            LogUtil.log(TAG, "y进入第" + excutingWaylineInfo.getCurrentWaypointIndex() + "个航点" + Movement.getInstance().getWaypointMissionExecuteState());
                            //到达航点(航点下标>0)
                            WaypointEventSender.getInstance().sendCustomReachOrLeave2Server( "0", String.valueOf(excutingWaylineInfo.getCurrentWaypointIndex()));
                        }
                    }

                    //回到主航线
                } else if (PreferenceUtils.getInstance().getMissionType() == 2) {
                    if (Movement.getInstance().getWaypointMissionExecuteState() != null &&
                            Movement.getInstance().getWaypointMissionExecuteState().equals("EXECUTING")
                    ) {
                        //这里默认续飞的航线，下标为1以上才算进入主航线
                        if (excutingWaylineInfo.getCurrentWaypointIndex() != 0) {
                            if (CurrentWayline.getInstance().getWaypoints() != null
                                    && CurrentWayline.getInstance().getWaypoints().size() > excutingWaylineInfo.getCurrentWaypointIndex()
                                    && CurrentWayline.getInstance().getRouteWaypoints() != null
                                    && CurrentWayline.getInstance().getWaypoints().size() > excutingWaylineInfo.getCurrentWaypointIndex()) {
                                if (Movement.getInstance().getCurrentWaypointIndex() != excutingWaylineInfo.getCurrentWaypointIndex()) {
                                    //续飞航线当前航点在主航线中的下标
                                    int indexInWaypoints = findIndexInWaypoints(excutingWaylineInfo.getCurrentWaypointIndex());
                                    if (indexInWaypoints!=-1){
                                        LogUtil.log(TAG, "续飞进入第" + excutingWaylineInfo.getCurrentWaypointIndex() + "个航点，位于主航线第"+indexInWaypoints+"个航点" + Movement.getInstance().getWaypointMissionExecuteState());
                                        WaypointEventSender.getInstance().sendCustomReachOrLeave2Server( "0", String.valueOf(indexInWaypoints));
                                    }else {
                                        LogUtil.log(TAG, "未查到到主航线中包含该续飞航点");
                                    }
                                }
                            } else {
                                LogUtil.log(TAG, "数组下标不对");
                            }

                        }
                    }
                }
                //状态等执行完发送再更新
                Movement.getInstance().setCurrentWaypointIndex(excutingWaylineInfo.getCurrentWaypointIndex());


            }
        }
        @Override
        public void onWaylineExecutingInterruptReasonUpdate(IDJIError error) {
            if (error != null) {
                LogUtil.log(TAG, "航线中断: ---" + new Gson().toJson(error));
                    if (isManualPause || error.errorCode().equals("USER_BREAK")
                            || error.errorCode().equals("INTERRUPT_REASON_AVOID_USER_REQ_BREAK")) {//如果是手动暂停航线,则不会触发返航或拉高
                        isManualPause = false;
                    } else {
                        if (PreferenceUtils.getInstance().getMissionInterruptAction() == 2) {
                            if (error.errorCode().equals("INTERRUPT_REASON_AVOID") ||
                                    error.errorCode().equals("INTERRUPT_REASON_AVOID_HEIGHT_LIMIT")) {
                                mainHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeMission(null);
                                    }
                                }, 1000);
                            } else {
                                WayLineExecutingInterruptManager.getInstance().onExecutingInterruptToDo();
                            }
                        } else if (PreferenceUtils.getInstance().getMissionInterruptAction() == 3) {
                            WayLineExecutingInterruptManager.getInstance().onExecutingInterruptToDo();
                        }
                        sendMissionExecuteEvents( "任务意外发生中断:" + error.errorCode());

                    }

            }
        }
    };
    /**
     * 查找 routeWaypoints 中指定索引的元素在 waypoints 中的索引
     * @param indexInRouteWaypoints routeWaypoints 中的元素索引
     * @return 在 waypoints 中的索引，如果不存在返回 -1
     */
    public int findIndexInWaypoints(int indexInRouteWaypoints) {
        // 边界检查
        if (indexInRouteWaypoints < 0 || indexInRouteWaypoints >= CurrentWayline.getInstance().getRouteWaypoints().size()) {
            return -1;
        }
        // 获取 routeWaypoints 中的指定元素
        WaylineExecuteWaypoint waypoint = CurrentWayline.getInstance().getRouteWaypoints().get(indexInRouteWaypoints);
        // 在 waypoints 中查找该元素的索引
        return CurrentWayline.getInstance().getWaypoints().indexOf(waypoint);
    }
    private int checkMissionStateTimes = 0;

    final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void startTaskProcess(MQMessage message) {
        this.message = message;
        if (!TextUtils.isEmpty(Movement.getInstance().getWarningMessage())&&
                Movement.getInstance().getWarningMessage().equals("camera进程异常")){
            if (!message.isNewRoute() && !Movement.getInstance().isPlaneWing()) {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                Movement.getInstance().setTaskFail(true);
                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
            }
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMissionExecuteEvents( "挂载相机进程异常,获取图传失败");
                    LogUtil.log(TAG, "任务执行失败,挂载相机进程异常,获取图传失败");
                }
            },1000);

            return;
        }
        Integer value = KeyManager.getInstance().getValue(createKey(FlightControllerKey.
                KeyBatteryPowerPercent, 0));
        if (value != null && value < Integer.parseInt(PreferenceUtils.getInstance().getMinumumBattery()) && !PreferenceUtils.getInstance().getIsDebugMode()) {
            if (!message.isNewRoute() && !Movement.getInstance().isPlaneWing()) {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                Movement.getInstance().setTaskFail(true);
                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
            }
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMissionExecuteEvents( "任务执行失败,电量过低");
                    LogUtil.log(TAG, "任务执行失败,电量过低");
                }
            },1000);

            return;
        }
        RemoteControllerFlightMode remoteControllerFlightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
        if (remoteControllerFlightMode != null && remoteControllerFlightMode != RemoteControllerFlightMode.P) {
            if (
                    !message.isNewRoute() &&
                            !Movement.getInstance().isPlaneWing()) {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                Movement.getInstance().setTaskFail(true);
                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
            }
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMissionExecuteEvents( "任务执行失败,请将遥控器切换为P/N挡");
                    LogUtil.log(TAG, "任务执行失败,请将遥控器切换为P/N挡");
                }
            },1000);
            return;
        }
        if (PreferenceUtils.getInstance().getHaveRTK()) {
            if ((missionStateCode == 2 || missionStateCode == 0|| missionStateCode == 7) && Movement.getInstance().isRtkSign() &&
                    (!TextUtils.isEmpty(Movement.getInstance().getPlaneMessage()) && !Movement.getInstance().getPlaneMessage().equals("无法起飞"))) {
                downLoadKMZFile(message);
                sendMissionExecuteEvents( "执行任务下载 ");
            } else {
                sendMissionExecuteEvents( "飞行器自检中 ");
                verifyAircraftStatus(message);
            }
        } else {
            //没有RTK的情况下延迟下载航线，等待GPS信号收敛
            if ((missionStateCode == 2 || missionStateCode == 0 || missionStateCode == 7) &&
                    (!TextUtils.isEmpty(Movement.getInstance().getPlaneMessage())
                            && !Movement.getInstance().getPlaneMessage().equals("无法起飞")
                            && (Movement.getInstance().getGPSSignalLevel().equals("LEVEL_4")
                            || Movement.getInstance().getGPSSignalLevel().equals("LEVEL_5")
                            || Movement.getInstance().getGPSSignalLevel().equals("LEVEL_10")))
            ) {
                if (!message.isNewRoute()) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            downLoadKMZFile(message);
                        }
                    }, 1000);
                } else {
                    downLoadKMZFile(message);
                }
            } else {
                sendMissionExecuteEvents( "飞行器自检中 ");
                verifyAircraftStatus(message);
            }
        }
    }

    //等待航线任务状态更新或RTK健康状态刷新
    private void verifyAircraftStatus(MQMessage message) {
        if (checkMissionStateTimes < 150) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startTaskProcess(message);
                    checkMissionStateTimes++;
                    LogUtil.log(TAG, "航线状态第" + checkMissionStateTimes + "次检索失败:" +
                            WaypointMissionExecuteState.find(missionStateCode).name() +
                            "-RTK解算:" + Movement.getInstance().isRtkSign() + "-飞行器状态" +
                            Movement.getInstance().getPlaneMessage() +
                            "-GPS信号等级:" + Movement.getInstance().getGPSSignalLevel());
                    sendMissionExecuteEvents( "航线状态第" + checkMissionStateTimes + "次检索失败:" +
                            WaypointMissionExecuteState.find(missionStateCode).name() +
                            "-RTK:" + Movement.getInstance().isRtkSign() + "-飞行器状态:" +
                            Movement.getInstance().getPlaneMessage() +
                            "-GPS信号等级:" + Movement.getInstance().getGPSSignalLevel());
                }
            }, 2000);
        } else {
            if (!message.isNewRoute()) {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                Movement.getInstance().setTaskFail(true);
                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);

                LogUtil.log(TAG, "飞行器自检异常:发送关机通知");
                if (PreferenceUtils.getInstance().getHaveRTK()) {
                    if (!Movement.getInstance().isRtkSign()) {
                        LogUtil.log(TAG, "飞行器RTK收敛异常");
                        sendMissionExecuteEvents( "飞行器RTK收敛异常");
                    } else if (!(missionStateCode == 2 || missionStateCode == 0 || missionStateCode == 7)) {
                    LogUtil.log(TAG, "飞行器航线状态异常:" + WaypointMissionExecuteState.find(missionStateCode).name());
                        sendMissionExecuteEvents( "飞行器航线状态异常:" + WaypointMissionExecuteState.find(missionStateCode).name());
                    } else {
                        LogUtil.log(TAG, "飞行器自检异常:" + Movement.getInstance().getPlaneMessage()+","+Movement.getInstance().getWarningMessage());
                        sendMissionExecuteEvents( "飞行器自检异常:" + Movement.getInstance().getPlaneMessage()+","+Movement.getInstance().getWarningMessage());
                    }
                } else {
                    LogUtil.log(TAG, "飞行器自检异常:" + Movement.getInstance().getPlaneMessage()+","+Movement.getInstance().getWarningMessage());
                    sendMissionExecuteEvents( "飞行器自检异常:" + Movement.getInstance().getPlaneMessage()+","+Movement.getInstance().getWarningMessage());
                }

            } else {
                LogUtil.log(TAG, "指点任务自检第" + checkMissionStateTimes + "次失败" + WaypointMissionExecuteState.find(missionStateCode).name() + "RTK状态:" + Movement.getInstance().isRtkSign());
                sendMissionExecuteEvents( "指点任务自检异常");
            }
        }
    }

    public void downLoadKMZFile(MQMessage message) {
        if (!TextUtils.isEmpty(message.getKmz_url())) {
            Movement.getInstance().setFlightPathName(message.getFlight_name());
            Request request = new Request.Builder().url(message.getKmz_url()).build();
            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    //下载失败，直接入库
                    LogUtil.log(TAG, "航线文件下载失败:" + e.toString());
                    if (!message.isNewRoute()) {
                        ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                        Movement.getInstance().setTaskFail(true);
                        DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
                        sendMissionExecuteEvents( "任务下载失败:"+ e.toString());
                    } else {
                        sendMissionExecuteEvents( "指点任务下载失败:"+ e.toString());
                    }
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
                            sendMissionExecuteEvents( "任务下载成功 ");
                            LogUtil.log(TAG, "航线下载成功" + WaypointMissionExecuteState.find(missionStateCode).name());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    pushKMZFileToAircraft(message);
                                }
                            });
                            checkMissionStateTimes = 0;
                        } catch (Exception e) {
                            sendMissionExecuteEvents( "任务下载,网络异常");
                            LogUtil.log(TAG, "航线下载异常:" + e.toString());
                            if (!message.isNewRoute()) {
                                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                                Movement.getInstance().setTaskFail(true);
                                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
                            }
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            });
        } else {
            sendMissionExecuteEvents( "任务url有误");
            LogUtil.log(TAG, "任务url有误");
        }

    }


    private void publishMission2Server() {
        MqttMessage flightMessage = null;
        try {
            flightMessage = new MqttMessage(new Gson().toJson(Movement.getInstance()).getBytes("UTF-8"));
        } catch (Exception e) {
            LogUtil.log(TAG, "航线状态发送异常:" + e.toString());
            throw new RuntimeException(e);
        }
        flightMessage.setQos(0);
        publish( AMSConfig.getInstance().getMqttMsdkPushMessage2ServerTopic(), flightMessage);

    }

    private int pushKMZFileTimes = 0;
    public static long pushKMZFailTimeMillis;

    public boolean isPushKMZFailTimes() {
        long time = System.currentTimeMillis();
        if (time - pushKMZFailTimeMillis > 3000) {
            pushKMZFailTimeMillis = time;
            return true;
        }
        return false;
    }

    public boolean isPushKMZSuccess;


    public void pushKMZFileToAircraft(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            //在这里重置航线开始成功的判定次数，避免指点航线失败，但这个值确是true，导致的不走回调
            isMissionStart = false;

            LogUtil.log(TAG, "航线开始上传:" + WaypointMissionExecuteState.find(missionStateCode).name());
//            WaylineCheckErrorMsg waylineCheckErrorMsg = WPMZManager.getInstance().checkValidation(Environment.getExternalStorageDirectory().getPath() + "/" + "aros.kmz");
//            List<WaylineCheckError> value = waylineCheckErrorMsg.getValue();
//            if (value != null && value.size() > 0) {
//                if (message.getIsGuidingFlight() == 0) {
//                    SystemManager.getInstance().setMediaFilePushOver(true);
//                    DroneStorageManager.getInstance().sendDroneStorageMsg2Server(client, -1);
//                }
//                sendMissionExecuteEvents( "航线文件格式有误:" + value.get(0));
//                LogUtil.log(TAG, "航线文件格式不正确:" + new Gson().toJson(value));
//                return;
//            }

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

            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();

            missionManager.pushKMZFileToAircraft(Environment.getExternalStorageDirectory().getPath() + "/" + "aros.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
                @Override
                public void onProgressUpdate(Double progress) {
                    LogUtil.log(TAG, "航线上传进度:" + progress);
                    sendMissionExecuteEvents( "航线上传进度:" + progress);
                }

                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "航线上传成功,准备执行任务");
                    sendMissionExecuteEvents( "航线上传成功,准备执行任务");
                    isPushKMZSuccess = true;

                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startMission( message);
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
                            if (!message.isNewRoute()) {
                                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                                Movement.getInstance().setTaskFail(true);
                                DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
                                sendMissionExecuteEvents( "任务上传失败,执行关机");
                                LogUtil.log(TAG, "航线第" + pushKMZFileTimes + "次上传失败,直接关机");
                            } else {
                                LogUtil.log(TAG, "指点航线第" + pushKMZFileTimes + "次上传失败");
                            }

                        }
                    } else {
                        LogUtil.log(TAG, "航线上传已经执行onSuccess回调:" + WaypointMissionExecuteState.find(missionStateCode).name());
                    }


                }

            });
        } else {
            Log.e("Aros", "设备未连接");
        }
    }

    private int startMissionFailTimes = 0;
    private boolean isMissionStart = false;

    public void startMission(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            //每次航线开始时，重置是否需要识别二维码状态，避免刚起飞就识别二维码/并确保不是飞向备降点的航线
            PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
            PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
            PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);

            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.startMission("aros", new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    isMissionStart = true;
                    LogUtil.log(TAG, "航线第" + startMissionFailTimes + "次开始成功");
                    Movement.getInstance().setFlightPathStatus(0);
                    startMissionFailTimes = 0;
                    sendMissionExecuteEvents( "任务开始执行");

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
                                        LogUtil.log(TAG, "航线第" + startMissionFailTimes + "次开始失败:" + Movement.getInstance().getGPSSignalLevel() + "---" + new Gson().toJson(error));
                                        startMissionFailTimes++;
                                    }
                                }, 2000);
                            } else {
                                if (
                                        !message.isNewRoute() &&
                                                !Movement.getInstance().isPlaneWing()) {
                                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                                    Movement.getInstance().setTaskFail(true);
                                    DroneStorageManager.getInstance().sendDroneStorageMsg2Server(-1);
                                    sendMissionExecuteEvents("任务开始失败,执行关机:" + getIDJIErrorMsg(error));
                                    LogUtil.log(TAG, "航线第" + startMissionFailTimes + "次开始失败,直接关机:" + "---" + new Gson().toJson(error) + "--" + Movement.getInstance().getGPSSignalLevel());
                                } else {
                                    sendMissionExecuteEvents( "指点任务开始失败");
                                    LogUtil.log(TAG, "指点第" + startMissionFailTimes + "次开始失败" + "---" + new Gson().toJson(error));
                                }
                            }
                        } else {
                            LogUtil.log(TAG, "航线已经执行:" + WaypointMissionExecuteState.find(missionStateCode).name());
                        }
                    }
                }
            });
        } else {
            sendMissionExecuteEvents( "任务开始失败,设备未连接");
            Log.e(TAG, "设备未连接");
        }
    }

    private boolean isManualPause;

    public void pauseMission(MQMessage message) {
        //暂停前将index保存
        PreferenceUtils.getInstance().setPauseIndex(Movement.getInstance().getCurrentWaypointIndex()+"");
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.pauseMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server( message);
                    sendPausePosition2Server();
                    LogUtil.log(TAG, "航线暂停成功");
                    Movement.getInstance().setFlightPathStatus(1);
                    isManualPause = true;
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server( message, "航线任务暂停失败:" + getIDJIErrorMsg(error));
                    LogUtil.log(TAG, "航线暂停失败:" + new Gson().toJson(error));
                }
            });
        } else {
            LogUtil.log(TAG, "航线任务暂停失败:飞控未连接");
        }
    }

    public void resumeMission(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    if (message != null) {
                        sendMsg2Server( message);
                    }
                    LogUtil.log(TAG, "航线继续成功");
                    Movement.getInstance().setFlightPathStatus(0);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    if (message != null) {
                        sendMsg2Server( message, "航线继续失败:" + getIDJIErrorMsg(error));
                    }
                    LogUtil.log(TAG, "航线继续失败:" + new Gson().toJson(error));
                }
            });
        } else {
            LogUtil.log(TAG, "设备未连接");
        }
    }

    public void stopMission(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.stopMission("aros", new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server( message);
                    LogUtil.log(TAG, "航线终止成功");
                    Movement.getInstance().setFlightPathStatus(2);
                    if (PreferenceUtils.getInstance().getIsNewRoute()){
                        Movement.getInstance().setWaylineCanResume(true);
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server( message, "航线终止失败:"+ getIDJIErrorMsg(error));
                    LogUtil.log(TAG, "航线终止失败:" + new Gson().toJson(error));
                }
            });
        } else {
            LogUtil.log(TAG, "设备未连接");
        }
    }


    public void releaseMissionKey() {
//        if (missionManager != null) {
//            missionManager.removeWaylineExecutingInfoListener(waylineExecutingInfoListener);
//            missionManager.removeWaypointMissionExecuteStateListener(waypointMissionExecuteStateListener);
//        }
    }

    //收到暂停航线命令后，发送经纬度给后端
    public void sendPausePosition2Server() {
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60200);
                message.setResult(1);
                message.setLat(Movement.getInstance().getCurrentLatitude());
                message.setLon(Movement.getInstance().getCurrentLongitude());
                message.setWaypointIndex(PreferenceUtils.getInstance().getPauseIndex());
                message.setTask_id(PreferenceUtils.getInstance().getTaskId());
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "暂停航线发送经纬度成功");
            } else {
                LogUtil.log(TAG, "暂停航线发送经纬度失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "暂停航线发送经纬度失败：mqtt 未连接");
            e.printStackTrace();
        }
    }


}