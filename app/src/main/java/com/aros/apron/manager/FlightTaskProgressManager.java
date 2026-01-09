package com.aros.apron.manager;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.FlightTaskProgress;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo;

public class FlightTaskProgressManager extends BaseManager {


    private FlightTaskProgressManager() {
    }

    private static class FlightTaskHolder {
        private static final FlightTaskProgressManager INSTANCE = new FlightTaskProgressManager();
    }

    public static FlightTaskProgressManager getInstance() {
        return FlightTaskHolder.INSTANCE;
    }

    // 使用GsonBuilder配置Gson实例以允许序列化特殊浮点数值
    Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues() // 这是关键
            .create();

    private static final long INTERVAL = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastExecuteTime = 0L;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.elapsedRealtime();
            if (now - lastExecuteTime < INTERVAL) {
                handler.postDelayed(this, INTERVAL - (now - lastExecuteTime));
                return;
            }
            lastExecuteTime = now;
            try {
                pushFlightAttitude();
                Log.e(TAG, "打印task：" + new Gson().toJson(flightTaskProgress));
                MqttMessage flightMessage =
                        new MqttMessage(gson.toJson(flightTaskProgress).getBytes(StandardCharsets.UTF_8));
                flightMessage.setQos(0);
                publish(
                        MqttManager.getInstance().mqttAndroidClient,
                        AMSConfig.UP_UAV_EVENT,
                        flightMessage
                );

            } catch (Exception e) {
                LogUtil.log(TAG, "推送task异常: " + e);
            }

            // 始终基于“实际执行时间”来调度
            handler.postDelayed(this, INTERVAL);
        }
    };


    FlightTaskProgress.Data.Output.Ext.BreakPoint breakPoint = new FlightTaskProgress.Data.Output.Ext.BreakPoint();
    FlightTaskProgress.Data.Output.Ext ext = new FlightTaskProgress.Data.Output.Ext();
    FlightTaskProgress.Data.Output.Progress progress = new FlightTaskProgress.Data.Output.Progress();

    FlightTaskProgress.Data.Output output = new FlightTaskProgress.Data.Output();
    FlightTaskProgress.Data data = new FlightTaskProgress.Data();
    FlightTaskProgress flightTaskProgress = FlightTaskProgress.getInstance();

    BreakPointInfo mBreakPointInfo;

    private void pushFlightAttitude() {
        WaypointMissionManager.getInstance().queryBreakPointInfoFromAircraft("aros", new CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo>() {
            @Override
            public void onSuccess(BreakPointInfo breakPointInfo) {
                mBreakPointInfo=breakPointInfo;
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {

            }
        });
        breakPoint.setAttitude_head(Movement.getInstance().getTask_attitude_head());
        breakPoint.setBreak_reason(Movement.getInstance().getTask_break_reason());
        breakPoint.setHeight(Movement.getInstance().getTask_height());
        breakPoint.setIndex(0);
        breakPoint.setLatitude(Movement.getInstance().getTask_latitude());
        breakPoint.setLongitude(Movement.getInstance().getTask_longitude());
        breakPoint.setProgress(Movement.getInstance().getTask_progress());
        breakPoint.setState(Movement.getInstance().getState());
        breakPoint.setWayline_id(Movement.getInstance().getTask_wayline_id());
        ext.setCurrent_waypoint_index(Movement.getInstance().getTask_current_waypoint_index());
        ext.setFlight_id(Movement.getInstance().getTask_flight_id());
        ext.setMedia_count(Movement.getInstance().getTask_media_count());
        ext.setTrack_id(Movement.getInstance().getTrack_id());
        ext.setWayline_id(Movement.getInstance().getTask_wayline_id());
        ext.setWayline_mission_state(Movement.getInstance().getTask_wayline_mission_state());
        if (CurrentWayline.getInstance().getWaypoints()!=null){
            progress.setPercent(100 * (Movement.getInstance().getCurrentWaypointIndex() + 1)
                    / CurrentWayline.getInstance().getWaypoints().size());
        }
        progress.setCurrent_step(Movement.getInstance().getTask_current_step());

        ext.setBreak_point(breakPoint);
        output.setExt(ext);
        data.setOutput(output);
        data.setResult("ok");

        flightTaskProgress.setTid(UUID.randomUUID().toString());
        flightTaskProgress.setBid(UUID.randomUUID().toString());
        flightTaskProgress.setTimestamp(System.currentTimeMillis());
        flightTaskProgress.setMethod("flighttask_progress");
        flightTaskProgress.setData(data);
    }
}