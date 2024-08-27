package com.aros.apron.entity;

import com.dji.industry.mission.waypointv2.common.waypointv2.ActionType;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.value.mission.Waypoint;

public class FlightPath {

    private List<WaypointActionGroup> waypointActionGroups;

    public FlightPath() {
        this.waypointActionGroups = new ArrayList<>();
    }

    public void addWaypointActionGroup(WaypointActionGroup group) {
        this.waypointActionGroups.add(group);
    }

    public List<WaypointActionGroup> getWaypointActionGroups() {
        return waypointActionGroups;
    }

    // 其他getter和setter方法...

    @Override
    public String toString() {
        return "FlightPath{" +
                "waypointActionGroups=" + waypointActionGroups +
                '}';
    }
}

 class WaypointActionGroup {

    private List<WaypointAction> actions;

    public WaypointActionGroup() {
        this.actions = new ArrayList<>();
    }

    public void addAction(WaypointAction action) {
        this.actions.add(action);
    }

    public List<WaypointAction> getActions() {
        return actions;
    }

    // 其他getter和setter方法...

    @Override
    public String toString() {
        return "WaypointActionGroup{" +
                "actions=" + actions +
                '}';
    }
}

 class WaypointAction {

    private Waypoint waypoint;
    private ActionType actionType;
    private int duration; // in seconds

    public WaypointAction(Waypoint waypoint, ActionType actionType, int duration) {
        this.waypoint = waypoint;
        this.actionType = actionType;
        this.duration = duration;
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public int getDuration() {
        return duration;
    }

    // 其他getter和setter方法...

    @Override
    public String toString() {
        return "WaypointAction{" +
                "waypoint=" + waypoint +
                ", actionType=" + actionType +
                ", duration=" + duration +
                '}';
    }
}

