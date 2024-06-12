package com.aros.apron.entity;

public class GISNeedDataEntity {

    private static class GISNeedDataHolder {
        private static final GISNeedDataEntity INSTANCE = new GISNeedDataEntity();
    }

    private GISNeedDataEntity() {
    }

    public static final GISNeedDataEntity getInstance() {
        return GISNeedDataEntity.GISNeedDataHolder.INSTANCE;
    }

    private String currentLongitude;//当前经度
    private String currentLatitude;//当前纬度
    private int flyingHeight;//飞行高度
    private String roll;//机身姿态
    private String pitch;
    private String yaw;
    private String gimbalRoll;//云台角度
    private String gimbalPitch;
    private String gimbalYaw;
    private boolean planeWing;//当前飞机的桨叶是否转动


    public String getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(String currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public String getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(String currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public int getFlyingHeight() {
        return flyingHeight;
    }

    public void setFlyingHeight(int flyingHeight) {
        this.flyingHeight = flyingHeight;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getYaw() {
        return yaw;
    }

    public void setYaw(String yaw) {
        this.yaw = yaw;
    }

    public String getGimbalRoll() {
        return gimbalRoll;
    }

    public void setGimbalRoll(String gimbalRoll) {
        this.gimbalRoll = gimbalRoll;
    }

    public String getGimbalPitch() {
        return gimbalPitch;
    }

    public void setGimbalPitch(String gimbalPitch) {
        this.gimbalPitch = gimbalPitch;
    }

    public String getGimbalYaw() {
        return gimbalYaw;
    }

    public void setGimbalYaw(String gimbalYaw) {
        this.gimbalYaw = gimbalYaw;
    }

    public boolean isPlaneWing() {
        return planeWing;
    }

    public void setPlaneWing(boolean planeWing) {
        this.planeWing = planeWing;
    }



}
