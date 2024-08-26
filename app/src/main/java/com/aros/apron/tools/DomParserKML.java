package com.aros.apron.tools;

import android.util.Log;

import com.aros.apron.entity.FlightMission;
import com.aros.apron.entity.MissionPoint;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
 
 
public class DomParserKML {
    private static final String TAG = "DomParserKML";
    private String kmlFilePath = "";
    private String kmlFileName = "";
 
    private Element docElement;
 
    private List<MissionPoint> pointList;
    private FlightMission flightMission;
 
 
    /**
     * 构造
     *
     * @param filePath
     * @param fileName
     */
    public DomParserKML(String filePath, String fileName) {
        this.kmlFilePath = filePath;
        this.kmlFileName = fileName;
    }
 
    /**
     * 创建kml文件
     */
    public void createKml(FlightMission mission) {
//        flightMission = mission;
//        pointList =  mission.getPoints();
        String fileName = kmlFilePath + kmlFileName;
        Document document = DocumentHelper.createDocument();// 建立document对象，用来操作xml文件
        Element kmlElement = document.addElement("kml", "http://www.opengis.net/kml/2.2");// 建立根节点
        kmlElement.addAttribute("xmlns:wpml", "http://www.dji.com/wpmz/1.0.2");
        kmlElement.addNamespace("wpml", "http://www.dji.com/wpmz/1.0.2");
 
        docElement = kmlElement.addElement("Document");// 添加一个Document节点
 
//        createBaseInfo();
//        createMissionConfig();
//        createFolder();
        try {
            XMLWriter writer = new XMLWriter(new FileWriter(new File(fileName)));
            writer.write(document); //写入
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("createKml写入异常","-------");
        }
    }
 
    /**
     * 创建基本信息节点
     *
     * @return 0：添加节点成功  -1：添加节点失败
     */
//    private int createBaseInfo() {
//        if (docElement != null) {
//            docElement.addElement("wpml:author").setText(flightMission.getCreateUser() != null ?
//                    flightMission.getCreateUser() : "admin");
//
//            docElement.addElement("wpml:createTime").setText(flightMission.getCreateTime() != null ?
//                    String.valueOf(flightMission.getCreateTime().getTime()) : "0");
//
//            docElement.addElement("wpml:updateTime").setText(flightMission.getUpdateTime() != null ?
//                    String.valueOf(flightMission.getUpdateTime().getTime()) : "0");
//            return 0;
//        } else {
//            return -1;
//        }
//    }
 
    /**
     * 创建任务信息
     * @return
     */
//    private int createMissionConfig() {
//        if (docElement != null) {
//            try {
//                Element missionConfigElement = docElement.addElement("wpml:missionConfig");
//                //飞向起始点模式
//                missionConfigElement.addElement("wpml:flyToWaylineMode").setText("safely");
//
//                //航线结束动作
//                missionConfigElement.addElement("wpml:finishAction").setText("goHome");
//
//                //失控是否继续执行航线
//                missionConfigElement.addElement("wpml:exitOnRCLost").setText("executeLostAction");
//
//                //失控执行失控动作时下面元素是必须的的
//                missionConfigElement.addElement("wpml:executeRCLostAction").setText("goBack");
//
//                //安全起飞高度
//                missionConfigElement.addElement("wpml:takeOffSecurityHeight").setText(String.valueOf(flightMission.getTakeOffSecurityHeight()));
//
//                //全局航线过渡速度(飞向航线起点的速度)
//                missionConfigElement.addElement("wpml:globalTransitionalSpeed").setText(String.valueOf(flightMission.getSpeed()));
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage());
//                return -1;
//            }
//            return 0;
//        } else {
//            return -1;
//        }
//
//    }
 
    /**
     * 获取失控动作类型
     *
     * @return
     */
//    private String getExecuteRCLostAction() {
//        if (flightMission.getExecuteRCLostAction() != null) {
//            if (flightMission.getExecuteRCLostAction() == 0) {
//                //悬停
//                return "hover";
//            } else if (flightMission.getExecuteRCLostAction() == 1) {
//                //原地降落
//                return "landing";
//            } else if (flightMission.getExecuteRCLostAction() == 1) {
//                //返航
//                return "goBack";
//            } else {
//                //返航
//                return "goBack";
//            }
//        } else {
//            //返航
//            return "goBack";
//        }
//    }
 
    /**
     * 创建模板信息
     *
     * @return
     */
//    public int createFolder() {
//        if (docElement != null) {
//            Element folderElement = docElement.addElement("Folder");
//            //预定义模板类型
//            // waypoint：航点飞行
//            //mapping2d：建图航拍
//            //mapping3d：倾斜摄影
//            //mappingStrip：航带飞行
//            folderElement.addElement("wpml:templateType").setText("waypoint");
//
//            //模板ID,范围：[0, 65535]
//            folderElement.addElement("wpml:templateId").setText(String.valueOf(flightMission.getMissionId()));
//
//            //全局航线飞行速度
//            folderElement.addElement("wpml:autoFlightSpeed").setText(String.valueOf(flightMission.getSpeed()));
//
//            //全局航点类型（全局航点转弯模式）
//            //coordinateTurn：协调转弯，不过点，提前转弯
//            //toPointAndStopWithDiscontinuityCurvature：直线飞行，飞行器到点停
//            //toPointAndStopWithContinuityCurvature：曲线飞行，飞行器到点停
//            //toPointAndPassWithContinuityCurvature：曲线飞行，飞行器过点不停
//            folderElement.addElement("wpml:globalWaypointTurnMode").setText("toPointAndStopWithDiscontinuityCurvature");
//
//            //全局航段轨迹是否尽量贴合直线
//            //0：航段轨迹全程为曲线
//            //1：航段轨迹尽量贴合两点连线
//            //当且仅当“wpml:globalWaypointTurnMode”被设置为“toPointAndStopWithContinuityCurvature
//            // ”或“toPointAndPassWithContinuityCurvature”时必需。如果额外定义了某航点的该元素，则局部定义会覆盖全局定义。
//            folderElement.addElement("wpml:globalUseStraightLine").setText("1");
//
//            //云台俯仰角控制模式
//            //manual：手动控制。飞行器从一个航点飞向下一个航点的过程中，支持用户手动控制云台的俯仰角度。若无用户控制，则保持飞离航点时的云台俯仰角度。
//            //usePointSetting：依照每个航点设置。飞行器从一个航点飞向下一个航点的过程中，云台俯仰角均匀过渡至下一个航点的俯仰角。
//            folderElement.addElement("wpml:gimbalPitchMode").setText("usePointSetting");
//
//            //全局航线高度（相对起飞点高度）
//            folderElement.addElement("wpml:globalHeight").setText(String.valueOf(flightMission.getHeight()));
//
//            //全局偏航角模式参数
//            Element globalWaypointHeadingParamElement = folderElement.addElement("wpml:globalWaypointHeadingParam");
//            //云台模式
//            globalWaypointHeadingParamElement.addElement("wpml:waypointHeadingMode").setText("followWayline");
//            //飞机朝向
//            globalWaypointHeadingParamElement.addElement("wpml:waypointHeadingAngle").setText("45");
//
//
//            if (pointList != null && pointList.size() > 0) {
//                //航点信息（包括航点经纬度和高度等）
//                for (int i = 0; i < pointList.size(); i++) {
//                    addPointToPlacemark(folderElement, pointList.get(i), i);
//                }
//            }
//
//            return 0;
//        } else {
//            return -1;
//        }
//    }
 
    /**
     * 向Placemark添加航点信息
     *
     * @param parentElement
     */
//    private boolean addPointToPlacemark(Element parentElement, MissionPoint missionPoint, int index) {
//        if (missionPoint == null || missionPoint.getLat() == 0 || missionPoint.getLng() == 0) {
//            return false;
//        } else {
//            Element placemarkElement = parentElement.addElement("Placemark");
//            Element point = placemarkElement.addElement("Point");
//
//            //航点经纬度<经度,纬度>
//            point.addElement("coordinates")
//                    .setText(missionPoint.getLng() + "," + missionPoint.getLat());
//
//            //航点序号
//            placemarkElement.addElement("wpml:index")
//                    .setText(String.valueOf(index));
//
//            //是否使用全局高度
//            placemarkElement.addElement("wpml:useGlobalHeight")
//                    .setText("1");
//
//            //航点高度（WGS84椭球高度）useGlobalHeight 为0是必需
//            placemarkElement.addElement("wpml:ellipsoidHeight")
//                    .setText(String.valueOf(flightMission.getHeight()));
//
//            //航点高度（EGM96海拔高度/相对起飞点高度/AGL相对地面高度）useGlobalHeight 为0是必需
//            placemarkElement.addElement("wpml:height")
//                    .setText(String.valueOf(flightMission.getHeight()));
//
//            //是否使用全局速度
//            placemarkElement.addElement("wpml:useGlobalSpeed")
//                    .setText(missionPoint.getSpeed() > 0 ? "0" : "1");
//
//            //航点飞行速度 当且仅当“wpml:useGlobalSpeed”为“0”时必需
//            if (missionPoint.getSpeed() > 0) {
//                placemarkElement.addElement("wpml:waypointSpeed")
//                        .setText(String.valueOf(missionPoint.getSpeed()));
//            }
//
//            //是否使用全局偏航角模式参数
//            placemarkElement.addElement("wpml:useGlobalHeadingParam")
//                    .setText("1");
//            //当且仅当“wpml:useGlobalHeadingParam”为“0”时必需
////            parentElement.addElement("wpml:waypointHeadingParam")
////                    .setText("0");
//
//            //是否使用全局航点类型（全局航点转弯模式）
//            placemarkElement.addElement("wpml:useGlobalTurnParam")
//                    .setText("1");
//            //当且仅当“wpml:useGlobalTurnParam”为“0”时必需
////            parentElement.addElement("wpml:waypointTurnParam")
////                    .setText("toPointAndStopWithDiscontinuityCurvature");
//
//            //是该航段是否贴合直线
//            //0：航段轨迹全程为曲线
//            //1：航段轨迹尽量贴合两点连线
//            //当且仅当“wpml:waypointTurnParam”内"waypointTurnMode"被设置为“toPointAndStopWithContinuityCurvature
//            // ”或“toPointAndPassWithContinuityCurvature”时必需。如果此元素被设置，则局部定义会覆盖全局定义。
////            parentElement.addElement("wpml:useStraightLine")
////                    .setText("1");
//
//            //航点云台俯仰角
//            //当且仅当“wpml:gimbalPitchMode”为“usePointSetting”时必需。
//            placemarkElement.addElement("wpml:gimbalPitchAngle")
//                    .setText(String.valueOf(flightMission.getGimbalPitch()));
//
//            //航线初始动作
//            //*注：该元素用于规划一系列初始动作，在航线开始前执行。航线中断恢复时，先执行初始动作，再执行航点动作
//            Element actionGroupElement = placemarkElement.addElement("wpml:startActionGroup");
//            //动作组id
//            //* 注：在一个kmz文件内该ID唯一。建议从0开始单调连续递增。[0, 65535]
//            actionGroupElement.addElement("wpml:actionGroupId").setText(String.valueOf(flightMission.getMissionId()));
//            //动作组开始生效的航点 [0, 65535]
//            actionGroupElement.addElement("wpml:actionGroupStartIndex").setText(String.valueOf(0));
//            //动作组结束生效的航点
//            //* 注：该元素必须大于等于“actionGroupStartIndex” [0, 65535]
//            //* 注：当“动作组结束生效的航点”与“动作组开始生效的航点”一致，则代表该动作组仅在该航点处生效
//            actionGroupElement.addElement("wpml:actionGroupEndIndex").setText(String.valueOf(pointList.size() - 1));
//            //动作执行模式 sequence：串行执行。即动作组内的动作依次按顺序执行。
//            actionGroupElement.addElement("wpml:actionGroupMode").setText("sequence");
//            //动作组触发器
//            Element actionTriggerElement = actionGroupElement.addElement("wpml:actionTrigger");
//            //reachPoint：到达航点时执行
//            //betweenAdjacentPoints：航段触发，均匀转云台
//            //multipleTiming：等时触发
//            //multipleDistance：等距触发
//            //* 注：“betweenAdjacentPoints”需配合动作"gimbalEvenlyRotate"使用
//            actionTriggerElement.addElement("wpml:actionTriggerType").setText("reachPoint");
//            //动作列表
//            Element actionElement = actionGroupElement.addElement("wpml:action");
//            //动作id [0, 65535]
//            //* 注：在一个动作组内该ID唯一。建议从0开始单调连续递增。
//            actionElement.addElement("wpml:actionId").setText(String.valueOf(index));
////            actionElement.addElement("wpml:actionId").setText();
//            //动作类型
//            //	takePhoto：单拍
//            //startRecord：开始录像
//            //stopRecord：结束录像
//            //focus：对焦
//            //zoom：变焦
//            //customDirName：创建新文件夹
//            //gimbalRotate：旋转云台
//            //rotateYaw：飞行器偏航
//            //hover：悬停等待
//            //gimbalEvenlyRotate：航段间均匀转动云台pitch角
//            //accurateShoot：精准复拍动作（已暂停维护，建议使用orientedShoot）
//            //orientedShoot：精准复拍动作
//            actionElement.addElement("wpml:actionActuatorFunc").setText(getActionActuatorFunc(missionPoint.getActions().get(index).getType()));
//
//            Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
//            //负载挂载位置
//            //0：飞行器1号挂载位置。M300 RTK，M350 RTK机型，对应机身左前方。其它机型，对应主云台。
//            //1：飞行器2号挂载位置。M300 RTK，M350 RTK机型，对应机身右前方。
//            //2：飞行器3号挂载位置。M300 RTK，M350 RTK机型，对应机身上方。
//            actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").setText("0");
//            //拍摄照片文件后缀
//            actionActuatorFuncParamElement.addElement("wpml:fileSuffix").setText("point" + index);
//            //拍摄照片存储类型
//            // zoom: 存储变焦镜头拍摄照片
//            //wide: 存储广角镜头拍摄照片
//            //ir: 存储红外镜头拍摄照片
//            //narrow_band: 存储窄带镜头拍摄照片
//            //注：存储多个镜头照片，格式如“<wpml:payloadLensIndex>wide,ir,narrow_band</wpml:payloadLensIndex>”表示同时使用广角、红外和窄带镜头
//            actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").setText("wide,ir,narrow_band");
//            //是否使用全局存储类型
//            //0：不使用全局设置
//            //1：使用全局设置
//            actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").setText("0");
//
//            //飞行器悬停等待时间 单位s
//            actionActuatorFuncParamElement.addElement("wpml:hoverTime").setText("5");
//            return true;
//        }
//    }
 
    /**
     * 获取航点任务
     *
     * @return
     */
//    private String getActionActuatorFunc(String action) {
//        if (action.equals("0")) {
//            return "startRecord";
//        } else if (action.equals("2")) {
//            return "hover";
//        } else if (action.equals("3")) {
//            return "stopRecord";
//        } else {
//            return "takePhoto";
//        }
//    }
}