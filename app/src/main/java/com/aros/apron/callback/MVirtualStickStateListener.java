package com.aros.apron.callback;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;

import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.v5.manager.aircraft.virtualstick.VirtualStickState;

public class MVirtualStickStateListener extends BaseManager implements dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener{

    @Override
    public void onVirtualStickStateUpdate(@NonNull VirtualStickState stickState) {
        if (stickState!=null){
            LogUtil.log(TAG,"控制权:"+stickState.isVirtualStickEnable()+"-高级模式:"+stickState.isVirtualStickAdvancedModeEnabled());
            Movement.getInstance().setIsVirtualStickEnable(stickState.isVirtualStickEnable()?1:0);
            if (!stickState.isVirtualStickEnable()){
                Movement.getInstance().setVirtualStickEnableReason(0);
            }
            Movement.getInstance().setIsVirtualStickAdvancedModeEnabled(stickState.isVirtualStickAdvancedModeEnabled()?1:0);
        }
    }

    @Override
    public void onChangeReasonUpdate(@NonNull FlightControlAuthorityChangeReason reason) {
        LogUtil.log(TAG,"控制权变更原因:"+reason.name());

    }
}
