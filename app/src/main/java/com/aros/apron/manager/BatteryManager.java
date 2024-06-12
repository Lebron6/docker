package com.aros.apron.manager;


import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.LowBatteryRTHInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;

/**
 * 电池
 */
public class BatteryManager extends BaseManager {
    MqttAndroidClient client;

    private BatteryManager() {
    }

    private static class BatteryManagerHolder {
        private static final BatteryManager INSTANCE = new BatteryManager();
    }

    public static BatteryManager getInstance() {
        return BatteryManagerHolder.INSTANCE;
    }

    public void initBatteryInfo(MqttAndroidClient client) {
        this.client = client;
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(BatteryKey.KeyConnection, 0));
        if (isConnect != null && isConnect) {

            LowBatteryRTHInfo lowBatteryRTHInfo = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                    KeyLowBatteryRTHInfo));
            if (lowBatteryRTHInfo != null) {
                Movement.getInstance().setRemainFlightTime(lowBatteryRTHInfo.getRemainingFlightTime());
                Movement.getInstance().setReturnHomePower(lowBatteryRTHInfo.getBatteryPercentNeededToGoHome());
                Movement.getInstance().setLandingPower(lowBatteryRTHInfo.getBatteryPercentNeededToLand());
            }

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.
                    KeyLowBatteryRTHInfo), this, new CommonCallbacks.KeyListener<LowBatteryRTHInfo>() {
                @Override
                public void onValueChange(@Nullable LowBatteryRTHInfo lowBatteryRTHInfo, @Nullable LowBatteryRTHInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemainFlightTime(t1.getRemainingFlightTime());
                        Movement.getInstance().setReturnHomePower(t1.getBatteryPercentNeededToGoHome());
                        Movement.getInstance().setLandingPower(t1.getBatteryPercentNeededToLand());
                    }
                }
            });
/**************************************************************************************************************/
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyChargeRemainingInPercent, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setElectricityInfoA(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyChargeRemainingInPercent, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setElectricityInfoA(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyVoltage, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setVoltageInfoA(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 0), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBatteryTemperatureA(t1);
                    }
                }
            });

        }

        /********************************************************************************************************************/

        Boolean isConnectBatteryB = KeyManager.getInstance().getValue(KeyTools.createKey(BatteryKey.KeyConnection, 1));
        if (isConnectBatteryB != null && isConnectBatteryB) {

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyChargeRemainingInPercent, 1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setElectricityInfoB(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyVoltage, 1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setVoltageInfoB(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 1), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBatteryTemperatureB(t1);
                    }
                }
            });

        }
    }

    public void releaseBatteryKey() {
        KeyManager.getInstance().cancelListen(this);
    }
}
