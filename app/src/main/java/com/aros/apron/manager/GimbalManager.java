package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.ApronArucoDetect;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraType;
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

    public void initGimbalInfo() {
        ApronArucoDetect.getInstance().setDoublePayload(PreferenceUtils.getInstance().getCameraLocationType() == 2);


    }


    //用相对角度模式旋转云台
    public void gimbalRotateByRelativeAngle(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
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
                                sendMsg2Server( message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                LogUtil.log(TAG,"云台控制失败:"+new Gson().toJson(error));
                                sendMsg2Server( message, "云台控制失败:" + getIDJIErrorMsg(error));
                            }
                        }
                );
            }
        } else {
            sendMsg2Server( message, "云台未连接");
        }


    }

//    //用绝对角度模式旋转云台
//    public void gimbalRotateByAbsoluteAngle(MQMessage message) {
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
//            sendMsg2Server( message, "云台未连接");
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
                            LogUtil.log(TAG, "云台复位");
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

    //云台回中
    public void gimbalResetWithPitchAndYaw(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, 0),
                    GimbalResetType.PITCH_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server( message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG,"云台重置失败:"+new Gson().toJson(error));
                            sendMsg2Server( message, "云台控制失败:" + getIDJIErrorMsg(error));
                        }
                    }
            );
        } else {
            sendMsg2Server( message, "云台重置失败:设备未连接");
        }
    }

    //云台偏航回中
    public void gimbalResetWithYaw(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, 0),
                    GimbalResetType.ONLY_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server( message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG,"云台偏航回中失败:"+new Gson().toJson(error));
                            sendMsg2Server( message, "云台偏航回中失败:" + getIDJIErrorMsg(error));
                        }
                    }
            );
        } else {
            sendMsg2Server( message, "云台偏航回中失败:设备未连接");
        }
    }

    //偏航向下
    public void gimbalDownWithPitch(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            GimbalAngleRotation rotation = new GimbalAngleRotation();
                rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
                if (!TextUtils.isEmpty(Movement.getInstance().getGimbalYaw())){
                    rotation.setYaw(Double.parseDouble(Movement.getInstance().getGimbalYaw()));
                }
                rotation.setPitch(-90.0);
                KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, 0), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                            @Override
                            public void onSuccess(EmptyMsg emptyMsg) {
                                sendMsg2Server(message);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError error) {
                                sendMsg2Server(message,"偏航向下失败:"+getIDJIErrorMsg(error));
                            }
                        }
                );

        } else {
            sendMsg2Server( message, "云台偏航回中失败:设备未连接");
        }
    }

    //云台朝下
    public void gimbalDownWithPitchAndYaw(MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect && getGimbalAndCameraEnabled()) {
            GimbalAngleRotation rotation = new GimbalAngleRotation();
            rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
            rotation.setYaw(0.0);
            rotation.setRoll(0.0);
            rotation.setPitch(-90.0);
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, 0), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendMsg2Server(message,"云台向下失败:"+getIDJIErrorMsg(error));
                        }
                    }
            );

        } else {
            sendMsg2Server( message, "云台偏航回中失败:设备未连接");
        }
    }

    //设置云台控制的最大速度[1,100]
    public void setGimbalControlMaxSpeed( MQMessage
            message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect!=null&&isConnect) {
            DJIKey<Integer> pitchKey = KeyTools.createKey(GimbalKey.KeyPitchControlMaxSpeed, 0);
            DJIKey<Integer> yawKey = KeyTools.createKey(GimbalKey.KeyYawControlMaxSpeed, 0);
            if (pitchKey != null) {
                setGimbalControlSpeed(message, pitchKey, "云台俯仰控制速度设置失败:");
            } else {
                LogUtil.log(TAG, "云台俯仰控制速度设置失败:pitchKey is null!");
            }
            if (yawKey != null) {
                setGimbalControlSpeed(message, yawKey, "云台偏航控制速度设置失败:");
            } else {
                LogUtil.log(TAG, "云台偏航控制速度设置失败:yawKey is null!");
            }
        } else {
            sendMsg2Server( message, "云台未连接");
        }
    }


    private void setGimbalControlSpeed( MQMessage
            message, DJIKey<Integer> key, String errorMessage) {
        int value = message.getGimbalControlSpeed();
        KeyManager.getInstance().setValue(key, value, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server( message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG,errorMessage+new Gson().toJson(error));
                sendMsg2Server( message, errorMessage + getIDJIErrorMsg(error));
            }
        });
    }

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
//                        sendMsg2Server( message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server( message, "恢复出厂设置失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server( message, "云台未连接");
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
//                        sendMsg2Server( message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server( message, "启动校准失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server( message, "云台未连接");
//            }
//        }
//
//    }
//
//    //设置云台缓启/停，范围：[0,30]，数值越大，控制云台俯仰轴启动/停止转动的缓冲距离越长。
//    public void setSmoothingFactor(MQMessage message) {
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
//                        sendMsg2Server( message);
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull IDJIError error) {
//                        sendMsg2Server( message, "云台缓启/停设置失败：" + error.description());
//                    }
//                });
//            } else {
//                sendMsg2Server( message, "云台未连接");
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
//                            sendMsg2Server( message);
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull IDJIError error) {
//                            sendMsg2Server( message, "设置云台俯仰扩展失败:" + error.description());
//                        }
//                    });
//                } else {
//                    sendMsg2Server( message, "设置云台俯仰扩展参数有误");
//                }
//            } else {
//                sendMsg2Server( message, "云台未连接");
//            }
//        }
//
//    }
//
    //设置云台模式
    public void setGimbalMode(int gimbalMode) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, 0));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyGimbalMode,
                    0), GimbalMode.find(gimbalMode), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    switch (gimbalMode) {
                        case 0:
                            LogUtil.log(TAG, "设置云台自由模式成功");
                            gimbalReset();
                            break;
                        case 1:
                            LogUtil.log(TAG, "设置云台FPV模式成功");
                            break;
                        case 2:
                            LogUtil.log(TAG, "设置云台跟随模式成功");
                            break;
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    switch (gimbalMode) {
                        case 0:
                            LogUtil.log(TAG, "设置云台自由模式失败:" + error.description());
                            break;
                        case 1:
                            LogUtil.log(TAG, "设置云台FPV模式失败:" + error.description());
                            break;
                        case 2:
                            LogUtil.log(TAG, "设置云台跟随模式失败:" + error.description());
                            break;

                    }
                }
            });

        } else {
            LogUtil.log(TAG, "设置云台模式失败:未连接");
        }

    }

    public void releaseGimbalKey() {
        KeyManager.getInstance().cancelListen(this);
    }
}
