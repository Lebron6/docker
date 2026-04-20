package com.jby.apron.constant;

import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.v5.manager.KeyManager;

/**
 * MQTT参数配置
 */
public class AMSConfig {

    private AMSConfig() {
    }

    private static class MqttConfigHolder {
        private static final AMSConfig INSTANCE = new AMSConfig();
    }

    public static AMSConfig getInstance() {
        return MqttConfigHolder.INSTANCE;
    }

    private String mqttServerUri;
    private String userName;
    private String password;
    private String remoteSn;

    public String getRemoteSn() {
        return remoteSn;
    }

    public void setRemoteSn(String remoteSn) {
        this.remoteSn = remoteSn;
    }

    public String getMqttServerUri() {
        return mqttServerUri;
    }

    public void setMqttServerUri(String mqttServerUri) {
        this.mqttServerUri = mqttServerUri;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String alternateLandingTimes;

    public String getAlternateLandingTimes() {
        return alternateLandingTimes;
    }

    public void setAlternateLandingTimes(String alternateLandingTimes) {
        this.alternateLandingTimes = alternateLandingTimes;
    }


    /**
     * 服务端下发给msdk
     */
    public static String DOWN_UAV_SERVICES = "uav/service";

    /**
     * 收到服务端命令后MSDK回复
     */
    public static String UP_UAV_SERVICES_REPLY = "uav/service_reply";

    /**
     * MSDK上报事件给服务端
     */
    public static String UP_UAV_EVENT = "uav/event";

    /**
     * 服务端收到事件后回复给MSDK
     */
    public static String DOWN_UAV_EVENT_REPLY ="uav/event_reply";

    /**
     * 登录以及退出登录
     */
    public static final String REGISTER = "register";

    /**
     * 遥控器sn
     */
    public static String REMOTE_SN = "remote_sn";


}
