package com.aros.apron.tools;

import com.google.gson.annotations.SerializedName;

public class DjiMetaData {

    @SerializedName("AbsoluteAltitude")
    public String absoluteAltitude;

    @SerializedName("RelativeAltitude")
    public String relativeAltitude;

    @SerializedName("CreateDate")
    public String createDate;

    @SerializedName("GpsLatitude")
    public String gpsLatitude;

    @SerializedName("GpsLongitude")
    public String gpsLongitude;

    @SerializedName("DroneModel")
    public String droneModel;

    @SerializedName("GimbalRollDegree")
    public String gimbalRollDegree;

    @SerializedName("GimbalYawDegree")
    public String gimbalYawDegree;

    @SerializedName("GimbalPitchDegree")
    public String gimbalPitchDegree;

    public String getAbsoluteAltitude() {
        return absoluteAltitude;
    }

    public void setAbsoluteAltitude(String absoluteAltitude) {
        this.absoluteAltitude = absoluteAltitude;
    }

    public String getRelativeAltitude() {
        return relativeAltitude;
    }

    public void setRelativeAltitude(String relativeAltitude) {
        this.relativeAltitude = relativeAltitude;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getGimbalRollDegree() {
        return gimbalRollDegree;
    }

    public void setGimbalRollDegree(String gimbalRollDegree) {
        this.gimbalRollDegree = gimbalRollDegree;
    }

    public String getGimbalYawDegree() {
        return gimbalYawDegree;
    }

    public void setGimbalYawDegree(String gimbalYawDegree) {
        this.gimbalYawDegree = gimbalYawDegree;
    }

    public String getGimbalPitchDegree() {
        return gimbalPitchDegree;
    }

    public void setGimbalPitchDegree(String gimbalPitchDegree) {
        this.gimbalPitchDegree = gimbalPitchDegree;
    }

    public String getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(String gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public String getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(String gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public String getDroneModel() {
        return droneModel;
    }

    public void setDroneModel(String droneModel) {
        this.droneModel = droneModel;
    }
}