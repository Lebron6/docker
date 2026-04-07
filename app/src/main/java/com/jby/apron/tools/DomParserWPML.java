package com.jby.apron.tools;

import android.text.TextUtils;
import android.util.Log;

import com.jby.apron.entity.FlightMission;
import com.jby.apron.entity.MissionPoint;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class DomParserWPML {
    private static final String TAG = "DomParserWPML";
    private String wpmlFilePath = "";
    private String wpmlFileName = "";
    private Element docElement;
    int actionGroupId = 0;
    /**
     * 构造
     */
    public DomParserWPML(String wpmlFilePath, String wpmlFileName) {
        this.wpmlFilePath = wpmlFilePath;
        this.wpmlFileName = wpmlFileName;
    }

    /**
     * 创建kml文件
     */
    public void createWpml(FlightMission mission) {

        String fileName = wpmlFilePath + wpmlFileName;
        Document document = DocumentHelper.createDocument();// 建立document对象，用来操作xml文件
        Element kmlElement = document.addElement("kml", "http://www.opengis.net/kml/2.2");// 建立根节点
        kmlElement.addAttribute("xmlns:wpml", "http://www.dji.com/wpmz/1.0.2");
        kmlElement.addNamespace("wpml", "http://www.dji.com/wpmz/1.0.2");


        docElement = kmlElement.addElement("Document");// 添加一个Document节点

        createMissionConfig(mission);
        createFolder(mission);
        try {
            // 设置生成xml的格式
            XMLWriter writer = new XMLWriter(new FileWriter(new File(fileName)));
            writer.write(document); //写入
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("createWpml","-------");

        }
    }


    /**
     * 创建任务信息
     *
     * @return
     */
    private int createMissionConfig(FlightMission flightMission) {
        if (docElement != null) {
            try {
                Element missionConfigElement = docElement.addElement("wpml:missionConfig");
                //飞向起始点模式
                missionConfigElement.addElement("wpml:flyToWaylineMode").setText("safely");

                //航线结束动作
//                missionConfigElement.addElement("wpml:finishAction").setText("autoLand");
                missionConfigElement.addElement("wpml:finishAction").setText(flightMission.getFinishAction());

                //失控是否继续执行航线
                missionConfigElement.addElement("wpml:exitOnRCLost").setText("executeLostAction");

                //失控执行失控动作时下面元素是必须的的
                missionConfigElement.addElement("wpml:executeRCLostAction").setText("goBack");

                //安全起飞高度
                missionConfigElement.addElement("wpml:takeOffSecurityHeight").setText(String.valueOf(flightMission.getTakeOffSecurityHeight()));

                //全局航线过渡速度(飞向航线起点的速度)
                missionConfigElement.addElement("wpml:globalTransitionalSpeed").setText(String.valueOf(flightMission.getSpeed()));

                //飞行器机型信息
                Element droneInfoElement = missionConfigElement.addElement("wpml:droneInfo");
                droneInfoElement.addElement("wpml:droneEnumValue").setText(String.valueOf(77));
                droneInfoElement.addElement("wpml:droneSubEnumValue").setText(String.valueOf(1));

                //飞行器挂载信息
                Element payloadInfoElement = missionConfigElement.addElement("wpml:payloadInfo");
                payloadInfoElement.addElement("wpml:payloadEnumValue").setText(String.valueOf(66));
                payloadInfoElement.addElement("wpml:payloadSubEnumValue").setText(String.valueOf(0));
                payloadInfoElement.addElement("wpml:payloadPositionIndex").setText(String.valueOf(0));

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return -1;
            }
            return 0;
        } else {
            return -1;
        }

    }


    /**
     * 创建模板信息
     *
     * @return
     */
    public int createFolder(FlightMission mission) {
        if (docElement != null) {
            Element folderElement = docElement.addElement("Folder");

            //模板ID,范围：[0, 65535]
            folderElement.addElement("wpml:templateId").setText(String.valueOf(mission.getMissionId()));
            folderElement.addElement("wpml:executeHeightMode").setText("relativeToStartPoint");
            folderElement.addElement("wpml:waylineId").setText("2");
            folderElement.addElement("wpml:autoFlightSpeed").setText(String.valueOf(mission.getSpeed()));

            if (mission.getPoints() != null && mission.getPoints().size() > 0) {
                //航点信息（包括航点经纬度和高度等）
                for (int i = 0; i < mission.getPoints().size(); i++) {
                    addPointToPlacemark(folderElement, mission.getPoints().get(i), i);
                }
            }

            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 向 Placemark添加航点信息
     * @param parentElement
     */
    private boolean addPointToPlacemark(Element parentElement, MissionPoint missionPoint, int index) {
        if (missionPoint == null || TextUtils.isEmpty(missionPoint.getLat())||TextUtils.isEmpty(missionPoint.getLng())) {
            return false;
        } else {
            Element placemarkElement = parentElement.addElement("Placemark");
            Element point = placemarkElement.addElement("Point");

            //航点经纬度<经度,纬度>
            point.addElement("coordinates")
                    .setText(missionPoint.getLng() + "," + missionPoint.getLat());

            //航点序号
            placemarkElement.addElement("wpml:index")
                    .setText(String.valueOf(index));

            //航点高度（EGM96海拔高度/相对起飞点高度/AGL相对地面高度）useGlobalHeight 为0是必需
            placemarkElement.addElement("wpml:executeHeight")
                    .setText(String.valueOf(missionPoint.getExecuteHeight()));

            //航点飞行速度
            placemarkElement.addElement("wpml:waypointSpeed")
                    .setText(String.valueOf(missionPoint.getSpeed()));

            //该航段是否贴合直线
            placemarkElement.addElement("wpml:useStraightLine")
                    .setText(String.valueOf(1));

            //偏航角模式参数,当且仅当“wpml:useGlobalHeadingParam”为“0”时必需
            Element waypointHeadingParamElement = placemarkElement.addElement("wpml:waypointHeadingParam");
            waypointHeadingParamElement.addElement("wpml:waypointHeadingMode").setText("followWayline");

            //航点类型（航点转弯模式）,当且仅当“wpml:useGlobalTurnParam”为“0”时必需
            Element waypointTurnParamElement = placemarkElement.addElement("wpml:waypointTurnParam");
            waypointTurnParamElement.addElement("wpml:waypointTurnMode").setText("toPointAndStopWithDiscontinuityCurvature");
            waypointTurnParamElement.addElement("wpml:waypointTurnDampingDist").setText("0");

            if (missionPoint.getActions() != null && missionPoint.getActions().size() > 0) {
                Element actionGroup = placemarkElement.addElement("wpml:actionGroup");
                actionGroup.addElement("wpml:actionGroupId").setText(String.valueOf(actionGroupId++));
                actionGroup.addElement("wpml:actionGroupStartIndex").setText(String.valueOf(index));
                actionGroup.addElement("wpml:actionGroupEndIndex").setText(String.valueOf(index));
                actionGroup.addElement("wpml:actionGroupMode").setText("sequence");
                actionGroup.addElement("wpml:actionTrigger").addElement("wpml:actionTriggerType")
                        .setText("reachPoint");
                //添加航点动作
                addActionToPoint(actionGroup, index, missionPoint.getActions());
            }
            return true;
        }
    }

    private void addActionToPoint(Element actionGroup, int index, List<MissionPoint.Action> actions) {
        for (int i = 0; i < actions.size(); i++) {
            Element action = actionGroup.addElement("wpml:action");
            action.addElement("wpml:actionId").setText(String.valueOf(i));
            Element actionActuatorFunc = action.addElement("wpml:actionActuatorFunc");
            Element actionActuatorFuncParam = action.addElement("wpml:actionActuatorFuncParam");
            switch (actions.get(i).getType()) {
                //变焦
                case 1:
                    actionActuatorFunc.setText("zoom");
                    actionActuatorFuncParam.addElement("wpml:focalLength").setText(String.valueOf(Math.round(actions.get(i).getZoom() * 0.2375f)));
                    //强制使用飞行器1号挂载位置。M300 RTK机型，对应机身左前方。其它机型，对应主云台。
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    actionActuatorFuncParam.addElement("isUseFocalFactor").setText("0");
                    break;
                //拍照
                case 2:
                    actionActuatorFunc.setText("takePhoto");
                    actionActuatorFuncParam.addElement("wpml:fileSuffix").setText("point" + index);
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    break;
                //开始录像
                case 3:
                    actionActuatorFunc.setText("startRecord");
                    actionActuatorFuncParam.addElement("wpml:fileSuffix").setText("航点" + index);
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    break;
                //结束录像
                case 4:
                    actionActuatorFunc.setText("stopRecord");
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    break;
                //云台偏航角
                case 5:
                    actionActuatorFunc.setText("gimbalRotate");
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateMode")
                            .setText("absoluteAngle");
                    actionActuatorFuncParam.addElement("wpml:gimbalPitchRotateEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalPitchRotateAngle").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRollRotateEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRollRotateAngle").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalYawRotateEnable").setText("1");
                    actionActuatorFuncParam.addElement("wpml:gimbalYawRotateAngle").setText(String.valueOf(actions.get(i).getGimbalYawAngle() ));
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateTimeEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateTime").setText("0");
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    break;
                //云台俯仰角
                case 6:
                    actionActuatorFunc.setText("gimbalRotate");
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateMode").setText("absoluteAngle");
                    actionActuatorFuncParam.addElement("wpml:gimbalPitchRotateEnable").setText("1");
                    actionActuatorFuncParam.addElement("wpml:gimbalPitchRotateAngle").setText(String.valueOf(actions.get(i).getGimbalPitchAngle()));
                    actionActuatorFuncParam.addElement("wpml:gimbalRollRotateEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRollRotateAngle").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalYawRotateEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalYawRotateAngle").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateTimeEnable").setText("0");
                    actionActuatorFuncParam.addElement("wpml:gimbalRotateTime").setText("0");
                    actionActuatorFuncParam.addElement("wpml:payloadPositionIndex").setText("0");
                    break;
                //飞行器偏航角
                case 7:
                    actionActuatorFunc.setText("rotateYaw");
                    actionActuatorFuncParam.addElement("wpml:aircraftHeading").setText(String.valueOf(actions.get(i).getAircraftHeading()));
                    //强制逆时针旋转
                    actionActuatorFuncParam.addElement("wpml:aircraftPathMode").setText("counterClockwise");
                    break;
                //悬停
                case 8:
                    actionActuatorFunc.setText("hover");
                    actionActuatorFuncParam.addElement("wpml:hoverTime").setText(String.valueOf(actions.get(i).getHoverTime()));
                    break;
                default:
            }
        }
    }

    /**
     * 获取航点任务
     * @return
     */
    private String getActionActuatorFunc(String action) {
        if (action.equals("0")) {
            return "startRecord";
        } else if (action.equals("2")) {
            return "hover";
        } else if (action.equals("3")) {
            return "stopRecord";
        } else {
            return "takePhoto";
        }
    }
}