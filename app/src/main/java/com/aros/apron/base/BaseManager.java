package com.aros.apron.base;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.FileUploadResult;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class BaseManager {

    public String TAG = getClass().getSimpleName();


    public void sendMsg2Server(MqttAndroidClient client, MQMessage entity, String msg) {
        try {
            if (client.isConnected()) {
                MessageReply messageReply = new MessageReply();
                messageReply.setMsg_type(entity.getMsg_type());
                messageReply.setResult(-1);
                messageReply.setMsg(msg);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageReply).getBytes("UTF-8"));
                mqttMessage.setQos(1);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, "回复失败：mqtt 未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "回复异常：" + e.toString());
        }
    }


    public void sendMsg2Server(MqttAndroidClient client, MQMessage entity) {
        try {
            if (client.isConnected() && entity != null) {
                MessageReply messageReply = new MessageReply();
                messageReply.setMsg_type(entity.getMsg_type());
                messageReply.setResult(1);
                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(messageReply).getBytes("UTF-8"));
                mqttMessage.setQos(1);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, "回复失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "回复异常：" + e.toString());
            e.printStackTrace();
        }
    }


//    public static long lastTime;
//
//    public boolean isFlyClickTime() {
//        long time = System.currentTimeMillis();
//        if (time - lastTime > 1000||time-lastTime<0) {
//            lastTime = time;
//            return true;
//        }
//        return false;
//    }

    public void publish(MqttAndroidClient client, String topic, MqttMessage message) {
        try {
            if (client.isConnected()) {
                client.publish(topic, message);
//                LogUtil.log(TAG, "推送消息==》"+message);
            } else {
                LogUtil.log(TAG, "推送飞机状态失败:mqtt未连接");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.log(TAG, "推送飞机状态失败异常:" + topic + e.toString());
        }
    }

    //任务流程事件
    public void sendMissionExecuteEvents(MqttAndroidClient client, String event) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60113);
                message.setResult(1);
                message.setMsg(event);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, event+"-流程发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "流程发送异常："+e.toString());
            e.printStackTrace();
        }
    }



    //媒体文件上传结果上报
    public void sendFileUploadCallback(int msgType,MqttAndroidClient client, FileUploadResult result) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                result.setMsg_type(msgType);
                mqttMessage = new MqttMessage(new Gson().toJson(result).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "文件上传发送成功："+new Gson().toJson(result));

            } else {
                LogUtil.log(TAG, "文件上传发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "文件上传发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }


    //推送航点动作组执行状态
    public void sendMsgWaypointActionState2Server(MqttAndroidClient client,String data,String index) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60133);
                message.setResult(1);
                message.setWaypointActionState(data);
                message.setWaypointIndex(index);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance(). getMqttMsdkPushEvent2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "推送航点动作组失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "推送航点动作组发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }

    //获取总飞行里程
    public void sendAircraftTotalFlightDistance2Server(MqttAndroidClient client, MQMessage mqMessage, double data) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60132);
                message.setResult(1);
                message.setFlag(mqMessage.getFlag());
                message.setAircraftTotalFlightDistance(data+"");
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, "总飞行里程发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "总飞行里程发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }

    //收到暂停航线命令后，发送经纬度给后端
    public void sendLowBatteryRTHPosition2Server(MqttAndroidClient client) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60201);
                message.setResult(1);
                message.setLat(Movement.getInstance().getCurrentLatitude());
                message.setLon(Movement.getInstance().getCurrentLongitude());
                message.setTask_id(PreferenceUtils.getInstance().getTaskId());
                message.setFlyingHeight(Movement.getInstance().getFlyingHeight()+"");
                message.setWaypointIndex(Movement.getInstance().getCurrentWaypointIndex()+"");
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG,"低电量返航发送成功");
            } else {
                LogUtil.log(TAG, "触发低电量返航发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "触发低电量返航发送失败：mqtt 未连接");
            e.printStackTrace();
        }
    }

    //自定义到达/离开航点的事件
    public void sendCustomReachOrLeave2Server(MqttAndroidClient client,String data,String index) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60203);
                message.setResult(1);
                message.setWaypointActionState(data);
                message.setWaypointIndex(index);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance(). getMqttMsdkPushEvent2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "推送航点动作组失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "推送航点动作组发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }


    public boolean getGimbalAndCameraEnabled() {
        if (!PreferenceUtils.getInstance().getNeedTriggerApronArucoLand() && !PreferenceUtils.getInstance().getNeedTriggerAlterArucoLand()&& Movement.getInstance().getGoHomeState()!=1&&Movement.getInstance().getGoHomeState()!=2) {
            return true;
        } else {
            LogUtil.log(TAG, "降落时不允许操作云台或相机");
            return false;
        }
    }
}
