package com.aros.apron.manager;


import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.Utils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener;
import dji.v5.manager.aircraft.payload.widget.PayloadWidget;
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
                public void onFailure(@NonNull IDJIError idjiError) {
                    sendMsg2Server(client, message, "解锁失败:" + new Gson().toJson(idjiError));
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
                    sendMsg2Server(client, message, "解锁失败:" + new Gson().toJson(idjiError));

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
                                                    sendMsg2Server(client, message, "抛投失败:" + new Gson().toJson(idjiError));

                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    sendMsg2Server(client, message, "解锁失败:" + new Gson().toJson(idjiError));

                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    sendMsg2Server(client, message, "锁定失败:" + new Gson().toJson(idjiError));

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
                                                    sendMsg2Server(client, message, "全抛失败:" + new Gson().toJson(idjiError));

                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    sendMsg2Server(client, message, "解锁失败:" + new Gson().toJson(idjiError));

                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    sendMsg2Server(client, message, "锁定失败:" + new Gson().toJson(idjiError));

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
                        sendMsg2Server(mqttClient, message, "发送数据到psdk失败:" + new Gson().toJson(idjiError));
                    }
                });
            } else {
                sendMsg2Server(mqttClient, message, "发送数据到psdk失败:设备未连接");
            }
        } else {
            sendMsg2Server(mqttClient, message, "发送数据到psdk失败:未检测到设备");
        }
    }
}
