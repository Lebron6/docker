package com.aros.apron.callback;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.Movement;

import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener;
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo;


public class MWaylineExecutingInfoListener extends BaseManager implements dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener {


    @Override
    public void onWaylineExecutingInfoUpdate(WaylineExecutingInfo excutingWaylineInfo) {
        if (excutingWaylineInfo!=null){
            //状态等执行完发送再更新
            Movement.getInstance().setCurrentWaypointIndex(excutingWaylineInfo.getCurrentWaypointIndex());
            Movement.getInstance().setTask_current_waypoint_index(excutingWaylineInfo.getCurrentWaypointIndex());
//            if (CurrentWayline.getInstance().getWaypoints()!=null){
//                Movement.getInstance().setTask_percent(100 * (Movement.getInstance().getCurrentWaypointIndex() + 1)
//                        / CurrentWayline.getInstance().getWaypoints().size());
//            }
//            if (Movement.getInstance().getMissionStateCode()==WaypointMissionExecuteState.EXECUTING.value()){
//
//            }
        }

    }

    @Override
    public void onWaylineExecutingInterruptReasonUpdate(IDJIError error) {
    }
}
