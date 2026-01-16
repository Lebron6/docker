package com.aros.apron.callback;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;

import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState;

public class MWaypointMissionExecuteStateListener extends BaseManager implements dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener {

    //在ENTER_WAYLINE后10秒，航线状态变为FINISH，此时无人机不起飞/悬停
    private long enterWayLineTime;
    private long finishWayLineTime;
    //已经发送离开最后一个航点
    private boolean alreadySendLeaveLastPoint;
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onMissionStateUpdate(WaypointMissionExecuteState missionState) {
        if (missionState != null) {
            switch (missionState) {
                case DISCONNECTED:
                    Movement.getInstance().setAirlineFlight(false);
                    Movement.getInstance().setWaylineCanResume(false);
                    sendEvent2Server( "任务状态:未连接");
                    break;
                case IDLE:
                    Movement.getInstance().setAirlineFlight(false);
                    Movement.getInstance().setWaylineCanResume(false);
                    sendEvent2Server( "任务状态:初始化");
//                    sendFlightTaskProgress2Server();

                    break;
                case NOT_SUPPORTED:
                    Movement.getInstance().setAirlineFlight(false);
                    Movement.getInstance().setWaylineCanResume(false);
                    sendEvent2Server( "任务状态:此机型不支持航线任务3.0");
//                    sendFlightTaskProgress2Server();

                    break;
                case READY:
                    Movement.getInstance().setAirlineFlight(false);
                    if (PreferenceUtils.getInstance().getIsNewRoute()&&
                            !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                        Movement.getInstance().setWaylineCanResume(true);
                    }else {
                        Movement.getInstance().setWaylineCanResume(false);
                    }
                    sendEvent2Server( "任务状态:准备中");
//                    sendFlightTaskProgress2Server();

                    break;
                case UPLOADING:
                    Movement.getInstance().setAirlineFlight(false);
                    if (PreferenceUtils.getInstance().getIsNewRoute()&&
                            !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                        Movement.getInstance().setWaylineCanResume(true);
                    }else {
                        Movement.getInstance().setWaylineCanResume(false);
                    }
                    sendEvent2Server( "任务状态:上传中");
                    Movement.getInstance().setVirtualStickQuitMission(false);
                    sendFlightTaskProgress2Server();

                    break;
                case PREPARING:
                    Movement.getInstance().setAirlineFlight(false);
                    if (PreferenceUtils.getInstance().getIsNewRoute()&&
                            !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                        Movement.getInstance().setWaylineCanResume(true);
                    }else {
                        Movement.getInstance().setWaylineCanResume(false);
                    }
                    sendEvent2Server( "任务状态:执行准备中");
                    Movement.getInstance().setVirtualStickQuitMission(false);
                    sendFlightTaskProgress2Server();

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
                    sendEvent2Server( "任务状态:进入航线飞行,飞往指定航线的第一个航点");
                    sendFlightTaskProgress2Server();
                    break;
                case EXECUTING:
                    Movement.getInstance().setAirlineFlight(true);
                    sendEvent2Server( "任务状态:航线任务执行中");
                    if (PreferenceUtils.getInstance().getIsNewRoute()&&
                            !(Movement.getInstance().getCurrentWaypointIndex()>0)){
                        Movement.getInstance().setWaylineCanResume(true);
                    }else {
                        Movement.getInstance().setWaylineCanResume(false);
                    }
                    Movement.getInstance().setVirtualStickQuitMission(false);
                    //发送航线任务进度
                    sendFlightTaskProgress2Server();
                    break;
                case INTERRUPTED:
                    Movement.getInstance().setAirlineFlight(true);
                    Movement.getInstance().setWaylineCanResume(false);
                    Movement.getInstance().setVirtualStickQuitMission(false);
                    sendEvent2Server( "任务状态:航线任务执行中断");
                    sendFlightTaskProgress2Server();

                    break;
                case RECOVERING:
                    Movement.getInstance().setAirlineFlight(true);
                    Movement.getInstance().setWaylineCanResume(false);
                    Movement.getInstance().setVirtualStickQuitMission(false);
                    sendEvent2Server( "任务状态:航线任务恢复中");
                    sendFlightTaskProgress2Server();

                    break;
                case FINISHED:
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
                    sendFlightTaskProgress2Server();

                    break;
            }
            LogUtil.log(TAG, "WaypointMissionExecuteState:" + missionState.name());
            Movement.getInstance().setWaypointMissionExecuteState(missionState.name());
            Movement.getInstance().setTask_wayline_mission_state(missionState.value());
            Movement.getInstance().setMissionStateCode(missionState.value());


        }
    }
}
