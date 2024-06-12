package com.aros.apron.constant;

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
        return AMSConfig.MqttConfigHolder.INSTANCE;
    }

    //直接降落的超声波高度
    private int descentUltrasonicAltitude;
    //直接降落的椭球高度
    private double descentAltitude;

    private String mqttServerUri;
    private String userName;
    private String password;
    private String serialNumber;

    private String mqttServer2MsdkTopic;
    private String mqttMsdkReplyMessage2ServerTopic;
    private String mqttMsdkPushMessage2ServerTopic;
    private String mqttMsdkPushGisMessage2ServerTopic;
    private String mqttMsdkPushEvent2ServerTopic;
    private String alternateLandingTimes;

    public String getAlternateLandingTimes() {
        return alternateLandingTimes;
    }

    public void setAlternateLandingTimes(String alternateLandingTimes) {
        this.alternateLandingTimes = alternateLandingTimes;
    }

    public double getDescentAltitude() {
        return descentAltitude;
    }

    public void setDescentAltitude(double descentAltitude) {
        this.descentAltitude = descentAltitude;
    }

    public int getDescentUltrasonicAltitude() {
        return descentUltrasonicAltitude;
    }

    public void setDescentUltrasonicAltitude(int descentUltrasonicAltitude) {
        this.descentUltrasonicAltitude = descentUltrasonicAltitude;
    }

    public String getMqttMsdkPushEvent2ServerTopic() {
        return mqttMsdkPushEvent2ServerTopic;
    }

    public void setMqttMsdkPushEvent2ServerTopic(String mqttMsdkPushEvent2ServerTopic) {
        this.mqttMsdkPushEvent2ServerTopic = mqttMsdkPushEvent2ServerTopic;
    }

    public String getMqttServer2MsdkTopic() {
        return mqttServer2MsdkTopic;
    }

    public void setMqttServer2MsdkTopic(String mqttServer2MsdkTopic) {
        this.mqttServer2MsdkTopic = mqttServer2MsdkTopic;
    }

    public String getMqttMsdkReplyMessage2ServerTopic() {
        return mqttMsdkReplyMessage2ServerTopic;
    }

    public void setMqttMsdkReplyMessage2ServerTopic(String mqttMsdkReplyMessage2ServerTopic) {
        this.mqttMsdkReplyMessage2ServerTopic = mqttMsdkReplyMessage2ServerTopic;
    }

    public String getMqttMsdkPushMessage2ServerTopic() {
        return mqttMsdkPushMessage2ServerTopic;
    }

    public void setMqttMsdkPushMessage2ServerTopic(String mqttMsdkPushMessage2ServerTopic) {
        this.mqttMsdkPushMessage2ServerTopic = mqttMsdkPushMessage2ServerTopic;
    }

    public String getMqttMsdkPushGisMessage2ServerTopic() {
        return mqttMsdkPushGisMessage2ServerTopic;
    }

    public void setMqttMsdkPushGisMessage2ServerTopic(String mqttMsdkPushGisMessage2ServerTopic) {
        this.mqttMsdkPushGisMessage2ServerTopic = mqttMsdkPushGisMessage2ServerTopic;
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

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * 服务器IP地址
     */
    //朋业测试
//    public static String SOCKET_HOST = "tcp://212.64.64.69:1883";
//    public static String USER_NAME = "admin";
//    public static String USER_PASSWORD = "1qw23er45t";
    //西安机库
//    public static String SERIAL_NUMBER = "323RFDSVDTHBDFVSD";
    //太仓单开门
//    public static String SERIAL_NUMBER = "1581F5FJB229Q00A003W";
    //太仓御三机库
//    public static String SERIAL_NUMBER = "1581F5FJB229Q00A003Y";
    //太仓御三机库2
//    public static String SERIAL_NUMBER = "1581F5FJB229Q00A003A";
    //太仓ARS350
//    public static String SERIAL_NUMBER = "1581F5FJB229Q00A003980890809";

    /**
     * 接收指令
     */
//    public String MQTT_SERVER_2_MSDK_TOPIC = "nest/" + serialNumber + "/uav_services";

    /**
     * 回复结果
     */
//    public static String MQTT_MSDK_REPLY_MESSAGE_2_SERVER_TOPIC = "nest/" + SERIAL_NUMBER + "/uav_services_reply";

    /**
     * 推送飞行状态
     */
//    public static String MQTT_MSDK_PUSH_MESSAGE_2_SERVER_TOPIC = "nest/" + SERIAL_NUMBER + "/uav_status_message";

    /**
     * 推送gis飞行状态（GIS需要定频2S）
     */
//    public static String MQTT_MSDK_PUSH_GIS_MESSAGE_2_SERVER_TOPIC = "nest/" + SERIAL_NUMBER + "/uav_gis_message";

}
