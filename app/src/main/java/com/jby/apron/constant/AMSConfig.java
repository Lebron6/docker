package com.jby.apron.constant;

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
    public static String DOWN_UAV_SERVICES = "nest/uav_services";

    /**
     * 收到服务端命令后MSDK回复
     */
    public static String UP_UAV_SERVICES_REPLY = "nest/uav_services_reply";

    /**
     * MSDK上报事件给服务端
     */
    public static String UP_UAV_EVENT = "nest/uav_event";

    /**
     * 服务端收到事件后回复给MSDK
     */
    public static String DOWN_UAV_EVENT_REPLY = "nest/uav_event_reply";


}
