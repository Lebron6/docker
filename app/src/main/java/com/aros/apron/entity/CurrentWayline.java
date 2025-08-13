package com.aros.apron.entity;


import java.util.ArrayList;
import java.util.List;

import dji.sdk.wpmz.value.mission.WaylineWaypoint;

public class CurrentWayline {

    private static class CurrentWaylineHolder {
        private static final CurrentWayline INSTANCE = new CurrentWayline();
    }

    private CurrentWayline() {
    }

    public static final CurrentWayline getInstance() {
        return CurrentWaylineHolder.INSTANCE;
    }

    private List<WaylineWaypoint> waypoints=new ArrayList<>();


    public List<WaylineWaypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<WaylineWaypoint> waypoints) {
        this.waypoints = waypoints;
    }
}
