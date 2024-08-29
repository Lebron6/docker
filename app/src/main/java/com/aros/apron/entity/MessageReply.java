package com.aros.apron.entity;

public class MessageReply {

    private int msg_type;
    private int result;
    private String msg;
    private String payloadData;//接收psdk数据
    private String waypointActionState;//推送航点动作状态
    private String aircraftTotalFlightDistance;//总体飞行距离，单位：米。飞行器断电后不会清零。
    private String flag;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getWaypointActionState() {
        return waypointActionState;
    }

    public void setWaypointActionState(String waypointActionState) {
        this.waypointActionState = waypointActionState;
    }

    public String getAircraftTotalFlightDistance() {
        return aircraftTotalFlightDistance;
    }

    public void setAircraftTotalFlightDistance(String aircraftTotalFlightDistance) {
        this.aircraftTotalFlightDistance = aircraftTotalFlightDistance;
    }

    public String getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(String payloadData) {
        this.payloadData = payloadData;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(int msg_type) {
        this.msg_type = msg_type;
    }

}
