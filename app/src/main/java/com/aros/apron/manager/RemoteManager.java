package com.aros.apron.manager;


import static dji.sdk.keyvalue.key.KeyTools.createKey;

import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.remotecontroller.BatteryInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;

/**
 * 遥控器
 */
public class RemoteManager extends BaseManager {

    private RemoteManager() {
    }

    private static class RemoteManagerHolder {
        private static final RemoteManager INSTANCE = new RemoteManager();
    }

    public static RemoteManager getInstance() {
        return RemoteManagerHolder.INSTANCE;
    }


    public void initRemoteInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(
                KeyTools.createKey(RemoteControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            //遥控器序列号
            KeyManager.getInstance().listen(createKey(RemoteControllerKey.KeySerialNumber), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemoteSerialNumber(t1);
                    }
                }
            });
            //遥控器电池电量信息
            KeyManager.getInstance().listen(createKey(RemoteControllerKey.KeyBatteryInfo), this, new CommonCallbacks.KeyListener<BatteryInfo>() {
                @Override
                public void onValueChange(@Nullable BatteryInfo batteryInfo, @Nullable BatteryInfo t1) {
                    if (t1!=null){
                        Movement.getInstance().setRemoteBatteryPercent(String.valueOf(t1.getBatteryPercent()));
                    }
                }
            });
            //外置电池电量信息
            KeyManager.getInstance().listen(createKey(RemoteControllerKey.KeySecondBatteryInfo), this, new CommonCallbacks.KeyListener<BatteryInfo>() {
                @Override
                public void onValueChange(@Nullable BatteryInfo batteryInfo, @Nullable BatteryInfo t1) {
                    if (t1!=null){
                        Movement.getInstance().setRemoteSecondBatteryPercent(String.valueOf(t1.getBatteryPercent()));
                    }
                }
            });
        } else {
            LogUtil.log(TAG,"遥控器未连接");
        }
    }
}
