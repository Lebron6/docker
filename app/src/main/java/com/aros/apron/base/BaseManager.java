package com.aros.apron.base;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.constant.Constant;
import com.aros.apron.entity.CurrentWayline;
import com.aros.apron.entity.FlightTaskProgress;
import com.aros.apron.entity.MediaUpLoad;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.MessageEvent;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.entity.Movement;
import com.aros.apron.entity.WirelessLink;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

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
     * 发送taskFailevent
     * @param msg
     */
    public void sendTaskFailEvent2Server(String msg) {
        LogUtil.log(TAG,"发送任务失败事件:"+"  -"+msg);
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod(Constant.TASK_FAIL);
                MessageEvent.Data data=new MessageEvent.Data();
                data.setResult(-1);
                data.setErrorMsg(msg);
                messageEvent.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
            } else {
                LogUtil.log(TAG, "发送event失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复event异常：" + e.toString());
        }
    }

    /**
     * 发送常规event事件
     */
    public void sendEvent2Server(String msg) {
        LogUtil.log(TAG,"发送常规事件:"+"simple  -"+msg);
        try {
            if (MqttManager.getInstance().mqttAndroidClient.isConnected()) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setBid(UUID.randomUUID().toString());
                messageEvent.setTid(UUID.randomUUID().toString());
                messageEvent.setTimestamp(System.currentTimeMillis());
                messageEvent.setMethod("simple");
                MessageEvent.Data data=new MessageEvent.Data();
                data.setResult(1);
                data.setErrorMsg(msg);
                messageEvent.setData(data);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageEvent).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                MqttManager.getInstance().mqttAndroidClient.publish(AMSConfig.UP_UAV_EVENT, mqttMessage);
            } else {
                LogUtil.log(TAG, "发送event失败：mqtt 未连接");
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
                FlightTaskProgress.Data.Output.Ext.BreakPoint breakPoint = new FlightTaskProgress.Data.Output.Ext.BreakPoint();
                FlightTaskProgress.Data.Output.Ext ext = new FlightTaskProgress.Data.Output.Ext();
                FlightTaskProgress.Data.Output.Progress progress = new FlightTaskProgress.Data.Output.Progress();

                FlightTaskProgress.Data.Output output = new FlightTaskProgress.Data.Output();
                FlightTaskProgress.Data data = new FlightTaskProgress.Data();
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

                ext.setBreak_point(breakPoint);
                output.setExt(ext);
                output.setProgress(progress);
                if (Movement.getInstance().isMissionFinish()){
                    output.setStatus("ok");
                }else{
                    output.setStatus(Movement.getInstance().getTask_status());
                }
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

    /**
     * 上报航线任务进度
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
                LogUtil.log(TAG,"发送sdr事件:"+new Gson().toJson(wirelessLink));

            } else {
                LogUtil.log(TAG, "发送sdr失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "发送sdr异常：" + e.toString());
        }
    }


    public boolean getGimbalAndCameraEnabled() {
        if (!PreferenceUtils.getInstance().getNeedTriggerApronArucoLand() && !PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()&& Movement.getInstance().getGoHomeState()!=1&&Movement.getInstance().getGoHomeState()!=2) {
            return true;
        } else {
            LogUtil.log(TAG, "降落时不允许操作云台/相机/虚拟摇杆");
            return false;
        }
    }
}
