package com.aros.apron.manager;


import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.Utils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Map;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.payload.WidgetType;
import dji.sdk.keyvalue.value.payload.WidgetValue;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.payload.PayloadCenter;
import dji.v5.manager.aircraft.payload.PayloadIndexType;
import dji.v5.manager.aircraft.payload.data.PayloadBasicInfo;
import dji.v5.manager.aircraft.payload.data.PayloadWidgetInfo;
import dji.v5.manager.aircraft.payload.listener.PayloadBasicInfoListener;
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener;
import dji.v5.manager.aircraft.payload.listener.PayloadWidgetInfoListener;
import dji.v5.manager.interfaces.IPayloadManager;


public class PayloadWidgetManager extends BaseManager {


    private PayloadWidgetManager() {
    }

    private static class PayloadWidgetHolder {
        private static final PayloadWidgetManager INSTANCE = new PayloadWidgetManager();
    }

    public static PayloadWidgetManager getInstance() {
        return PayloadWidgetHolder.INSTANCE;
    }
    public void initPayloadInfo(MqttAndroidClient client) {

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();

            if (payloadManager != null) {
                IPayloadManager iPayloadManager = payloadManager.get(PayloadIndexType.EXTERNAL);
                if (iPayloadManager != null) {
                    iPayloadManager.addPayloadDataListener(new PayloadDataListener() {
                        @Override
                        public void onDataFromPayloadUpdate(byte[] data) {
                            sendMsgFromPSDK2Server(client, data);
//                            Log.e(TAG, "打印DataFromPayload" + "--1111---");
                        }
                    });

                } else {
                    LogUtil.log(TAG, "监听psdk数据失败:设备未连接");
                }
//                IPayloadManager lPayloadManager = payloadManager.get(PayloadIndexType.LEFT_OR_MAIN);
//                if (lPayloadManager!=null){
//                    lPayloadManager.addPayloadWidgetInfoListener(new PayloadWidgetInfoListener() {
//                        @Override
//                        public void onPayloadWidgetInfoUpdate(PayloadWidgetInfo info) {
//                            Log.e(TAG, "打印PayloadWidgetInfo" + new Gson().toJson(info));
//
//                        }
//                    });
//                }
            } else {
                LogUtil.log(TAG, "监听psdk数据失败:未检测到设备");
            }
        } else {
            LogUtil.log(TAG, "设备未连接");
        }
    }
    //锁定
    public void lock(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
            WidgetValue widgetValue = new WidgetValue();
            widgetValue.setValue(0);
            widgetValue.setIndex(0);
            widgetValue.setType(WidgetType.SWITCH);
            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(client, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(error));
                    sendMsg2Server(client, message, "解锁失败:" + getIDJIErrorMsg(error));
                }
            });
        }

    }

    //解锁
    public void unlock(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
            WidgetValue widgetValue = new WidgetValue();
            widgetValue.setValue(1);
            widgetValue.setIndex(0);
            widgetValue.setType(WidgetType.SWITCH);
            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(client, message);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server(client, message, "解锁失败:" + getIDJIErrorMsg(idjiError));

                }
            });
        }

    }

    //抛投
    public void throwOne(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
            WidgetValue widgetValue = new WidgetValue();
            widgetValue.setValue(0);
            widgetValue.setIndex(0);
            widgetValue.setType(WidgetType.SWITCH);
            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
                            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
                            WidgetValue widgetValue = new WidgetValue();
                            widgetValue.setValue(1);
                            widgetValue.setIndex(0);
                            widgetValue.setType(WidgetType.SWITCH);
                            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
                                            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
                                            WidgetValue widgetValue = new WidgetValue();
                                            widgetValue.setValue(1);
                                            widgetValue.setIndex(1);
                                            widgetValue.setType(WidgetType.BUTTON);
                                            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    sendMsg2Server(client, message);
                                                }

                                                @Override
                                                public void onFailure(@NonNull IDJIError idjiError) {
                                                    LogUtil.log(TAG,"抛投失败:" +new Gson().toJson(idjiError));
                                                    sendMsg2Server(client, message, "抛投失败:" + getIDJIErrorMsg(idjiError));

                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                                    sendMsg2Server(client, message, "解锁失败:" + getIDJIErrorMsg(idjiError));

                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"锁定失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server(client, message, "锁定失败:" + getIDJIErrorMsg(idjiError));
                }
            });

        }

    }

    //一件抛投
    public void throwAll(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
            WidgetValue widgetValue = new WidgetValue();
            widgetValue.setValue(0);
            widgetValue.setIndex(0);
            widgetValue.setType(WidgetType.SWITCH);
            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
                            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
                            WidgetValue widgetValue = new WidgetValue();
                            widgetValue.setValue(1);
                            widgetValue.setIndex(0);
                            widgetValue.setType(WidgetType.SWITCH);
                            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
                                            Map<PayloadIndexType, IPayloadManager> payloadManagerMap = payloadManager;
                                            WidgetValue widgetValue = new WidgetValue();
                                            widgetValue.setValue(1);
                                            widgetValue.setIndex(2);
                                            widgetValue.setType(WidgetType.BUTTON);
                                            payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    sendMsg2Server(client, message);
                                                }

                                                @Override
                                                public void onFailure(@NonNull IDJIError idjiError) {
                                                    LogUtil.log(TAG,"全抛失败:" +new Gson().toJson(idjiError));
                                                    sendMsg2Server(client, message, "全抛失败:" + getIDJIErrorMsg(idjiError));
                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                                    sendMsg2Server(client, message, "解锁失败:" + getIDJIErrorMsg(idjiError));
                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"锁定失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server(client, message, "锁定失败:" + getIDJIErrorMsg(idjiError));
                }
            });

        }
    }

    public void sendMsgToPayload(MqttAndroidClient mqttClient, MQMessage message) {
        Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
        if (payloadManager != null) {
            IPayloadManager iPayloadManager = payloadManager.get(PayloadIndexType.EXTERNAL);
            if (iPayloadManager != null) {
                if (TextUtils.isEmpty(message.getPayloadData())) {
                    sendMsg2Server(mqttClient, message, "发送数据到psdk失败:参数有误");
                    return;
                }
                iPayloadManager.sendDataToPayload(Utils.getByte(message.getPayloadData()), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "发送数据到psdk:" + Utils.getByte(message.getPayloadData()));
                        sendMsg2Server(mqttClient, message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG,"发送数据到psdk失败:" +new Gson().toJson(idjiError));
                        sendMsg2Server(mqttClient, message, "发送数据到psdk失败:" + getIDJIErrorMsg(idjiError));
                    }
                });
            } else {
                sendMsg2Server(mqttClient, message, "发送数据到psdk失败:设备未连接");
            }
        } else {
            sendMsg2Server(mqttClient, message, "发送数据到psdk失败:未检测到设备");
        }
    }

    //推送MSDK收到的PSDK数据
    public void sendMsgFromPSDK2Server(MqttAndroidClient client,byte[] data) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60119);
                message.setResult(1);
                message.setPayloadData(data);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance(). getMqttMsdkPushEvent2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "psdkData发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "psdkData发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }
}
