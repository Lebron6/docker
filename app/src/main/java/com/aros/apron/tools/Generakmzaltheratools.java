package com.aros.apron.tools;

import com.aros.apron.base.BaseManager;
import com.aros.apron.manager.TaskFailManager;
import com.dji.wpmzsdk.common.data.Template;
import com.dji.wpmzsdk.common.utils.kml.GpsUtils;
import com.dji.wpmzsdk.manager.WPMZManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.wpmz.value.mission.WaylineAltitudeMode;
import dji.sdk.wpmz.value.mission.WaylineCheckErrorMsg;
import dji.sdk.wpmz.value.mission.WaylineCoordinateMode;
import dji.sdk.wpmz.value.mission.WaylineCoordinateParam;
import dji.sdk.wpmz.value.mission.WaylineDroneInfo;
import dji.sdk.wpmz.value.mission.WaylineDroneType;
import dji.sdk.wpmz.value.mission.WaylineExitOnRCLostAction;
import dji.sdk.wpmz.value.mission.WaylineExitOnRCLostBehavior;
import dji.sdk.wpmz.value.mission.WaylineFinishedAction;
import dji.sdk.wpmz.value.mission.WaylineFlyToWaylineMode;
import dji.sdk.wpmz.value.mission.WaylineLocationCoordinate2D;
import dji.sdk.wpmz.value.mission.WaylineMission;
import dji.sdk.wpmz.value.mission.WaylineMissionConfig;
import dji.sdk.wpmz.value.mission.WaylinePositioningType;
import dji.sdk.wpmz.value.mission.WaylineTemplateWaypointInfo;
import dji.sdk.wpmz.value.mission.WaylineWaypoint;
import dji.sdk.wpmz.value.mission.WaylineWaypointGimbalHeadingMode;
import dji.sdk.wpmz.value.mission.WaylineWaypointGimbalHeadingParam;
import dji.sdk.wpmz.value.mission.WaylineWaypointTurnMode;
import dji.sdk.wpmz.value.mission.WaylineWaypointYawMode;
import dji.sdk.wpmz.value.mission.WaylineWaypointYawParam;
import dji.v5.manager.KeyManager;

/**
 * 单航点起飞→目标点→返航
 * 纯 Java、无 Builder、无 WaypointHeadingParam
 */
public class Generakmzaltheratools extends BaseManager {
    private Generakmzaltheratools() {}
    private static class Holder { private static final Generakmzaltheratools INSTANCE = new Generakmzaltheratools(); }
    public static Generakmzaltheratools getInstance() { return Holder.INSTANCE; }

    public Boolean generateKmz() {
        LogUtil.log(TAG, "备降点经纬度:" + PreferenceUtils.getInstance().getAlternatePointLat() +
                "/" + PreferenceUtils.getInstance().getAlternatePointLon());
        LogUtil.log("qwq","开始生成备降点航线");
        sendEvent2Server("开始生成备降点航线", 1);
        LocationCoordinate3D lat = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyAircraftLocation3D));

        int retryCount = 0;
        final int MAX_RETRY = 60;

        while (retryCount < MAX_RETRY) {
            lat = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D));
            if (lat != null) {
                break;
            }
            retryCount++;
            sendEvent2Server("生成失败：未获取到飞机当前位置（重试" + retryCount + "次）", 1);
            if (retryCount >= MAX_RETRY) {
                sendEvent2Server("生成失败：未获取到飞机当前位置（重试" + MAX_RETRY + "次超时）", 1);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                return false;
            }

            try {
                Thread.sleep(1000); // 休眠1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUtil.log("qwq", "获取位置线程被中断");
                sendEvent2Server("生成失败：获取位置被中断", 1);
                TaskFailManager.getInstance().sendTaskFailMsg2Server(-1);
                return false;
            }
        }


        if (lat == null) {
            LogUtil.log("qwq", "当前位置为空，无法生成航线");
            sendEvent2Server("生成失败：未获取到飞机当前位置", 1);
            return false;
        }
        if (lat.getLatitude() == null || lat.getLongitude() == null|| lat.getAltitude() == null) {
            LogUtil.log("qwq", "当前位置坐标无效");
            sendEvent2Server("生成失败：位置坐标无效", 1);
            return false;
        }

        File root = new File("/storage/self/primary/DJIDemo/cache/kmz");
        if (!root.exists()) root.mkdirs();
        File kmz = new File(root, "alternate.kmz");
        LogUtil.log(TAG,"高度："+lat.getAltitude());

        /* 1. <wpml:mission> 基本信息 */
        WaylineMission mission = new WaylineMission();
        mission.setAuthor("aros");
        double now = System.currentTimeMillis();
        mission.setCreateTime(now);
        mission.setUpdateTime(now);

        /* 2. <wpml:missionConfig> 全局策略 */
        WaylineMissionConfig config = new WaylineMissionConfig();
        //飞向目标点的模式
        config.setFlyToWaylineMode(WaylineFlyToWaylineMode.SAFELY);
        //航线结束后的动作
        config.setFinishAction(WaylineFinishedAction.AUTO_LAND);
        config.setExitOnRCLostBehavior(WaylineExitOnRCLostBehavior.GO_ON);
        config.setExitOnRCLostType(WaylineExitOnRCLostAction.LANDING);

        //全局过度速度
        config.setGlobalTransitionalSpeed(7.0);

        //全局返航高度
        double wpellheight=GpsUtils.wgs84Altitude(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()),Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLat()),Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLon()));

        config.setGlobalRTHHeight(wpellheight);
        config.setIsGlobalRTHHeightSet(true);


        //安全起飞高度
        config.setSecurityTakeOffHeight(wpellheight);
        config.setIsSecurityTakeOffHeightSet(true);



        //为400准备的
        WaylineDroneInfo droneInfo = new WaylineDroneInfo();
        droneInfo.setDroneType(WaylineDroneType.PM440);
        config.setDroneInfo(droneInfo);


        //这个航点列表
        WaylineWaypoint wp1 = new WaylineWaypoint();
        WaylineWaypoint wp = new WaylineWaypoint();

        //初始点
        double wp1heighet= GpsUtils.egm96Altitude(lat.getAltitude(),lat.getLatitude(),lat.getLongitude());
        //double wpheight=GpsUtils.egm96Altitude(data.getTargetHeight(),data.getTargetLatitude(),data.getTargetLongitude());

        //double wp1ellheighet=GpsUtils.wgs84Altitude();
        //double wpellheight=GpsUtils.wgs84Altitude(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()),Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLat()),Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLon()));

        wp1.setWaypointIndex(0);
        wp1.setLocation(new WaylineLocationCoordinate2D(lat.getLatitude(), lat.getLongitude()));
        //wp1.setLocation(new WaylineLocationCoordinate2D(data.getTargetLatitude(), data.getTargetLongitude()));
        //wp1.setHeight((double)data.getCommanderFlightHeight());
        wp1.setHeight(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));
        //wp.setUseGlobalFlightHeight(true);
        wp1.setEllipsoidHeight(wpellheight);

        wp1.setUseStraightLine(true);
        wp1.setUseGlobalTurnParam(true);
        wp1.setUseGlobalYawParam(true);
        wp1.setUseGlobalGimbalHeadingParam(true);
        wp1.setUseGlobalAutoFlightSpeed(true);
        wp1.setIsRisky(false);



        //基础设置
        wp.setWaypointIndex(1);
        wp.setLocation(new WaylineLocationCoordinate2D(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLat()), Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointLon())));
        //wp.setHeight((double)data.getCommanderFlightHeight());
        wp.setHeight(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));
        //wp.setUseGlobalFlightHeight(true);
        //椭球高
        wp.setEllipsoidHeight(wpellheight);



        wp.setUseStraightLine(true);
        wp.setUseGlobalTurnParam(true);
        wp.setUseGlobalYawParam(true);
        wp.setUseGlobalGimbalHeadingParam(true);
        wp.setUseGlobalAutoFlightSpeed(true);
        wp.setIsRisky(false);


        List<WaylineWaypoint> wpList = new ArrayList<>();
        wpList.add(wp1);
        wpList.add(wp);

        /* 4. <wpml:waypointInfo> */
        WaylineTemplateWaypointInfo waypointInfo = new WaylineTemplateWaypointInfo();

        waypointInfo.setGlobalFlightHeight(Double.parseDouble(PreferenceUtils.getInstance().getAlternatePointSecurityHeight()));
        waypointInfo.setIsGlobalFlightHeightSet(true);

        WaylineWaypointYawParam waypointYawParam=new WaylineWaypointYawParam();
        waypointYawParam.setYawMode(WaylineWaypointYawMode.FOLLOW_WAYLINE);
        waypointInfo.setGlobalYawParam(waypointYawParam);
        waypointInfo.setIsTemplateGlobalYawParamSet(true);

        WaylineWaypointGimbalHeadingParam waypointGimbalHeadingParam=new WaylineWaypointGimbalHeadingParam();
        waypointGimbalHeadingParam.setHeadingMode(WaylineWaypointGimbalHeadingMode.FOLLOW_WAYLINE);
        waypointGimbalHeadingParam.setYawAngle(0.0);
        waypointGimbalHeadingParam.setPitchAngle(0.0);
        waypointInfo.setGlobalGimbalHeadingParam(waypointGimbalHeadingParam);

        waypointInfo.setGlobalTurnMode(WaylineWaypointTurnMode.COORDINATE_TURN);
        waypointInfo.setIsTemplateGlobalTurnModeSet(true);
        waypointInfo.setWaypoints(wpList);

        waypointInfo.setUseStraightLine(true);



        /* 5. <wpml:coordinateParam> */
        WaylineCoordinateParam coordParam = new WaylineCoordinateParam();
        coordParam.setCoordinateMode(WaylineCoordinateMode.WGS84);

        coordParam.setAltitudeMode(WaylineAltitudeMode.EGM96);
        coordParam.setPositioningType(WaylinePositioningType.GPS);

        /* 6. <wpml:template> */
        Template template = new Template();
        template.setTemplateId(0);
        template.setCoordinateParam(coordParam);
        template.setTransitionalSpeed(10.0);
        template.setWaypointInfo(waypointInfo);
        template.setUseGlobalTransitionalSpeed(true);
        template.setAutoFlightSpeed(10.0);

        //不写就报错
        template.setPayloadParam(new ArrayList<>());
        /* 7. 生成文件 */
        WPMZManager.getInstance().generateKMZFile(kmz.getAbsolutePath(), mission, config, template);
        //验证这个kmz

        WaylineCheckErrorMsg result= WPMZManager.getInstance().checkValidation(kmz.getAbsolutePath());
        LogUtil.log("qwq",result.toString());
        if (result.getValue().isEmpty()) {
            LogUtil.log("qwq", "KMZ 校验通过");
            sendEvent2Server("备降点航线生成成功", 1);
            return true;
        } else {
            // 其他错误码照旧报错
            LogUtil.log("qwq", "校验失败，错误码：" + result.getValue());
            sendEvent2Server("备降点航线文件生成失败错误码："+result.getValue(), 1);
            return false;
        }
    }
}