package com.aros.apron.callback;

import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;

import dji.v5.common.error.IDJIError;

public class MWaylineExecu extends BaseManager implements dji.v5.manager.aircraft.waypoint3.WaypointActionListener {
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
}
