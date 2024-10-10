package com.aros.apron.entity;

import java.util.Date;
import java.util.List;

public class MissionPoint {
    private String lng;
    private String lat;
    private double waypointSpeed;
    //航点执行高度
    private double executeHeight;

    // 录像：0,
    // 拍照：1,
    // 悬停：2
    // 停止录像：3
    private List<Action> actions;

    //执行时间
    private Date executeTime;

    public double getExecuteHeight() {
        return executeHeight;
    }

    public void setExecuteHeight(double executeHeight) {
        this.executeHeight = executeHeight;
    }


    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public double getSpeed() {
        return waypointSpeed;
    }

    public void setSpeed(double speed) {
        this.waypointSpeed = speed;
    }


    public Date getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Date executeTime) {
        this.executeTime = executeTime;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public static class Action{
        //1 变焦 2拍照 3开始录像 4结束录像 5云台偏航角 6云台俯仰角 7飞行器偏航角 8悬停
        private int type;
        private int zoom;
        private int gimbalYawAngle;
        private int gimbalPitchAngle;
        private int aircraftHeading;
        private int hoverTime;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getZoom() {
            return zoom;
        }

        public void setZoom(int zoom) {
            this.zoom = zoom;
        }

        public int getGimbalYawAngle() {
            return gimbalYawAngle;
        }

        public void setGimbalYawAngle(int gimbalYawAngle) {
            this.gimbalYawAngle = gimbalYawAngle;
        }

        public int getGimbalPitchAngle() {
            return gimbalPitchAngle;
        }

        public void setGimbalPitchAngle(int gimbalPitchAngle) {
            this.gimbalPitchAngle = gimbalPitchAngle;
        }

        public int getAircraftHeading() {
            return aircraftHeading;
        }

        public void setAircraftHeading(int aircraftHeading) {
            this.aircraftHeading = aircraftHeading;
        }

        public int getHoverTime() {
            return hoverTime;
        }

        public void setHoverTime(int hoverTime) {
            this.hoverTime = hoverTime;
        }
    }
}
