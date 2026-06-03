package com.aros.apron.manager;

import static android.os.Environment.getExternalStoragePublicDirectory;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.FlightMission;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.MissionPoint;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DomParserKML;
import com.aros.apron.tools.DomParserWPML;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.Utils;
import com.aros.apron.tools.ZipUtil;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.interfaces.IWaypointMissionManager;


public class FlyToPointManager extends BaseManager {

    final Handler mainHandler = new Handler(Looper.getMainLooper());


    private FlyToPointManager() {
    }

    private static class FlyToPointHolder {
        private static final FlyToPointManager INSTANCE = new FlyToPointManager();
    }

    public static FlyToPointManager getInstance() {
        return FlyToPointHolder.INSTANCE;
    }

    public boolean isReceiverMission = false;

    //收到飞往目标点航线
    public void taskExecute(MessageDown message) {
//        PreferenceUtils.getInstance().setMissionType(2);
        PreferenceUtils.getInstance().setIsNewRoute(true);
        //避免重复执行
        if (isReceiverMission == false) {
            isReceiverMission = true;
        }
        if (message.getData().getPoints() != null && message.getData().getPoints().size() > 0) {
            sendMsg2Server(message);
        } else {
            sendFailMsg2Server(message, "指点飞行经纬度有误");
            return;
        }
        toGenerateKMZFile(message);
    }

    /**
     * 5.生成航线
     *
     * @param message
     */
    public void toGenerateKMZFile(MessageDown message) {
        Movement.getInstance().setTask_current_step(16);
        // 创建第一个 MissionPoint 对象
        MissionPoint missionPoint = new MissionPoint();
        missionPoint.setLat(String.valueOf(Movement.getInstance().getLatitude()));
        missionPoint.setLng(String.valueOf(Movement.getInstance().getLongitude()));
        missionPoint.setSpeed(Double.parseDouble(message.getData().getMax_speed()));
        missionPoint.setExecuteHeight(message.getData().getHeight());

        // 创建第二个 MissionPoint 对象
        MissionPoint missionPoint1 = new MissionPoint();
        missionPoint1.setLat(message.getData().getPoints().get(0).getLatitude() + "");
        missionPoint1.setLng(message.getData().getPoints().get(0).getLongitude() + "");
        missionPoint1.setSpeed(Double.parseDouble(message.getData().getMax_speed()));
        missionPoint1.setExecuteHeight(message.getData().getHeight());

        // 创建一个 MissionPoint 列表
        List<MissionPoint> missionPoints = new ArrayList<>();
        missionPoints.add(missionPoint);
        missionPoints.add(missionPoint1);

        // 创建 FlightMission 对象并设置其属性
        FlightMission flightMission = new FlightMission();
        flightMission.setPoints(missionPoints);
        flightMission.setMissionId(2);
        flightMission.setFinishAction("noAction");
        flightMission.setTakeOffSecurityHeight(
                Float.parseFloat(Movement.getInstance().getElevation() + ""));
        flightMission.setSpeed(Double.parseDouble(message.getData().getMax_speed()));

        LogUtil.log(TAG, "当前高度:" + Movement.getInstance().getElevation()
                + "-指点飞行安全起飞高度:" + message.getData().getSecurity_takeoff_height());

        sendEvent2Server("开始生成指点飞行航线", 1);

        // 生成xml文件
        File file1 = new File(
                getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz");
        if (!file1.exists()) {
            if (file1.mkdirs()) {
                sendEvent2Server("指点飞行航线文件生成成功", 1);
            } else {
                sendEvent2Server("指点飞行航线文件生成失败", 2);
            }
        }
        DomParserKML domParserKML = new DomParserKML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/template.kml");
        domParserKML.createKml(flightMission);

        DomParserWPML domParserWPML = new DomParserWPML(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + File.separator + "wpmz",
                "/waylines.wpml");
        domParserWPML.createWpml(flightMission);

        File kmzFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "FlyTo.kmz");
        kmzFile.getParentFile().mkdirs();

        try {
            ZipUtil.zip(getExternalStoragePublicDirectory("KMZ").getAbsolutePath() + "/wpmz",
                    getExternalStoragePublicDirectory("KMZ").getAbsolutePath()
                            + File.separator + "FlyTo.kmz");
        } catch (IOException e) {
            sendEvent2Server("指点飞行任务生成异常", 2);
            throw new RuntimeException(e);
        }
        pushKMZFileToAircraft(message);
    }


    /**
     * 6.上传指点航线
     *
     * @param message
     */
    private void pushKMZFileToAircraft(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            WaypointMissionManager.getInstance().pushKMZFileToAircraft(Environment.getExternalStorageDirectory().getPath() + "/" + "FlyTo.kmz", new CommonCallbacks.CompletionCallbackWithProgress<Double>() {
                @Override
                public void onProgressUpdate(Double progress) {
                    sendEvent2Server("指点航线上传进度:" + progress, 1);
                    Movement.getInstance().setTask_current_step(17);
                }

                @Override
                public void onSuccess() {
                    sendEvent2Server("指点航线上传成功,准备执行任务", 1);
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            /**
                             * 7.开始任务
                             */
                            Movement.getInstance().setTask_current_step(22);
                            startMission(message);
                        }
                    }, 2000);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                   sendFailMsg2Server(message,"指点航线上传失败:"+ Utils.getIDJIErrorMsg(error));
                }
            });
        }
    }


    /**
     * 6.开始航线
     *
     * @param message
     */
    public void startMission(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            CommonCallbacks.CompletionCallback callback = new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "指点航线开始成功");
                    Movement.getInstance().setTask_status("paused");
                    sendEvent2Server("任务开始执行", 1);
                    Movement.getInstance().setTask_current_step(23);
                    sendFlightTaskProgress2Server();
//                    Movement.getInstance().setMode_code(5);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server(message,"指点航线执行失败:" + new Gson().toJson(error));
                }
            };
            WaypointMissionManager.getInstance().startMission("ToPoint", callback);
        } else {
            sendEvent2Server("指点任务开始失败,设备未连接", 2);
        }
    }

    public void stopMission(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
            missionManager.stopMission("ToPoint", new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(message);
                    LogUtil.log(TAG, "指点任务终止成功");
                    Movement.getInstance().setTask_status("paused");
                    sendFlightTaskProgress2Server();
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendFailMsg2Server(message, "指点任务终止失败:" + getIDJIErrorMsg(error));
                }
            });
        } else {
            LogUtil.log(TAG, "指点任务终止失败:设备未连接");
        }
    }

}
