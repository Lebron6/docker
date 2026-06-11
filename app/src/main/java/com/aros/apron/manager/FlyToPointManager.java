package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.entity.Synchronizedstatus;
import com.aros.apron.tools.FlyToPointProgressScheduler;
import com.aros.apron.tools.LogUtil;

import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.flightcontroller.FlyToMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.intelligent.IntelligentFlightManager;
import dji.v5.manager.intelligent.flyto.FlyToParam;
import dji.v5.manager.intelligent.flyto.FlyToTarget;
import dji.v5.manager.intelligent.flyto.IFlyToMissionManager;


/**
 * 飞向目标点任务管理器
 * 使用 DJI MSDK 5.14+ 原生 IFlyToMissionManager API
 * 对应 MQTT 协议:
 *   - fly_to_point         开始飞向目标点
 *   - fly_to_point_stop    结束飞向目标点任务
 *   - fly_to_point_update  更新目标点
 */
public class FlyToPointManager extends BaseManager {

    private static final String TAG = "FlyToPointManager";

    private FlyToPointManager() {
    }

    private static class FlyToPointHolder {
        private static final FlyToPointManager INSTANCE = new FlyToPointManager();
    }

    public static FlyToPointManager getInstance() {
        return FlyToPointHolder.INSTANCE;
    }

    public volatile boolean isReceiverMission = false;

    public boolean isReceiverMission() {
        return isReceiverMission;
    }

    public void setReceiverMission(boolean receiverMission) {
        isReceiverMission = receiverMission;
    }
    // ==================== MQTT 入口 ====================

    /**
     * 收到飞往目标点指令 (fly_to_point)
     */
    public void taskExecute(MessageDown message) {
        // 参数校验
        if (message.getData() == null || message.getData().getPoints() == null
                || message.getData().getPoints().isEmpty()) {
            sendFailMsg2Server(message, "指点飞行目标点为空");
            Synchronizedstatus.setFlyto(false);
            return;
        }

        // 检查飞控连接
        Boolean isConnect = KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect == null || !isConnect) {
            sendFailMsg2Server(message, "设备未连接");
            Synchronizedstatus.setFlyto(false);
            return;
        }

        startMission(message);

    }

    /**
     * 开始飞向目标点
     * 协议: fly_to_point
     * 参数: fly_to_id, max_speed, points[{latitude, longitude, height}]
     */
    private void startMission(MessageDown message) {
        try {
            MessageDown.Data data = message.getData();
            List<MessageDown.Data.Points> points = data.getPoints();
            if (points == null || points.isEmpty()) {
                sendFailMsg2Server(message, "目标点为空");
                return;
            }

            MessageDown.Data.Points target = points.get(0);

            // 当前椭球高（起飞点椭球高)
            //double currentEllipsoid = Movement.getInstance().getRtk_takeoff_altitude();

            // 目标相对高度 = 目标椭球高(WGS84) - 当前椭球高（全程 WGS84 椭球系）
            double targetRelative = target.getHeight() - Movement.getInstance().getHeight()+Movement.getInstance().getElevation();

            // 防止高度过低炸机，最低限制20米
            if (targetRelative < 20) {
                LogUtil.log(TAG, "计算目标相对高=" + targetRelative + " < 20，限制为20米防炸机");
                targetRelative = 20;
            }

            LogUtil.log(TAG, "目标椭球高=" + target.getHeight() + " 当前椭球高=" + Movement.getInstance().getHeight()
                    + " 当前相对高=" + Movement.getInstance().getElevation() + " 最终目标相对高=" + targetRelative);

            // 构建 FlyToTarget
            FlyToTarget flyToTarget = new FlyToTarget();
            LocationCoordinate3D locationCoordinate3D=new LocationCoordinate3D();
            locationCoordinate3D.setLatitude(target.getLatitude());
            locationCoordinate3D.setLongitude(target.getLongitude());
            locationCoordinate3D.setAltitude(targetRelative);
            flyToTarget.setTargetLocation(locationCoordinate3D);

            flyToTarget.setSecurityTakeoffHeight(20);

            if (data.getMax_speed() > 0) {
                flyToTarget.setMaxSpeed(data.getMax_speed());
            } else {
                flyToTarget.setMaxSpeed(10); // 默认 10m/s
            }

            // 构建 FlyToParam
            FlyToParam flyToParam = new FlyToParam();
            // max_speed 协议定义为 int(米/秒)，范围 0-15
            flyToParam.setFlyToMode(FlyToMode.SET_HEIGHT);

            flyToParam.setHeight((int) targetRelative);


            // 如果协议携带了 fly_to_id，记录下来
            if (data.getFly_to_id() != null) {
                Movement.getInstance().setFly_to_id(data.getFly_to_id());
                LogUtil.log(TAG, "fly_to_id: " + data.getFly_to_id());
            }


            int maxSpeed = data.getMax_speed() > 0 ? data.getMax_speed() : 10;

            sendEvent2Server("开始飞向目标点: lat=" + target.getLatitude()
                    + ", lng=" + target.getLongitude()
                    + ", height=" + target.getHeight()+"targetRelative"+targetRelative, 1);

            IFlyToMissionManager manager = IntelligentFlightManager.getInstance().getFlyToMissionManager();


            manager.startMission(flyToTarget, flyToParam, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "飞向目标点任务开始成功");
                    //Movement.getInstance().setTask_current_step(23);
                    Movement.getInstance().setMode_code(17); // 指令飞行模式

                    //开始上报
                    FlyToPointProgressScheduler.getInstance().startReporting();

                    // 保存目标点信息到 Movement，供进度上报使用
                    Movement.getInstance().setFlyto_target_latitude(target.getLatitude());
                    Movement.getInstance().setFlyto_target_longitude(target.getLongitude());
                    Movement.getInstance().setFlyto_target_height((float) target.getHeight());

                    Movement.getInstance().setFlyto_max_speed(maxSpeed);

                    // 回复成功
                    sendMsg2Server(message);
                    sendEvent2Server("飞向目标点任务已启动", 1);
                    Synchronizedstatus.setFlyto(false);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    String errorMsg = "飞向目标点启动失败: " + error.description();
                    LogUtil.log(TAG, errorMsg);
                    sendFailMsg2Server(message, errorMsg);
                    sendEvent2Server(errorMsg, 2);

                    FlyToPointProgressScheduler.getInstance().markFailed();

                    Synchronizedstatus.setFlyto(false);
                }
            });
        } catch (Exception e) {
            sendFailMsg2Server(message, "飞向目标点参数异常: " + e.getMessage());
            Synchronizedstatus.setFlyto(false);
        }
    }

    // ==================== 停止任务 ====================

    /**
     * 结束飞向目标点任务 (fly_to_point_stop)
     */
    public void stopMission(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect == null || !isConnect) {
            sendFailMsg2Server(message, "设备未连接");
            return;
        }

        IFlyToMissionManager manager = IntelligentFlightManager.getInstance().getFlyToMissionManager();
        manager.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "飞向目标点任务终止成功");
                sendMsg2Server(message);
                FlyToPointProgressScheduler.getInstance().markCancel();
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                String errorMsg = "飞向目标点终止失败: " + error.description();
                LogUtil.log(TAG, errorMsg);
                sendFailMsg2Server(message, errorMsg);
            }
        });
    }

    /**
     * 更新飞向目标点 (fly_to_point_update)
     * 可在「一键起飞」或「flyto 飞向目标点」执行过程中调用
     * 参数: max_speed, points[{latitude, longitude, height}]
     */
    public void updateTarget(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect == null || !isConnect) {
            sendFailMsg2Server(message, "设备未连接");
            return;
        }
        if (!KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyIsFlying))) {
            sendFailMsg2Server(message, "飞机没起飞不允许指点");
        }
        try {
//            MessageDown.Data data = message.getData();
//            if (data == null || data.getPoints() == null || data.getPoints().isEmpty()) {
//                sendFailMsg2Server(message, "更新目标点为空");
//                return;
//            }
//
//            MessageDown.Data.Points target = data.getPoints().get(0);
//
//            // 当前椭球高（起飞点椭球高 + 相对起飞点高度）
//            //double currentEllipsoid = Movement.getInstance().getRtk_takeoff_altitude();
//
//            // 目标相对高度 = 目标椭球高(WGS84) - 当前椭球高（全程 WGS84 椭球系）
//            double targetRelative = target.getHeight() - Movement.getInstance().getHeight() + Movement.getInstance().getElevation();
//
//            // 防止高度过低炸机，最低限制20米
//            if (targetRelative < 20) {
//                LogUtil.log(TAG, "计算目标相对高=" + targetRelative + " < 20，限制为20米防炸机");
//                targetRelative = 20;
//            }
//
//            // 构建 FlyToTarget
//            FlyToTarget flyToTarget = new FlyToTarget();
//            LocationCoordinate3D locationCoordinate3D=new LocationCoordinate3D();
//            locationCoordinate3D.setLatitude(target.getLatitude());
//            locationCoordinate3D.setLongitude(target.getLongitude());
//            locationCoordinate3D.setAltitude(targetRelative);
//            flyToTarget.setTargetLocation(locationCoordinate3D);
//
//
//
//            flyToTarget.setSecurityTakeoffHeight(20);
//
//            int maxspeed;
//            if (data.getMax_speed() > 0) {
//                flyToTarget.setMaxSpeed(data.getMax_speed());
//                maxspeed=data.getMax_speed();
//            } else {
//                flyToTarget.setMaxSpeed(10); // 默认 10m/s
//                maxspeed=10;
//            }
//
//            sendEvent2Server("更新目标点: lat=" + target.getLatitude()
//                    + ", lng=" + target.getLongitude()
//                    + ", height=" + target.getHeight()+"targetRelative"+targetRelative, 1);
//
//            LogUtil.log(TAG, "updateTarget: 目标高=" + target.getHeight()
//                    + " getHeight=" + Movement.getInstance().getHeight()
//                    + " getElevation=" + Movement.getInstance().getElevation()
//                    + " targetRelative=" + targetRelative);

            IFlyToMissionManager manager = IntelligentFlightManager.getInstance().getFlyToMissionManager();

            manager.stopMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "目标点更新成功");
                    sendMsg2Server(message);
                    sendEvent2Server("目标点已更新", 1);


                    new Handler().postDelayed(()->{
                        startMissionupdate(message);
                    },1000);

                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {

                    String errorMsg = "目标点更新失败: " + idjiError;
                    LogUtil.log(TAG, errorMsg);
                    sendFailMsg2Server(message, errorMsg);

                }
            });



        } catch (Exception e) {
            sendFailMsg2Server(message, "更新目标点参数异常: " + e.getMessage());
        }
    }


    private void startMissionupdate(MessageDown message) {
        try {
            MessageDown.Data data = message.getData();
            List<MessageDown.Data.Points> points = data.getPoints();
            if (points == null || points.isEmpty()) {
                sendFailMsg2Server(message, "目标点为空");
                return;
            }

            MessageDown.Data.Points target = points.get(0);

            // 当前椭球高（起飞点椭球高)
            //double currentEllipsoid = Movement.getInstance().getRtk_takeoff_altitude();

            // 目标相对高度 = 目标椭球高(WGS84) - 当前椭球高（全程 WGS84 椭球系）
            double targetRelative = target.getHeight() - Movement.getInstance().getHeight()+Movement.getInstance().getElevation();

            // 防止高度过低炸机，最低限制20米
            if (targetRelative < 20) {
                LogUtil.log(TAG, "计算目标相对高=" + targetRelative + " < 20，限制为20米防炸机");
                targetRelative = 20;
            }

            LogUtil.log(TAG, "目标椭球高=" + target.getHeight() + " 当前椭球高=" + Movement.getInstance().getHeight()
                    + " 当前相对高=" + Movement.getInstance().getElevation() + " 最终目标相对高=" + targetRelative);

            // 构建 FlyToTarget
            FlyToTarget flyToTarget = new FlyToTarget();
            LocationCoordinate3D locationCoordinate3D=new LocationCoordinate3D();
            locationCoordinate3D.setLatitude(target.getLatitude());
            locationCoordinate3D.setLongitude(target.getLongitude());
            locationCoordinate3D.setAltitude(targetRelative);
            flyToTarget.setTargetLocation(locationCoordinate3D);

            flyToTarget.setSecurityTakeoffHeight(20);

            if (data.getMax_speed() > 0) {
                flyToTarget.setMaxSpeed(data.getMax_speed());
            } else {
                flyToTarget.setMaxSpeed(10); // 默认 10m/s
            }

            // 构建 FlyToParam
            FlyToParam flyToParam = new FlyToParam();
            // max_speed 协议定义为 int(米/秒)，范围 0-15
            flyToParam.setFlyToMode(FlyToMode.SET_HEIGHT);

            flyToParam.setHeight((int) targetRelative);


            // 如果协议携带了 fly_to_id，记录下来
            if (data.getFly_to_id() != null) {
                Movement.getInstance().setFly_to_id(data.getFly_to_id());
                LogUtil.log(TAG, "fly_to_id: " + data.getFly_to_id());
            }


            int maxSpeed = data.getMax_speed() > 0 ? data.getMax_speed() : 10;

            sendEvent2Server("开始飞向目标点: lat=" + target.getLatitude()
                    + ", lng=" + target.getLongitude()
                    + ", height=" + target.getHeight()+"targetRelative"+targetRelative, 1);

            IFlyToMissionManager manager = IntelligentFlightManager.getInstance().getFlyToMissionManager();


            manager.startMission(flyToTarget, flyToParam, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "飞向目标点任务开始成功");
                    //Movement.getInstance().setTask_current_step(23);
                    Movement.getInstance().setMode_code(17); // 指令飞行模式
                    //开始上报
                    FlyToPointProgressScheduler.getInstance().startReporting();

                    // 保存目标点信息到 Movement，供进度上报使用
                    Movement.getInstance().setFlyto_target_latitude(target.getLatitude());
                    Movement.getInstance().setFlyto_target_longitude(target.getLongitude());
                    Movement.getInstance().setFlyto_target_height((float) target.getHeight());
                    Movement.getInstance().setFlyto_max_speed(maxSpeed);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    String errorMsg = "飞向目标点启动失败: " + error.description();
                    LogUtil.log(TAG, errorMsg);

                    FlyToPointProgressScheduler.getInstance().markFailed();
                }
            });
        } catch (Exception e) {
            sendFailMsg2Server(message, "飞向目标点参数异常: " + e.getMessage());
        }
    }
}