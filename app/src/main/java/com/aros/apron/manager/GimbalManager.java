package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.ApronArucoDetect;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
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

    //设置云台模式
    public void setGimbalMode(int gimbalMode) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.
                KeyConnection, ComponentIndexType.PORT_1));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().setValue(KeyTools.createKey(GimbalKey.KeyGimbalMode,
                    ComponentIndexType.PORT_1), GimbalMode.find(gimbalMode), new CommonCallbacks.CompletionCallback() {
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
