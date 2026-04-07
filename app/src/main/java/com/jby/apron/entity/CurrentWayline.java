package com.jby.apron.entity;


import java.util.ArrayList;
import java.util.List;
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint;

public class CurrentWayline {

    private static class CurrentWaylineHolder {
        private static final CurrentWayline INSTANCE = new CurrentWayline();
    }

    private CurrentWayline() {
    }

    public static final CurrentWayline getInstance() {
        return CurrentWaylineHolder.INSTANCE;
    }

    private List<WaylineExecuteWaypoint> waypoints=new ArrayList<>();


    public List<WaylineExecuteWaypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<WaylineExecuteWaypoint> waypoints) {
        this.waypoints = waypoints;
    }

    private List<WaylineExecuteWaypoint> routeWaypoints=new ArrayList<>();


    public List<WaylineExecuteWaypoint> getRouteWaypoints() {
        return routeWaypoints;
    }

    public void setRouteWaypoints(List<WaylineExecuteWaypoint> waypoints) {
        this.routeWaypoints = waypoints;
    }
}
