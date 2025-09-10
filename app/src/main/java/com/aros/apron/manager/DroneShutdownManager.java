//package com.aros.apron.manager;
//
//import android.os.Handler;
//import android.os.Looper;
//
//import com.aros.apron.base.BaseManager;
//import com.aros.apron.constant.AMSConfig;
//import com.aros.apron.entity.ApronExecutionStatus;
//import com.aros.apron.entity.MessageReply;
//import com.aros.apron.tools.LogUtil;
//import com.google.gson.Gson;
//import org.eclipse.paho.android.service.MqttAndroidClient;
//import org.eclipse.paho.client.mqttv3.IMqttActionListener;
//import org.eclipse.paho.client.mqttv3.IMqttToken;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import java.nio.charset.StandardCharsets;
//
//public class DroneShutdownManager extends BaseManager {
//
//    private final int maxRetries = 20;
//    private int sendDroneShutDownSuccessTimes;
//    private boolean isSendDroneShutDownSuccess;
//
//    private DroneShutdownManager() {
//    }
//
//    private static class DroneShutHolder {
//        private static final DroneShutdownManager INSTANCE = new DroneShutdownManager();
//    }
//
//    public static DroneShutdownManager getInstance() {
//        return DroneShutHolder.INSTANCE;
//    }
//
//
//    public void sendDroneShutDownMsg2Server(MqttAndroidClient client) {
////        if (isSendDroneShutDownSuccess||sendDroneShutDownSuccessTimes >= maxRetries) {
//        if (sendDroneShutDownSuccessTimes >= maxRetries) {
//            LogUtil.log(TAG, "达到最大重试次数或已发送关机"+isSendDroneShutDownSuccess+sendDroneShutDownSuccessTimes);
//            return;
//        }
//        try {
//            if (client.isConnected()) {
//                sendShutDownMessage(client);
//            } else {
//                handleNotConnected(client);
//            }
//        } catch (Exception e) {
//            LogUtil.log(TAG, "关机发送异常：" + e.toString());
//            e.printStackTrace();
//        }
//    }
//
//    private void sendShutDownMessage(MqttAndroidClient client) {
//
//
//        try {
//            if (client.isConnected()){
//                MessageReply message = new MessageReply();
//                message.setMsg_type(60011);
//                message.setResult(1);
//                message.setMsg("关机");
//                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
//                mqttMessage.setQos(0);
//                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage, null, new IMqttActionListener() {
//                    @Override
//                    public void onSuccess(IMqttToken asyncActionToken) {
//                        LogUtil.log(TAG, "关机发送成功：60011---"+sendDroneShutDownSuccessTimes+"clientId:"+client.getClientId());
//                        sendMissionExecuteEvents( "AMS通知机库执行无人机关机");
//                        mainHandler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (ApronExecutionStatus.getInstance().isServerReplyDroneShut()) {
//                                    isSendDroneShutDownSuccess = true;
//                                    LogUtil.log(TAG, "已收到服务端响应飞机关机");
//                                } else {
//                                    LogUtil.log(TAG, "未收到服务端响应飞机关机,重新发送");
//                                    retrySend(client);
//                                }
//                            }
//                        }, 2000);
//                    }
//
//                    @Override
//                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                        LogUtil.log(TAG, "关机发送回调失败：" + exception.toString());
//                        retrySend(client);
//                    }
//                });
//            } else {
//                LogUtil.log(TAG, "关机发送失败：mqtt 未连接");
//            }
//
//        } catch (Exception e) {
//            LogUtil.log(TAG, "关机发送异常：" + e.toString());
//            e.printStackTrace();
//        }
//
//
//    }
//    final Handler mainHandler = new Handler(Looper.getMainLooper());
//
//    private void retrySend(MqttAndroidClient client) {
//        sendDroneShutDownSuccessTimes++;
//        if (sendDroneShutDownSuccessTimes < maxRetries) {
//            mainHandler.postDelayed(() -> sendDroneShutDownMsg2Server(client), 2000);
//        } else {
//            LogUtil.log(TAG, "达到最大重试次数，关机发送失败：" + sendDroneShutDownSuccessTimes);
//        }
//    }
//
//    private void handleNotConnected(MqttAndroidClient client) {
//        if (!isSendDroneShutDownSuccess && sendDroneShutDownSuccessTimes < maxRetries) {
//            sendDroneShutDownSuccessTimes++;
//            mainHandler.postDelayed(() -> sendDroneShutDownMsg2Server(client), 2000);
//            LogUtil.log(TAG, "关机发送失败：mqtt未连接" + "--" + sendDroneShutDownSuccessTimes);
//        } else {
//            LogUtil.log(TAG, "关机发送失败：" + sendDroneShutDownSuccessTimes);
//        }
//    }
//
//}