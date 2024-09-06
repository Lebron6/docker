package com.aros.apron.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.ApronArucoDetect;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation;
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode;
import dji.sdk.keyvalue.value.gimbal.GimbalMode;
import dji.sdk.keyvalue.value.gimbal.GimbalResetType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;

public class GimbalManager extends BaseManager {


    private GimbalManager() {
    }

    private static class GimbalHolder {
        private static final GimbalManager INSTANCE = new GimbalManager();
    }

    public static GimbalManager getInstance() {
        return GimbalHolder.INSTANCE;
    }

    public void initGimbalInfo(){
        KeyManager.getInstance().listen(KeyTools.createKey(GimbalKey.
                KeyConnection, ComponentIndexType.RIGHT), this, new CommonCallbacks.KeyListener<Boolean>() {
            @Override
            public void onValueChange(@Nullable Boolean aBoolean, @Nullable Boolean t1) {
                if (t1!=null){
                    //双挂
                    ApronArucoDetect.getInstance().setDoublePayload(t1);
                    LogUtil.log(TAG,"检测是否双挂:"+t1);
                }
            }
        });

    }


    //用相对角度模式旋转云台
    public void gimbalRotateByRelativeAngle(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect) {
            if (message.getX() == 0 && message.getY() == 0) {
                gimbalReset();
            } else {
                int yaw = message.getX();
                int pitch = message.getY();
                GimbalAngleRotation rotation = new GimbalAngleRotation();
                rotation.setMode(GimbalAngleRotationMode.RELATIVE_ANGLE);
                rotation.setYaw(Double.valueOf(yaw));
                rotation.setPitch(Double.valueOf(pitch));
                KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, 0), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                            @Override
                            public void onSuccess(EmptyMsg emptyMsg) {
                                sendMsg2Server(mqttAndroidClient, message);
                                LogUtil.log(TAG, "云台控制成功:" + yaw + "---" + pitch);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                sendMsg2Server(mqttAndroidClient, message, "云台控制失败:" + new Gson().toJson(error));
                                LogUtil.log(TAG, "云台控制失败:" + new Gson().toJson(error));
                            }
                        }
                );
            }
        } else {
            sendMsg2Server(mqttAndroidClient, message, "云台未连接");
        }


    }

//    //用绝对角度模式旋转云台
//    public void gimbalRotateByAbsoluteAngle(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                KeyConnection, 0));
//        if (isConnect != null && isConnect) {
//            if (message.getX() == 0 && message.getY() == 0) {
//                gimbalReset();
//            } else {
//                int yaw = message.getX();
//                int pitch = message.getY();
//                GimbalAngleRotation rotation = new GimbalAngleRotation();
//                rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
//                rotation.setYaw(Double.valueOf(yaw * 10));
//                rotation.setPitch(Double.valueOf(pitch * 10));
//                KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, 0), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                            @Override
//                            public void onSuccess(EmptyMsg emptyMsg) {
//                                LogUtil.log(TAG, "云台控制成功:" + yaw + "---" + pitch);
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull IDJIError error) {
//                                LogUtil.log(TAG, "云台控制失败:" + error.description());
//                            }
//                        }
//                );
//            }
//        } else {
//            sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//        }
//    }

    //云台重置
    public void gimbalReset() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, 0), GimbalResetType.PITCH_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "云台复位成功");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "云台复位失败:" + error.description());
                        }
                    }
            );
        } else {
            LogUtil.log(TAG, "云台未连接");
        }
    }

    //云台重置
    public void gimbalReset(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, 0), GimbalResetType.PITCH_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server(client, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendMsg2Server(client, message, "云台重置失败:" + new Gson().toJson(error));
                        }
                    }
            );
        } else {
            sendMsg2Server(client, message, "云台重置失败:设备未连接");
        }
    }

    //
//    //设置云台控制的最大速度[1,100]
//    public void setGimbalControlMaxSpeed(MqttAndroidClient mqttAndroidClient, MQMessage
//            message) {
//        MQMessage.Data data = message.getData();
//        if (data != null) {
//            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                    KeyConnection, Integer.parseInt(data.getComponentIndex())));
//            if (isConnect) {
//                String value = data.getGimbalControlMaxSpeed();
//                String type = data.getGimbalControlMaxSpeedType();
//                KeyManager.getInstance().setValue(KeyTools.createKey(type.equals("0") ? GimbalKey.KeyPitchControlMaxSpeed : GimbalKey.KeyYawControlMaxSpeed, Integer.parseInt(data.getComponentIndex())), Integer.parseInt(value), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "云台偏航速度设置失败:" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//            }
//        }
//    }
//
//    //恢复出厂设置
//    public void setRestoreFactorySettings(MqttAndroidClient mqttAndroidClient, MQMessage
//            message) {
//        MQMessage.Data data = message.getData();
//        if (data != null) {
//            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                    KeyConnection, Integer.parseInt(data.getComponentIndex())));
//            if (isConnect) {
//                KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRestoreFactorySettings, Integer.parseInt(data.getComponentIndex())), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                    @Override
//                    public void onSuccess(EmptyMsg emptyMsg) {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "恢复出厂设置失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//            }
//        }
//
//    }
//
//    //启动自动校准
//    public void startGimbalCalibrate(MqttAndroidClient mqttAndroidClient, MQMessage
//            message) {
//        MQMessage.Data data = message.getData();
//        if (data != null) {
//            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                    KeyConnection, Integer.parseInt(data.getComponentIndex())));
//            if (isConnect) {
//                KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalCalibrate, Integer.parseInt(data.getComponentIndex())), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                    @Override
//                    public void onSuccess(EmptyMsg emptyMsg) {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "启动校准失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//            }
//        }
//
//    }
//
//    //设置云台缓启/停，范围：[0,30]，数值越大，控制云台俯仰轴启动/停止转动的缓冲距离越长。
//    public void setSmoothingFactor(MqttAndroidClient mqttAndroidClient, MQMessage message) {
//        MQMessage.Data data = message.getData();
//        if (data != null) {
//            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                    KeyConnection, Integer.parseInt(data.getComponentIndex())));
//            if (isConnect) {
//                String value = data.getGimbalSmoothingFactor();
//                String type = data.getGimbalSmoothingFactorType();
//                KeyManager.getInstance().setValue(KeyTools.createKey(type.equals("0") ? GimbalKey.KeyPitchSmoothingFactor : GimbalKey.KeyYawSmoothingFactor, Integer.parseInt(data.getComponentIndex())), Integer.parseInt(value), new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onSuccess() {
//                        sendMsg2Server(mqttAndroidClient, message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server(mqttAndroidClient, message, "云台缓启/停设置失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//            }
//        }
//
//
//    }
//
//    //设置云台限位扩展
//    public void setPitchRangeExtensionEnabled(MqttAndroidClient
//                                                      mqttAndroidClient, MQMessage message) {
//        MQMessage.Data data = message.getData();
//        if (data != null) {
//            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
//                    KeyConnection, Integer.parseInt(data.getComponentIndex())));
//            if (isConnect) {
//                String type = data.getPitchRangeExtensionEnabled();
//                if (!TextUtils.isEmpty(type)) {
//                    KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyPitchRangeExtensionEnabled,
//                            Integer.parseInt(data.getComponentIndex())), type.equals("1") ? true : false, new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onSuccess() {
//                            sendMsg2Server(mqttAndroidClient, message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            sendMsg2Server(mqttAndroidClient, message, "设置云台俯仰扩展失败:" + error.description());
//                        }
//                    });
//                } else {
//                    sendMsg2Server(mqttAndroidClient, message, "设置云台俯仰扩展参数有误");
//                }
//            } else {
//                sendMsg2Server(mqttAndroidClient, message, "云台未连接");
//            }
//        }
//
//    }
//
    //设置云台模式
    public void setGimbalMode(int gimbalMode) {
            Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                    KeyConnection, 0));
            if (isConnect!=null&&isConnect) {
                    KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyGimbalMode,
                            0), GimbalMode.find(gimbalMode), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            switch (gimbalMode){
                                case 0:
                                    LogUtil.log(TAG,"设置云台自由模式成功");
                                    gimbalReset();
                                    break;
                                case 1:
                                    LogUtil.log(TAG,"设置云台FPV模式成功");
                                    break;
                                case 2:
                                    LogUtil.log(TAG,"设置云台跟随模式成功");
                                    break;
                            }
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            switch (gimbalMode){
                                    case 0:
                                        LogUtil.log(TAG,"设置云台自由模式失败:"+error.description());
                                        break;
                                    case 1:
                                        LogUtil.log(TAG,"设置云台FPV模式失败:"+error.description());
                                        break;
                                    case 2:
                                        LogUtil.log(TAG,"设置云台跟随模式失败:"+error.description());
                                        break;

                            }
                        }
                    });

            } else {
                LogUtil.log(TAG,"设置云台模式失败:未连接");
            }

    }

    public void releaseGimbalKey() {
        KeyManager.getInstance().cancelListen(this);
    }
}
