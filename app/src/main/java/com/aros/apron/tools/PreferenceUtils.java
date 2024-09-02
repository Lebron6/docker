package com.aros.apron.tools;

import android.content.Context;
import android.text.TextUtils;

import com.aros.apron.app.ApronApp;
import com.aros.apron.entity.MQMessage;


public class PreferenceUtils extends BasePreference {
    private String TAG = "PreferenceUtils";

    private static PreferenceUtils preferenceUtils;

    /**
     * 需要增加key就在这里新建
     */

    private String RTMP_PUSH_URL = "rtmp_push_url";//推流地址
    private String UPLOAD_URL = "upload_url";//minIO上传地址
    private String ACCESS_KEY = "access_key";
    private String SECRET_KEY = "secret_key";
    private String BUCKET_NAME = "bucket_name";
    private String FLIGHT_NAME = "flight_name";
    private String KEY = "key";
    private String SORTIES_ID = "sortiesId";
    private String FLIGHT_ID = "flightId";
    private String NEED_TRIGGER_APRON_ARUCO_LAND = "need_trigger_apron_aruco_land";//是否需要触发crash后继续降落到机库
    private String NEED_TRIGGER_ALTER_ARUCO_LAND = "need_trigger_alter_aruco_land";//是否需要触发crash后继续降落到备降点
    private String TRIGGER_TO_ALTERNATE_POINT = "trigger_to_alternate_point";//是否需要触发crash后继续降落

    //AMS配置清单
    private String HAVA_RTK = "have_rtk";
    private String CUSTOM_STREAM_ENABLE = "custom_stream";
    private String CUSTOM_STREAM_URL = "custom_stream_url";
    private String NTR_IP = "ntr_ip";
    private String NTR_PORT = "ntr_port";
    private String NTR_ACCOUNT = "ntr_account";
    private String NTR_PASSWORD = "ntr_password";
    private String NTR_MOUNT_POINT = "ntr_mount_point";
    private String MQTT_SERVER_URI = "mqtt_server_uri";
    private String MQTT_USERNAME = "mqtt_username";
    private String MQTT_PASSWORD = "mqtt_password";
    private String MQTT_SN = "mqtt_sn";
    private String NEED_UPLOAD_VEDIO = "need_upload_vedio";
    private String AIRPORT_TYPE = "airport_type";
    private String LANDING_TYPE = "land_type"; //1RTK优先 2视觉优先
    private String RTK_TYPE = "rtk_type"; //1自定义网络RTK 2DJI赠送RTK
    private String DOCKER_LON = "docker_lon"; //机库经纬度
    private String DOCKER_LAT = "docker_lat"; //机库经纬度
    private String AIRCRAFT_HEADING = "aircraft_heading"; //记录无人机在机库的起飞朝向
    private String ALTERNATE_POINT_LON = "alternate_point_lon"; //备降点经纬度
    private String ALTERNATE_POINT_LAT = "alternate_point_lat"; //备降点经纬度
    private String ALTERNATE_POINT_SECURITY_HEIGHT = "alternate_point_security_height"; //备降点安全起飞高度
    private String ALTERNATE_POINT_HEIGHT = "alternate_point_height"; //备降点安全起飞高度
    private String ALTERNATE_POINT_TIMES = "alternate_point_times"; //允许复降次数
    private String DEBUG_MODE = "debug_mode"; //调试模式
    private String NAVIGATION_LEDS_ON = "navigation_LEDs_On"; //夜航灯
    private String CLOSE_OBS_ENABLE = "close_obstacle_enable"; //是否关闭避障
    private String MISSION_INTERRUPT_ACTION = "mission_interrupt_action"; //航线终止后动作

    public void setStreamAndMinIOConfig(MQMessage message) {
        setString(RTMP_PUSH_URL, getRTMPUrl());
        if (!TextUtils.isEmpty(message.getUpload_url())) {
            String[] split = message.getUpload_url().split("//");
            String[] split1 = split[1].split("/");
            if (!(split1.length < 4)) {
                setString(UPLOAD_URL, "http://" + split1[0]);
                setString(BUCKET_NAME, split1[1]);
                setString(KEY, split1[2]);
                setString(SORTIES_ID, split1[3]);
            } else {
                LogUtil.log(TAG, "minio参数有误");
            }
            setString(ACCESS_KEY, message.getAccess_key());
            setString(SECRET_KEY, message.getSecret_key());
            setString(FLIGHT_NAME, message.getFlight_name());
            setString(FLIGHT_ID, message.getFlightId());

        } else {
            LogUtil.log(TAG, "minio参数有误:地址为空");
        }
    }

    public String getFlightName() {
        return getString(FLIGHT_NAME);
    }

    public String getSortiesId() {
        return getString(SORTIES_ID);
    }

    public String getFlightId() {
        return getString(FLIGHT_ID);
    }

    public String getBucketName() {
        return getString(BUCKET_NAME);
    }

    public String getKey() {
        return getString(KEY);
    }

    public String getSecretKey() {
        return getString(SECRET_KEY);
    }

    public String getAccessKey() {
        return getString(ACCESS_KEY);
    }

    public String getUploadUrl() {
        return getString(UPLOAD_URL);
    }

    public String getRTMPUrl() {
        return getString(RTMP_PUSH_URL);
    }

    public boolean getHaveRTK() {
        return getBoolean(HAVA_RTK);
    }

    public void setHaveRtk(boolean haveRtk) {
        setBoolean(HAVA_RTK, haveRtk);
    }

    public int getRtkType() {
        return getInt(RTK_TYPE);
    }

    public void setRtkType(int rtkType) {
        setInt(RTK_TYPE, rtkType);
    }

    public void setNeedTriggerApronArucoLand(boolean needTrigger) {
        setBoolean(NEED_TRIGGER_APRON_ARUCO_LAND, needTrigger);
    }

    public boolean getNeedTriggerApronArucoLand() {
        return getBoolean(NEED_TRIGGER_APRON_ARUCO_LAND);
    }

    public void setNeedTriggerAlterArucoLand(boolean needTrigger) {
        setBoolean(NEED_TRIGGER_ALTER_ARUCO_LAND, needTrigger);
    }

    public boolean getNeedTriggerAlterArucoLand() {
        return getBoolean(NEED_TRIGGER_ALTER_ARUCO_LAND);
    }

    public void setTriggerToAlternatePoint(boolean trigger) {
        setBoolean(TRIGGER_TO_ALTERNATE_POINT, trigger);
    }

    public boolean getTriggerToAlternatePoint() {
        return getBoolean(TRIGGER_TO_ALTERNATE_POINT);
    }

    public void setCustomStreamEnable(boolean customStreamEnable) {
        setBoolean(CUSTOM_STREAM_ENABLE, customStreamEnable);
    }

    public boolean getCustomStreamEnable() {
        return getBoolean(CUSTOM_STREAM_ENABLE);
    }

    public void setCustomStreamUrl(String customStreamUrl) {
        setString(CUSTOM_STREAM_URL, customStreamUrl);
    }

    public String getCustomStreamUrl() {
        return getString(CUSTOM_STREAM_URL);
    }

    public String getNTRIP() {
        return getString(NTR_IP);
    }

    public void setNTRIP(String ntrip) {
        setString(NTR_IP, ntrip);
    }

    public String getNTRPort() {
        return getString(NTR_PORT);
    }

    public void setNTRPort(String ntrport) {
        setString(NTR_PORT, ntrport);
    }

    public String getNTRAccount() {
        return getString(NTR_ACCOUNT);
    }

    public void setNTRAccount(String ntrAccount) {
        setString(NTR_ACCOUNT, ntrAccount);
    }

    public String getNTRPassword() {
        return getString(NTR_PASSWORD);
    }

    public void setNTRPassword(String ntrPassword) {
        setString(NTR_PASSWORD, ntrPassword);
    }

    public String getNTRMountPoint() {
        return getString(NTR_MOUNT_POINT);
    }

    public void setNTRMountPoint(String ntrMountPoint) {
        setString(NTR_MOUNT_POINT, ntrMountPoint);
    }

    public String getMqttServerUri() {
        return getString(MQTT_SERVER_URI);
    }

    public void setMqttServerUri(String mqttServerUri) {
        setString(MQTT_SERVER_URI, mqttServerUri);
    }

    public String getMqttUserName() {
        return getString(MQTT_USERNAME);
    }

    public void setMqttUserName(String mqttUserName) {
        setString(MQTT_USERNAME, mqttUserName);
    }

    public String getMqttPassword() {
        return getString(MQTT_PASSWORD);
    }

    public void setMqttPassword(String mqttPassword) {
        setString(MQTT_PASSWORD, mqttPassword);
    }

    public String getMqttSn() {
        return getString(MQTT_SN);
    }

    public void setMqttSn(String mqttSn) {
        setString(MQTT_SN, mqttSn);
    }

    public boolean getNeedUpLoadVideo() {
        return getBoolean(NEED_UPLOAD_VEDIO);
    }

    public void setNeedUpLoadVideo(boolean needUpLoadVideo) {
        setBoolean(NEED_UPLOAD_VEDIO, needUpLoadVideo);
    }

    public int getAirPortType() {
        return getInt(AIRPORT_TYPE);
    }

    public void setAirPortType(int airPortType) {
        setInt(AIRPORT_TYPE, airPortType);
    }

    public int getMissionInterruptAction() {
        return getInt(MISSION_INTERRUPT_ACTION);
    }

    public void setMissionInterruptAction(int missionInterruptAction) {
        setInt(MISSION_INTERRUPT_ACTION, missionInterruptAction);
    }

    public int getLandType() {
        return getInt(LANDING_TYPE);
    }

    public void setLandType(int landType) {
        setInt(LANDING_TYPE, landType);
    }

    public String getDockerLat() {
        return getString(DOCKER_LAT);
    }

    public void setDockerLat(String dockerLat) {
        setString(DOCKER_LAT, dockerLat);
    }

    public String getDockerLon() {
        return getString(DOCKER_LON);
    }

    public void setDockerLon(String dockerLon) {
        setString(DOCKER_LON, dockerLon);
    }

    public String getAircraftHeading() {
        return getString(AIRCRAFT_HEADING);
    }

    public void setAircraftHeading(String dockerLon) {
        setString(AIRCRAFT_HEADING, dockerLon);
    }

    public String getAlternatePointLat() {
        return getString(ALTERNATE_POINT_LAT);
    }

    public void setAlternatePointLat(String alternate_point_lat) {
        setString(ALTERNATE_POINT_LAT, alternate_point_lat);
    }

    public String getAlternatePointLon() {
        return getString(ALTERNATE_POINT_LON);
    }

    public void setAlternatePointLon(String alternate_point_lon) {
        setString(ALTERNATE_POINT_LON, alternate_point_lon);
    }

    public String getAlternatePointSecurityHeight() {
        return getString(ALTERNATE_POINT_SECURITY_HEIGHT);
    }

    public void setAlternatePointSecurityHeight(String alternatePointSecurityHeight) {
        setString(ALTERNATE_POINT_SECURITY_HEIGHT, alternatePointSecurityHeight);
    }

    public String getAlternatePointHeight() {
        return getString(ALTERNATE_POINT_HEIGHT);
    }

    public void setAlternatePointHeight(String alternatePointHeight) {
        setString(ALTERNATE_POINT_HEIGHT, alternatePointHeight);
    }

    public String getAlternatePointTimes() {
        return getString(ALTERNATE_POINT_TIMES);
    }

    public void setAlternatePointTimes(String alternatePointTimes) {
        setString(ALTERNATE_POINT_TIMES, alternatePointTimes);
    }

    public boolean getIsDebugMode() {
        return getBoolean(DEBUG_MODE);
    }

    public void setIsDebugMode(boolean debugMode) {
        setBoolean(DEBUG_MODE, debugMode);
    }

    public boolean getNavigationLEDsOn() {
        return getBoolean(NAVIGATION_LEDS_ON);
    }

    public void setNavigationLEDsOn(boolean navigationLEDsOn) {
        setBoolean(NAVIGATION_LEDS_ON, navigationLEDsOn);
    }

    public boolean getCloseObsEnable() {
        return getBoolean(CLOSE_OBS_ENABLE);
    }

    public void setCloseObsEnable(boolean close_obs_enable) {
        setBoolean(CLOSE_OBS_ENABLE, close_obs_enable);
    }

    private PreferenceUtils(Context context) {
        super(context);
    }

    public synchronized static PreferenceUtils getInstance() {
        if (null == preferenceUtils) {
            preferenceUtils = new PreferenceUtils(ApronApp.Companion.getApplication());
        }
        return preferenceUtils;
    }
}