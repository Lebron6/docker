package com.jby.apron.manager;

import static com.jby.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jby.apron.base.BaseManager;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.ApronArucoDetect;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.flightcontroller.LookAtInfo;
import dji.sdk.keyvalue.value.flightcontroller.LookAtMode;
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
        LogUtil.log(TAG,"主摄像头位置:"+PreferenceUtils.getInstance().getCameraLocationType());
        Boolean gimBalIsConnect = KeyManager.getInstance().getValue(createKey(GimbalKey.KeyConnection, ComponentIndexType.PORT_1));
        if (gimBalIsConnect != null && gimBalIsConnect) {
            KeyManager.getInstance().listen(createKey(GimbalKey.KeyGimbalAttitude,  ComponentIndexType.PORT_1), this, new CommonCallbacks.KeyListener<Attitude>() {
                @Override
                public void onValueChange(@Nullable Attitude oldValue, @Nullable Attitude newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setGimbal_pitch(newValue.getPitch().intValue());
                        Movement.getInstance().setGimbal_roll(newValue.getRoll().intValue());
                        Movement.getInstance().setGimbal_yaw(newValue.getYaw().intValue());
                    }
                }
            });
        }
    }

    //负载控制权抢夺（MSDK没有此方法，直接返回true）
    public void payloadAuthorityGrab(MessageDown message) {
        sendMsg2Server(message);
    }


    //云台重置
    public void gimbalReset() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, ComponentIndexType.PORT_1), GimbalResetType.PITCH_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
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

    //云台重置
    public void gimbalReset(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {
            switch (message.getData().getReset_mode()){
                case 0:
                    KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, ComponentIndexType.PORT_1), GimbalResetType.PITCH_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                                @Override
                                public void onSuccess(EmptyMsg emptyMsg) {
                                    sendMsg2Server(message);
                                }
                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    sendFailMsg2Server(message,"云台复位失败:" + error.description());
                                }
                            }
                    );
                    break;
                case 1:
                    GimbalAngleRotation rotation = new GimbalAngleRotation();
                    rotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
                    rotation.setYaw(0.0);
                    rotation.setRoll(0.0);
                    rotation.setPitch(-90.0);
                    KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, ComponentIndexType.PORT_1), rotation, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                                @Override
                                public void onSuccess(EmptyMsg emptyMsg) {
                                    sendMsg2Server(message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    sendFailMsg2Server(message,"云台向下失败:" + error.description());
                                }
                            }
                    );
                    break;
                case 2:
                    KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyGimbalReset, ComponentIndexType.PORT_1),
                            GimbalResetType.ONLY_YAW, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                                @Override
                                public void onSuccess(EmptyMsg emptyMsg) {
                                    sendMsg2Server(message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    sendFailMsg2Server(message,"云台偏航回中失败:" + getIDJIErrorMsg(error));
                                }
                            }
                    );
                    break;
                case 3:
                    GimbalAngleRotation rotation1 = new GimbalAngleRotation();
                    rotation1.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE);
                    if (!TextUtils.isEmpty(Movement.getInstance().getGimbal_yaw()+"")){
                        rotation1.setYaw(Movement.getInstance().getGimbal_yaw());
                    }
                    rotation1.setPitch(-90.0);
                    KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle, ComponentIndexType.PORT_1), rotation1, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                                @Override
                                public void onSuccess(EmptyMsg emptyMsg) {
                                    sendMsg2Server(message);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError error) {
                                    sendFailMsg2Server(message,"偏航向下失败:"+getIDJIErrorMsg(error));
                                }
                            }
                    );
                    break;
            }

        } else {
            LogUtil.log(TAG, "云台未连接");
        }
    }

    //Look At
    public void gimbalLookAt(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {
            LookAtInfo lookAtInfo = new LookAtInfo();
            lookAtInfo.setMode(message.getData().isLocked() ?
                    LookAtMode.LOOK_AT_GIMBAL_FOLLOWING : LookAtMode.LOOK_AT_GIMBAL_FREE);
            LocationCoordinate3D locationCoordinate3D = new LocationCoordinate3D();
            locationCoordinate3D.setLatitude(message.getData().getLatitude());
            locationCoordinate3D.setLongitude(message.getData().getLongitude());
            locationCoordinate3D.setAltitude(Double.parseDouble(message.getData().getHeight() + ""));
            lookAtInfo.setLocation(locationCoordinate3D);
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyLookAt), lookAtInfo, new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendFailMsg2Server(message, "看向目标点失败:" + getIDJIErrorMsg(error));
                        }
                    }
            );
        } else {
            LogUtil.log(TAG, "云台未连接");
        }
    }
}
