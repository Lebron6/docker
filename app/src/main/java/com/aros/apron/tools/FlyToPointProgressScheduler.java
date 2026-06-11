package com.aros.apron.tools;

import android.os.Handler;
import android.os.Looper;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.FlyToPointProgress;
import com.aros.apron.entity.Movement;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.v5.manager.KeyManager;

/**
 * 飞向目标点进度定时上报器
 * 协议: fly_to_point_progress
 */
public class FlyToPointProgressScheduler {

    private static final String TAG = "FlyToPointProgressScheduler";
    private static volatile FlyToPointProgressScheduler instance;

    private Handler reportHandler;
    private Runnable reportRunnable;

    private volatile boolean isRunning = false;

    // 轨迹点缓存
    private List<FlyToPointProgress.PlannedPathPoint> pathPoints;

    // 采样间隔 1秒
    private static final long REPORT_INTERVAL = 1000;

    // 当前状态
    private String currentStatus = "wayline_progress";
    private int currentResult = 0;

    private FlyToPointProgressScheduler() {
        reportHandler = new Handler(Looper.getMainLooper());
        pathPoints = new ArrayList<>();
    }

    public static FlyToPointProgressScheduler getInstance() {
        if (instance == null) {
            synchronized (FlyToPointProgressScheduler.class) {
                if (instance == null) {
                    instance = new FlyToPointProgressScheduler();
                }
            }
        }
        return instance;
    }

    /**
     * 开始定时上报
     */
    public void startReporting() {
        if (isRunning) {
            LogUtil.log(TAG, "定时上报已在运行中");
            return;
        }
        isRunning = true;
        currentStatus = "wayline_progress";
        currentResult = 0;
        pathPoints.clear();

        LogUtil.log(TAG, "启动飞向目标点进度定时上报");

        reportRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                try {
                    // 1. 采样飞机位置
                    new Thread(() -> sampleCurrentLocation()).start();
                    // 2. 发送上报
                    sendProgressReport();
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.log(TAG, "定时任务异常：" + e.getMessage());
                }

                if (isRunning) {
                    reportHandler.postDelayed(this, REPORT_INTERVAL);
                }
            }
        };

        // 立即执行第一次
        reportHandler.post(reportRunnable);
    }

    /**
     * 停止定时上报
     */
    public void stopReporting() {
        if (!isRunning) return;
        isRunning = false;
        // 最后发送一次
        sendProgressReport();
        reportHandler.removeCallbacks(reportRunnable);
        pathPoints.clear();
        LogUtil.log(TAG, "停止飞向目标点进度定时上报");
    }

    /**
     * 标记任务成功
     */
    public void markSuccess() {
        currentStatus = "wayline_ok";
        currentResult = 0;
        stopReporting();
    }

    /**
     * 标记任务失败
     */
    public void markFailed() {
        currentStatus = "wayline_failed";
        currentResult = -1;
        stopReporting();
    }

    /**
     * 标记任务取消
     */
    public void markCancel() {
        currentStatus = "wayline_cancel";
        currentResult = 0;
        stopReporting();
    }

    /**
     * 采样当前飞机位置
     */
    private void sampleCurrentLocation() {
        try {
            LocationCoordinate3D location = KeyManager.getInstance().getValue(
                    KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D));

            if (location == null) return;

            double lat = location.getLatitude();
            double lng = location.getLongitude();
            double alt = location.getAltitude();

            // 过滤无效坐标
            if (Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001) return;

            FlyToPointProgress.PlannedPathPoint point = new FlyToPointProgress.PlannedPathPoint();
            point.setLatitude(lat);
            point.setLongitude(lng);
            point.setHeight((float) alt);

            // 计算剩余距离和时间
            double remaining_distance = Gpsdistance.calculate3DDistance(
                    lat, lng, alt,
                    Movement.getInstance().getFlyto_target_latitude(),
                    Movement.getInstance().getFlyto_target_longitude(),
                    Movement.getInstance().getFlyto_target_height());

            int speed = Movement.getInstance().getFlyto_max_speed();
            double remaining_time = speed > 0 ? remaining_distance / speed : 0;

            Movement.getInstance().setFlyto_remaining_distance((float) remaining_distance);
            Movement.getInstance().setFlyto_remaining_time((float) remaining_time);

            // 刷新规划路径点（3个：飞机当前位置 → 中间点 → 目标点）
            pathPoints.clear();
            pathPoints.add(point);

            // 中间点 = 当前位置与目标点连线的中点
            FlyToPointProgress.PlannedPathPoint midPoint = new FlyToPointProgress.PlannedPathPoint();
            midPoint.setLatitude((lat + Movement.getInstance().getFlyto_target_latitude()) / 2.0);
            midPoint.setLongitude((lng + Movement.getInstance().getFlyto_target_longitude()) / 2.0);
            midPoint.setHeight((float) ((alt + Movement.getInstance().getFlyto_target_height()) / 2.0));
            pathPoints.add(midPoint);

            // 目标点
            FlyToPointProgress.PlannedPathPoint targetPoint = new FlyToPointProgress.PlannedPathPoint();
            targetPoint.setLatitude(Movement.getInstance().getFlyto_target_latitude());
            targetPoint.setLongitude(Movement.getInstance().getFlyto_target_longitude());

            targetPoint.setHeight((float) Movement.getInstance().getFlyto_target_height());

            pathPoints.add(targetPoint);

        } catch (Exception e) {
            LogUtil.log(TAG, "采样位置失败：" + e.getMessage());
        }
    }

    /**
     * 发送进度上报
     */
    public void sendProgressReport() {
        try {
            if (!MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                LogUtil.log(TAG, "MQTT未连接，跳过上报");
                return;
            }
            FlyToPointProgress.Data data = new FlyToPointProgress.Data();
            data.setFly_to_id(Movement.getInstance().getFly_to_id());
            data.setStatus(currentStatus);
            data.setResult(currentResult);
            data.setWay_point_index(1);
            data.setRemaining_distance(Movement.getInstance().getFlyto_remaining_distance());
            data.setRemaining_time(Movement.getInstance().getFlyto_remaining_time());
            data.setPlanned_path_points(new ArrayList<>(pathPoints));

            FlyToPointProgress progress = new FlyToPointProgress();
            progress.setBid(UUID.randomUUID().toString());
            progress.setTid(UUID.randomUUID().toString());
            progress.setTimestamp(System.currentTimeMillis());
            progress.setMethod("fly_to_point_progress");
            progress.setNeedReply(1);
            progress.setData(data);

            String json = new Gson().toJson(progress);
            MqttMessage mqttMessage = new MqttMessage(json.getBytes("UTF-8"));
            mqttMessage.setQos(0);
            MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);

            LogUtil.log(TAG, "上报进度:"+json);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "上报异常：" + e.getMessage());
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        stopReporting();
        instance = null;
    }
}