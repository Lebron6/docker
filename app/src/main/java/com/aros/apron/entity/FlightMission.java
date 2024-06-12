package com.aros.apron.entity;

import java.util.List;

public class FlightMission {
    private String id;

    private Integer templateId;
    private Integer waylineId;
    private Integer missionId;

    public Integer getMissionId() {
        return missionId;
    }

    public void setMissionId(Integer missionId) {
        this.missionId = missionId;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    private Double speed=5.0;


    /**
     * 安全起飞高度[1.5, 1500] （高度模式：相对起飞点高度）
     * * 注：飞行器起飞后，先爬升至该高度，再根据“飞向首航点模式”的设置飞至首航点。该元素仅在飞行器未起飞时生效。
     */
    private Float takeOffSecurityHeight;

    //航线结束动作
    private String finishAction;


    public String getFinishAction() {
        return finishAction;
    }

    public void setFinishAction(String finishAction) {
        this.finishAction = finishAction;
    }



    /**
     * 航点信息
     * @return
     */
    private List<MissionPoint> points;

    public List<MissionPoint> getPoints() {
        return points;
    }

    public void setPoints(List<MissionPoint> points) {
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public Integer getWaylineId() {
        return waylineId;
    }

    public void setWaylineId(Integer waylineId) {
        this.waylineId = waylineId;
    }


    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }





    public Float getTakeOffSecurityHeight() {
        return takeOffSecurityHeight;
    }

    public void setTakeOffSecurityHeight(Float takeOffSecurityHeight) {
        this.takeOffSecurityHeight = takeOffSecurityHeight;
    }
}
