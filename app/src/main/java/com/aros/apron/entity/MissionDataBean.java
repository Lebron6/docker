package com.aros.apron.entity;


/**
 * 服务端下发的整条 JSON 对应实体
 */
public class MissionDataBean {

    /** 备降点 */
    private LatLngAlt alternate_land_point;

    /** 指挥官飞行高度 */
    private int commander_flight_height;

    /** 指挥官模式丢失动作 */
    private int commander_mode_lost_action;

    /** 航线唯一编号 */
    private String flight_id;

    /** 最大飞行速度 (m/s) */
    private int max_speed;

    /** 遥控丢失动作 */
    private int rc_lost_action;

    /** 返航高度 (m) */
    private int rth_altitude;

    /** 安全起飞高度 (m) */
    private double security_takeoff_height;

    /** 目标高度 (m) */
    private double target_height;

    /** 目标纬度 (°) */
    private double target_latitude;

    /** 目标经度 (°) */
    private double target_longitude;

    /* ---------------- getter / setter ---------------- */
    public LatLngAlt getAlternateLandPoint() {
        return alternate_land_point;
    }
    public void setAlternateLandPoint(LatLngAlt alternate_land_point) {
        this.alternate_land_point = alternate_land_point;
    }
    public int getCommanderFlightHeight() {
        return commander_flight_height;
    }
    public void setCommanderFlightHeight(int commander_flight_height) {
        this.commander_flight_height = commander_flight_height;
    }
    public int getCommanderModeLostAction() {
        return commander_mode_lost_action;
    }
    public void setCommanderModeLostAction(int commander_mode_lost_action) {
        this.commander_mode_lost_action = commander_mode_lost_action;
    }
    public String getFlightId() {
        return flight_id;
    }
    public void setFlightId(String flight_id) {
        this.flight_id = flight_id;
    }
    public int getMaxSpeed() {
        return max_speed;
    }
    public void setMaxSpeed(int max_speed) {
        this.max_speed = max_speed;
    }
    public int getRcLostAction() {
        return rc_lost_action;
    }
    public void setRcLostAction(int rc_lost_action) {
        this.rc_lost_action = rc_lost_action;
    }
    public int getRthAltitude() {
        return rth_altitude;
    }
    public void setRthAltitude(int rth_altitude) {
        this.rth_altitude = rth_altitude;
    }
    public double getSecurityTakeoffHeight() {
        return security_takeoff_height;
    }
    public void setSecurityTakeoffHeight(int security_takeoff_height) {
        this.security_takeoff_height = security_takeoff_height;
    }
    public double getTargetHeight() {
        return target_height;
    }
    public void setTargetHeight(double target_height) {
        this.target_height = target_height;
    }
    public double getTargetLatitude() {
        return target_latitude;
    }
    public void setTargetLatitude(double target_latitude) {
        this.target_latitude = target_latitude;
    }
    public double getTargetLongitude() {
        return target_longitude;
    }
    public void setTargetLongitude(double target_longitude) {
        this.target_longitude = target_longitude;
    }

    /* ---------------- 内部嵌套类：备降点 ---------------- */
    public static class LatLngAlt {
        private double latitude;
        private double longitude;
        private double safe_land_height;

        public double getLatitude() {
            return latitude;
        }
        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
        public double getSafeLandHeight() {
            return safe_land_height;
        }
        public void setSafeLandHeight(int safe_land_height) {
            this.safe_land_height = safe_land_height;
        }
    }
}