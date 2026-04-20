package com.jby.apron.constant;

/**
 * MQTT参数配置
 */
public class MqttConfig {

    private MqttConfig() {
    }

    private static class MqttConfigHolder {
        private static final MqttConfig INSTANCE = new MqttConfig();
    }

    public static MqttConfig getInstance() {
        return MqttConfigHolder.INSTANCE;
    }

    private String downUavServiceTopic;
    private String upUavServiceReplyTopic;
    private String upUavEventTopic;
    private String downUavEventReplyTopic;
    private String remoteSn;

    public String getRemoteSn() {
        return remoteSn;
    }

    public void setRemoteSn(String remoteSn) {
        this.remoteSn = remoteSn;
    }

    public String getDownUavServiceTopic() {
        return downUavServiceTopic;
    }

    public void setDownUavServiceTopic(String downUavServiceTopic) {
        this.downUavServiceTopic = downUavServiceTopic;
    }

    public String getUpUavServiceReplyTopic() {
        return upUavServiceReplyTopic;
    }

    public void setUpUavServiceReplyTopic(String upUavServiceReplyTopic) {
        this.upUavServiceReplyTopic = upUavServiceReplyTopic;
    }

    public String getUpUavEventTopic() {
        return upUavEventTopic;
    }

    public void setUpUavEventTopic(String upUavEventTopic) {
        this.upUavEventTopic = upUavEventTopic;
    }

    public String getDownUavEventReplyTopic() {
        return downUavEventReplyTopic;
    }

    public void setDownUavEventReplyTopic(String downUavEventReplyTopic) {
        this.downUavEventReplyTopic = downUavEventReplyTopic;
    }
}
