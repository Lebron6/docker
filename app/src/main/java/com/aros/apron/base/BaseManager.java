package com.aros.apron.base;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.FileUploadResult;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
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
            LogUtil.log(TAG, "回复异常：" + e.toString());
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }


    public static long lastTime;
    public static long lastGisTime;

    public boolean isFlyClickTime() {
        long time = System.currentTimeMillis();
        if (time - lastTime > 1000) {
            lastTime = time;
            return true;
        }
        return false;
    }

    public boolean isGisFlyClickTime() {
        long time = System.currentTimeMillis();
        if (time - lastGisTime > 2000) {
            lastGisTime = time;
            return true;
        }
        return false;
    }

    public void publish(MqttAndroidClient client, String topic, MqttMessage message) {
        try {
            if (client.isConnected()) {
                client.publish(topic, message);
            } else {
//                LogUtil.log(TAG, "推送飞机状态失败:mqtt未连接");
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
                mqttMessage.setQos(1);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, event+"-流程发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "流程发送异常：mqtt 未连接");
            throw new RuntimeException(e);
        }
    }



    //媒体文件上传结果上报
    public void sendFileUploadCallback(MqttAndroidClient client, FileUploadResult result) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                result.setMsg_type(60102);
                mqttMessage = new MqttMessage(new Gson().toJson(result).getBytes("UTF-8"));
                mqttMessage.setQos(1);

                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "文件上传发送成功：60102"+new Gson().toJson(result));

            } else {
                LogUtil.log(TAG, "文件上传发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "文件上传发送异常：mqtt 未连接");
            throw new RuntimeException(e);
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
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance(). getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
            } else {
                LogUtil.log(TAG, "总飞行里程发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "总飞行里程发送异常：mqtt 未连接");
            throw new RuntimeException(e);
        }
    }
}
