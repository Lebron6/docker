package com.jby.apron.base;

import static com.jby.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jby.apron.constant.AMSConfig;
import com.jby.apron.constant.Constant;
import com.jby.apron.entity.CurrentWayline;
import com.jby.apron.entity.FlightTaskProgress;
import com.jby.apron.entity.MediaUpLoad;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.MessageEvent;
import com.jby.apron.entity.MessageReply;
import com.jby.apron.entity.Movement;
import com.jby.apron.entity.TaskFailEvent;
import com.jby.apron.entity.WirelessLink;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.MqttManager;
import com.jby.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo;

public abstract class BaseManager {

    public String TAG = getClass().getSimpleName();


    /**
     * 响应reply
     * @param entity
     */
    public void sendMsg2Server(MessageDown entity) {
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageReply messageReply = new MessageReply();
                messageReply.setBid(entity.getBid());
                messageReply.setTid(entity.getTid());
                messageReply.setTimestamp(entity.getTimestamp());
                messageReply.setMethod(entity.getMethod());
                MessageReply.Data data=new MessageReply.Data();
                data.setResult(0);
                messageReply.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageReply).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_SERVICES_REPLY, mqttMessage);
            } else {
                LogUtil.log(TAG, "回复失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复异常：" + e.toString());
        }
    }
    final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 响应replay+event
     * @param entity
     * @param errorMsg
     */
    public void sendFailMsg2Server(MessageDown entity,String errorMsg) {
        LogUtil.log(TAG,entity.getMethod()+":"+errorMsg);
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageReply messageReply = new MessageReply();
                messageReply.setBid(entity.getBid());
                messageReply.setTid(entity.getTid());
                messageReply.setTimestamp(entity.getTimestamp());
                messageReply.setMethod(entity.getMethod());
                MessageReply.Data data=new MessageReply.Data();
                data.setResult(-1);
                data.setErrorMsg(errorMsg);
                messageReply.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageReply).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_SERVICES_REPLY, mqttMessage);

//                if (!entity.getMethod().equals(Constant.AIRCRAFT_ON)){
//                    //这里通过event事件上报执行动作失败
//                    mainHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
//                            } catch (MqttException e) {
//                                throw new RuntimeException(e);
//                            }
//
//                        }
//                    },500);
//                }

            } else {
                LogUtil.log(TAG, "回复失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复异常：" + e.toString());
        }
    }


    public void publish(MqttAndroidClient client, String topic, MqttMessage message) {
        try {
            if (client.isConnected()) {
                client.publish(topic, message);
            } else {
                LogUtil.log(TAG, "推送飞机状态失败:mqtt未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "推送飞机状态失败异常:" + topic + e.toString());
        }
    }

    /**
     * 发送常规event事件
     */
    public void sendEvent2Server(String msg,int level) {
        LogUtil.log(TAG,"发送常规事件:"+"run_log  -"+msg);
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod("run_log");
                MessageEvent.Data data=new MessageEvent.Data();
                data.setMsg(msg);
                data.setLevel(level);
                if (TextUtils.isEmpty(PreferenceUtils.getInstance().getFlightId())){
                    data.setFlight_id("null");
                }else{
                    data.setFlight_id(PreferenceUtils.getInstance().getFlightId());
                }
                messageEvent.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
            } else {
                LogUtil.log(TAG, "发送run_log event失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复event异常：" + e.toString());
        }
    }

    /**
     * 发送媒体文件上传事件
     */
    public void sendMediaUpload2Server(String fileName,int uploaded_file_count,int expected_file_count) {
        LogUtil.log(TAG,"发送媒体上传完成事件:"+fileName);
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MediaUpLoad mediaUpLoad = new MediaUpLoad();
                mediaUpLoad.setBid(UUID.randomUUID().toString());
                mediaUpLoad.setTid(UUID.randomUUID().toString());
                mediaUpLoad.setTimestamp(System.currentTimeMillis());
                mediaUpLoad.setMethod(Constant.FILE_UPLOAD_CALLBACK);
                MediaUpLoad.Data data=new MediaUpLoad.Data();
                data.setBucket_name(PreferenceUtils.getInstance().getBucketName());
                data.setObject_key(PreferenceUtils.getInstance().getObjectKey());
                data.setFlight_id(PreferenceUtils.getInstance().getFlightId());
                data.setFile_name(fileName);
                data.setUploaded_file_count(uploaded_file_count);
                data.setExpected_file_count(expected_file_count);
                mediaUpLoad.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(mediaUpLoad).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
            } else {
                LogUtil.log(TAG, "发送媒体event失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复event异常：" + e.toString());
        }
    }



    /**
     * 上报航线任务进度
     */
    public void sendFlightTaskProgress2Server() {
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                FlightTaskProgress.Data.Output.Ext ext = new FlightTaskProgress.Data.Output.Ext();
                FlightTaskProgress.Data.Output.Progress progress = new FlightTaskProgress.Data.Output.Progress();

                FlightTaskProgress.Data.Output output = new FlightTaskProgress.Data.Output();
                FlightTaskProgress.Data data = new FlightTaskProgress.Data();

                ext.setCurrent_waypoint_index(Movement.getInstance().getTask_current_waypoint_index());
                ext.setFlight_id(PreferenceUtils.getInstance().getFlightId());
                ext.setMedia_count(Movement.getInstance().getTask_media_count());
                ext.setTrack_id(Movement.getInstance().getTrack_id());
                ext.setWayline_id(Movement.getInstance().getTask_wayline_id());
                ext.setWayline_mission_state(Movement.getInstance().getTask_wayline_mission_state());

                if (CurrentWayline.getInstance().getWaypoints()!=null
                        &&CurrentWayline.getInstance().getWaypoints().size()>0
                        &&Movement.getInstance().getTask_wayline_mission_state()==6){
                    progress.setPercent((100 * (Movement.getInstance().getCurrentWaypointIndex()+ 1)
                            / CurrentWayline.getInstance().getWaypoints().size()));
                }
                progress.setCurrent_step(Movement.getInstance().getTask_current_step());

                if (Movement.getInstance().isMissionFinish()){
                    WaypointMissionManager.getInstance().queryBreakPointInfoFromAircraft(
                            "aros", new CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo>() {
                        @Override
                        public void onSuccess(BreakPointInfo breakPointInfo) {
                            if (breakPointInfo != null) {
                                LogUtil.log(TAG, "查询断点成功:" + new Gson().toJson(breakPointInfo));
                                FlightTaskProgress.Data.Output.Ext.BreakPoint breakPoint =
                                        new FlightTaskProgress.Data.Output.Ext.BreakPoint();
                                if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getAttitudeHead())){
                                    breakPoint.setAttitude_head(Integer.parseInt(PreferenceUtils.getInstance().getAttitudeHead()));
                                }else{
                                    breakPoint.setAttitude_head(Movement.getInstance().getAttitude_head());
                                }
                                breakPoint.setBreak_reason(Movement.getInstance().getTask_break_reason());
                                breakPoint.setHeight(breakPointInfo.getLocation().getAltitude());
                                if (!TextUtils.isEmpty(PreferenceUtils.getInstance().getWaypointIndex())){
                                    breakPoint.setIndex(Integer.parseInt(PreferenceUtils.getInstance().getWaypointIndex()));
                                }else{
                                    breakPoint.setIndex(0);
                                }
                                breakPoint.setLatitude(breakPointInfo.getLocation().getLatitude());
                                breakPoint.setLongitude(breakPointInfo.getLocation().getLongitude());
                                breakPoint.setProgress(Movement.getInstance().getTask_progress());
                                breakPoint.setState(Movement.getInstance().getState());
                                breakPoint.setWayline_id(Movement.getInstance().getTask_wayline_id());
                                ext.setBreak_point(breakPoint);
                                output.setStatus("partially_done");

                            } else {
                                output.setStatus("ok");
                                LogUtil.log(TAG, "未查询到断点信息");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            output.setStatus("ok");
                        }
                    });
                }else{
                    output.setStatus(Movement.getInstance().getTask_status());
                }
                output.setExt(ext);
                output.setProgress(progress);
                data.setResult(0);
                data.setOutput(output);
                FlightTaskProgress flightTaskProgress = new FlightTaskProgress();
                flightTaskProgress.setTid(UUID.randomUUID().toString());
                flightTaskProgress.setBid(UUID.randomUUID().toString());
                flightTaskProgress.setTimestamp(System.currentTimeMillis());
                flightTaskProgress.setMethod(Constant.FLIGHT_TASK_PROGRESS);
                flightTaskProgress.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(flightTaskProgress).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
                LogUtil.log(TAG,"发送任务进度事件:"+new Gson().toJson(flightTaskProgress));

            } else {
                LogUtil.log(TAG, "发送任务进度event失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "发送任务进度event异常：" + e.toString());
        }
    }

    //计划在航线状态为finish时，查询断点并上报
    public void queryBreakPoint() {
        WaypointMissionManager.getInstance().queryBreakPointInfoFromAircraft("aros",
                new CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo>() {
            @Override
            public void onSuccess(BreakPointInfo breakPointInfo) {
                if (breakPointInfo != null) {
                    LogUtil.log(TAG, "查询断点成功:" + new Gson().toJson(breakPointInfo));
                    Movement.getInstance().setTask_attitude_head(Movement.getInstance().getAttitude_head());
                    Movement.getInstance().setTask_break_reason(2);
                    Movement.getInstance().setTask_index(Movement.getInstance().getTask_current_waypoint_index());
                    Movement.getInstance().setHeight(breakPointInfo.getLocation().getAltitude());
                    Movement.getInstance().setTask_latitude(breakPointInfo.getLocation().getLatitude());
                    Movement.getInstance().setTask_longitude(breakPointInfo.getLocation().getLongitude());
                    Movement.getInstance().setTask_progress(breakPointInfo.getSegmentProgress());
                    Movement.getInstance().setTask_state(0);
                    Movement.getInstance().setTask_wayline_id(breakPointInfo.getWaylineID());
                } else {
                    LogUtil.log(TAG, "未查询到断点信息");
                }
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "查询断点失败:" + getIDJIErrorMsg(idjiError));
            }
        });
    }

    /**
     * 上报sdr
     */
    public void sendWireless2Server() {
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                WirelessLink wirelessLink=new WirelessLink();
                WirelessLink.Data data=new WirelessLink.Data();
                data.setDongle_number(Movement.getInstance().getDongle_number());
                data.setLink_state_4g(Movement.getInstance().getLink_state_4g());
                data.setSdr_link_state(Movement.getInstance().getSdr_link_state());
                data.setLink_workmode(Movement.getInstance().getLink_workmode());
                data.setSdr_quality(Movement.getInstance().getSdr_quality());
                data.setQuality_4g(Movement.getInstance().getQuality_4g());
                data.setUav_quality_4g(Movement.getInstance().getUav_quality_4g());
                data.setGnd_quality_4g(Movement.getInstance().getGnd_quality_4g());
                data.setSdr_freq_band(Movement.getInstance().getSdr_freq_band());
                data.setFreq_band_4g(Movement.getInstance().getFreq_band_4g());

                wirelessLink.setTid(UUID.randomUUID().toString());
                wirelessLink.setBid(UUID.randomUUID().toString());
                wirelessLink.setTimestamp(System.currentTimeMillis());
                wirelessLink.setMethod(Constant.WIRELESS_LINK);
                wirelessLink.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(wirelessLink).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
//                LogUtil.log(TAG,"发送sdr事件:"+new Gson().toJson(wirelessLink));

            } else {
                LogUtil.log(TAG, "发送sdr失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "发送sdr异常：" + e.toString());
        }
    }


    public boolean getGimbalAndCameraEnabled() {
        if (!PreferenceUtils.getInstance().getNeedTriggerApronArucoLand() && !PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand() && Movement.getInstance().getGoHomeState() != 1 && Movement.getInstance().getGoHomeState() != 2) {
            return true;
        } else {
            LogUtil.log(TAG, "降落时不允许操作云台/相机/虚拟摇杆");
            return false;
        }
    }
}
