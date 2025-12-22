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
import com.aros.apron.entity.Movement;
import com.aros.apron.entity.PayloadInfo;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    private List<PayloadInfo> payloadInfos = new ArrayList<>();

    private PayloadWidgetManager() {
    }

    private static class PayloadWidgetHolder {
        private static final PayloadWidgetManager INSTANCE = new PayloadWidgetManager();
    }

    public static PayloadWidgetManager getInstance() {
        return PayloadWidgetHolder.INSTANCE;
    }
    private  String byte2HexString(byte[] bytes) {
        String hex = "";
        if (bytes != null) {
            for (Byte b : bytes) {
                hex += String.format("%02X", b.intValue() & 0xFF);
            }
        }
        return hex;
    }

    // 使用GsonBuilder配置Gson实例以允许序列化特殊浮点数值
    Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues() // 这是关键
            .create();

    public void initPayloadInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
            if (payloadManager != null) {
                IPayloadManager iPayloadManager = payloadManager.get(PayloadIndexType.EXTERNAL);
                    iPayloadManager.addPayloadDataListener(new PayloadDataListener() {
                        @Override
                        public void onDataFromPayloadUpdate(byte[] data) {
                            sendMsgFromPSDK2Server(MqttManager.getInstance().mqttAndroidClient, data);
                        }
                    });
                    iPayloadManager.addPayloadBasicInfoListener(new PayloadBasicInfoListener() {
                        @Override
                        public void onPayloadBasicInfoUpdate(PayloadBasicInfo info) {
                            if (info != null && info.isConnected()) {
                                removePayloadByIndexType(payloadInfos,PayloadIndexType.EXTERNAL.name());
                                PayloadInfo payloadInfo = new PayloadInfo();
                                payloadInfo.setPayloadIndexType(PayloadIndexType.EXTERNAL.name());
                                payloadInfo.setFirmwareVersion(info.getFirmwareVersion());
                                payloadInfo.setProductName(info.getPayloadProductName());
                                payloadInfo.setSerialNumber(info.getSerialNumber());
                                payloadInfos.add(payloadInfo);
                                Movement.getInstance().setPayloadInfos(payloadInfos);
                            }
                        }
                    });

                /*******************************************************************************************************/

                IPayloadManager leftOrMainPayloadManager = payloadManager.get(PayloadIndexType.LEFT_OR_MAIN);
                    leftOrMainPayloadManager.addPayloadBasicInfoListener(new PayloadBasicInfoListener() {
                        @Override
                        public void onPayloadBasicInfoUpdate(PayloadBasicInfo info) {
                            LogUtil.log(TAG,"左负载:"+new Gson().toJson(info));
                            if (info != null ) {
                                removePayloadByIndexType(payloadInfos,PayloadIndexType.LEFT_OR_MAIN.name());

                                PayloadInfo payloadInfo = new PayloadInfo();
                                payloadInfo.setPayloadIndexType(PayloadIndexType.LEFT_OR_MAIN.name());
                                payloadInfo.setFirmwareVersion(info.getFirmwareVersion());
                                payloadInfo.setProductName(info.getPayloadProductName());
                                payloadInfo.setSerialNumber(info.getSerialNumber());
                                payloadInfos.add(payloadInfo);
                                Movement.getInstance().setPayloadInfos(payloadInfos);
                            }
                        }
                    });

                /*******************************************************************************************************/

                IPayloadManager rightPayloadManager = payloadManager.get(PayloadIndexType.RIGHT);
                    rightPayloadManager.addPayloadBasicInfoListener(new PayloadBasicInfoListener() {
                        @Override
                        public void onPayloadBasicInfoUpdate(PayloadBasicInfo info) {
                            if (info != null && info.isConnected()) {
                                removePayloadByIndexType(payloadInfos,PayloadIndexType.RIGHT.name());

                                PayloadInfo payloadInfo = new PayloadInfo();
                                payloadInfo.setPayloadIndexType(PayloadIndexType.RIGHT.name());
                                payloadInfo.setFirmwareVersion(info.getFirmwareVersion());
                                if (info.getPayloadProductName() != null) {
                                    if (info.getPayloadProductName().equals("HP3200")) {
                                        payloadInfo.setProductName("SVR3");
                                    } else {
                                        payloadInfo.setProductName(info.getPayloadProductName());
                                    }
                                }
                                payloadInfo.setSerialNumber(info.getSerialNumber());
                                payloadInfos.add(payloadInfo);
                                Movement.getInstance().setPayloadInfos(payloadInfos);
                            }
                        }
                    });



                /*******************************************************************************************************/

                IPayloadManager upPayloadManager = payloadManager.get(PayloadIndexType.UP);
                    upPayloadManager.addPayloadBasicInfoListener(new PayloadBasicInfoListener() {
                        @Override
                        public void onPayloadBasicInfoUpdate(PayloadBasicInfo info) {
                            if (info != null && info.isConnected()) {
                                removePayloadByIndexType(payloadInfos,PayloadIndexType.UP.name());
                                PayloadInfo payloadInfo = new PayloadInfo();
                                payloadInfo.setPayloadIndexType(PayloadIndexType.UP.name());
                                payloadInfo.setFirmwareVersion(info.getFirmwareVersion());
                                payloadInfo.setProductName(info.getPayloadProductName());
                                payloadInfo.setSerialNumber(info.getSerialNumber());
                                payloadInfos.add(payloadInfo);
                                Movement.getInstance().setPayloadInfos(payloadInfos);
                            }
                        }
                    });

                //可以把负载设备控件打印
                PayloadCenter.getInstance().getPayloadManager().get(PayloadIndexType.RIGHT).addPayloadWidgetInfoListener(new PayloadWidgetInfoListener() {
                    @Override
                    public void onPayloadWidgetInfoUpdate(PayloadWidgetInfo info) {
                        Log.e(TAG,"右侧负载控件信息:"+new Gson().toJson(info));
                        if (info!=null&&info.getFloatingWindowWidget()!=null&&info.getFloatingWindowWidget().getHintMessage()!=null){
                            sendCH4MsgFromPSDK2Server(MqttManager.getInstance().mqttAndroidClient,info.getFloatingWindowWidget().getHintMessage());
                        }
                    }
                });

            } else {
                LogUtil.log(TAG, "监听psdk数据失败:未检测到设备");
            }
        } else {
            LogUtil.log(TAG, "设备未连接");
        }
    }


    //锁定/关闭激光
    public void lock(MQMessage message) {
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
                    sendMsg2Server( message);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(error));
                    sendMsg2Server( message, "解锁失败:" + getIDJIErrorMsg(error));
                }
            });
        }

    }

    //解锁/打开激光
    public void unlock(MQMessage message) {
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
                    sendMsg2Server( message);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server( message, "解锁失败:" + getIDJIErrorMsg(idjiError));
                }
            });
        }
    }

    //抛投
    public void throwOne(MQMessage message) {
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
                                                    sendMsg2Server( message);
                                                }

                                                @Override
                                                public void onFailure(@NonNull IDJIError idjiError) {
                                                    LogUtil.log(TAG,"抛投失败:" +new Gson().toJson(idjiError));
                                                    sendMsg2Server( message, "抛投失败:" + getIDJIErrorMsg(idjiError));

                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                                    sendMsg2Server( message, "解锁失败:" + getIDJIErrorMsg(idjiError));

                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"锁定失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server( message, "锁定失败:" + getIDJIErrorMsg(idjiError));
                }
            });

        }

    }

    //一件抛投
    public void throwAll(MQMessage message) {
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
                                                    sendMsg2Server( message);
                                                }

                                                @Override
                                                public void onFailure(@NonNull IDJIError idjiError) {
                                                    LogUtil.log(TAG,"全抛失败:" +new Gson().toJson(idjiError));
                                                    sendMsg2Server( message, "全抛失败:" + getIDJIErrorMsg(idjiError));
                                                }
                                            });
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    LogUtil.log(TAG,"解锁失败:" +new Gson().toJson(idjiError));
                                    sendMsg2Server( message, "解锁失败:" + getIDJIErrorMsg(idjiError));
                                }
                            });
                        }
                    }, 500);
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG,"锁定失败:" +new Gson().toJson(idjiError));
                    sendMsg2Server( message, "锁定失败:" + getIDJIErrorMsg(idjiError));
                }
            });

        }
    }

    public void sendMsgToPayload(MQMessage message) {
        Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
        if (payloadManager != null) {
            IPayloadManager iPayloadManager = payloadManager.get(PayloadIndexType.EXTERNAL);
            if (iPayloadManager != null) {
                if (TextUtils.isEmpty(message.getPayloadData())) {
                    sendMsg2Server( message, "发送数据到psdk失败:参数有误");
                    return;
                }
                iPayloadManager.sendDataToPayload(Utils.getByte(message.getPayloadData()), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "发送数据到psdk:" + Utils.getByte(message.getPayloadData()));
                        sendMsg2Server( message);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG,"发送数据到psdk失败:" +new Gson().toJson(idjiError));
                        sendMsg2Server( message, "发送数据到psdk失败:" + getIDJIErrorMsg(idjiError));
                    }
                });
            } else {
                sendMsg2Server( message, "发送数据到psdk失败:设备未连接");
            }
        } else {
            sendMsg2Server( message, "发送数据到psdk失败:未检测到设备");
        }
    }

    public void sendMsgToLeftPayload(String txt) {
        Map<PayloadIndexType, IPayloadManager> payloadManager = PayloadCenter.getInstance().getPayloadManager();
        if (payloadManager != null) {
            IPayloadManager iPayloadManager = payloadManager.get(PayloadIndexType.LEFT_OR_MAIN);
            if (iPayloadManager != null) {

                iPayloadManager.sendDataToPayload(txt.getBytes(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG,"send"+txt+"to psdk success");
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG,"send"+txt+"to psdk fail:"+new Gson().toJson(idjiError));
                    }
                });
            } else {
                LogUtil.log(TAG,"发送数据到psdk失败:设备未连接");
            }
        } else {
            LogUtil.log(TAG,"发送数据到psdk失败:未检测到设备");
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
                client.publish(AMSConfig.getInstance().getMqttMsdkPushEvent2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "psdkData发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "psdkData发送异常：mqtt 未连接");
            e.printStackTrace();
        }
    }

    //推送MSDK收到的PSDK数据
    public void sendCH4MsgFromPSDK2Server(MqttAndroidClient client,String data) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60153);
                message.setResult(1);
                message.setCh4(data);
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(2);
                client.publish(AMSConfig.getInstance().getMqttMsdkPushEvent2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "psdkData发送ch4失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "psdkData发送ch4异常：mqtt 未连接");
            e.printStackTrace();
        }
    }


    /**
     * 如果列表中存在指定 payloadIndexType 的 PayloadInfo 对象，则将其移除。
     *
     * @param list 要操作的 PayloadInfo 列表
     * @param payloadIndexType 要移除的 payloadIndexType
     * @return 是否成功移除（true 表示已移除，false 表示未找到）
     */
    private void removePayloadByIndexType(List<PayloadInfo> list, String payloadIndexType) {
        for (Iterator<PayloadInfo> iterator = list.iterator(); iterator.hasNext(); ) {
            PayloadInfo payloadInfo = iterator.next();
            if (payloadInfo.getPayloadIndexType().equals(payloadIndexType)) {
                iterator.remove();
            }
        }
    }
}
