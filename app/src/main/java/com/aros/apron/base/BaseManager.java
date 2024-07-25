package com.aros.apron.base;

import android.os.Handler;
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
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "流程发送:"+event);
            } else {
                LogUtil.log(TAG, event+"-流程发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "流程发送异常：mqtt 未连接");
            throw new RuntimeException(e);
        }
    }


    //执行入库
    public void sendDroneStorageMsg2Server(MqttAndroidClient client, int result) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60010);
                message.setResult(result);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);

                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "入库发送成功：60010");
                sendMissionExecuteEvents(client,"AMS通知机库执行入库");
            } else {
                LogUtil.log(TAG, "入库失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "入库异常：mqtt 未连接");
            throw new RuntimeException(e);
        }
    }
    public boolean isSendDroneShutDownSuccess;
    public int sendDroneShutDownSuccessTimes;
    //执行无人机关机
    public void sendDroneShutDownMsg2Server(MqttAndroidClient client) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60011);
                message.setResult(1);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "关机发送成功：60011");
                sendMissionExecuteEvents(client,"AMS通知机库执行无人机关机");

                isSendDroneShutDownSuccess = true;
                sendDroneShutDownSuccessTimes = 0;
            } else {
                if (!isSendDroneShutDownSuccess && sendDroneShutDownSuccessTimes < 15) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendDroneShutDownSuccessTimes++;
                            sendDroneShutDownMsg2Server(client);
                        }
                    }, 3000);
                    LogUtil.log(TAG, "关机发送失败：mqtt未连接"+"--"+sendDroneShutDownSuccessTimes);
                }else{
                    LogUtil.log(TAG, "关机开舱失败："+"--"+sendDroneShutDownSuccessTimes);
                }
                LogUtil.log(TAG, "关机失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关机异常：mqtt 未连接");
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
                mqttMessage.setQos(2);

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

    //飞机飞走,关闭舱门
    public void sendCloseCabinDoorMsg2Server(MqttAndroidClient client) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60107);
                message.setResult(1);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "关舱发送成功：60107");
                sendMissionExecuteEvents(client,"AMS通知机库执行关闭舱门");

            } else {
                LogUtil.log(TAG, "关闭舱门发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "关闭舱门发送异常：mqtt 未连接");
            throw new RuntimeException(e);
        }
    }

    public boolean isSendOpenCabinDoorSuccess;
    public int sendOpenCabinDoorSuccessTimes;

    //飞机飞回,打开舱门
    public void sendOpenCabinDoorMsg2Server(MqttAndroidClient client) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60108);
                message.setResult(1);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);
                LogUtil.log(TAG, "开舱发送成功：60108"+"--"+sendOpenCabinDoorSuccessTimes);
                sendMissionExecuteEvents(client,"AMS通知机库执行打开舱门");

                isSendOpenCabinDoorSuccess = true;
                sendOpenCabinDoorSuccessTimes = 0;
            } else {
                if (!isSendOpenCabinDoorSuccess && sendOpenCabinDoorSuccessTimes < 10) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendOpenCabinDoorSuccessTimes++;
                            sendOpenCabinDoorMsg2Server(client);
                        }
                    }, 3000);
                    LogUtil.log(TAG, "开舱发送失败：mqtt未连接"+"--"+sendOpenCabinDoorSuccessTimes);
                }else{
                    LogUtil.log(TAG, "开舱失败："+"--"+sendOpenCabinDoorSuccessTimes);

                    //这里可以飞往备降点？
                }

            }
        } catch (Exception e) {
            LogUtil.log(TAG, "开舱发送异常：mqtt 异常"+e.toString());
            throw new RuntimeException(e);
        }
    }
}
