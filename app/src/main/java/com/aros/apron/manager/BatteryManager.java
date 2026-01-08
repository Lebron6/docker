package com.aros.apron.manager;


import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;

import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.battery.IndustryBatteryType;
import dji.sdk.keyvalue.value.flightcontroller.LowBatteryRTHInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;

/**
 * 电池
 */
public class BatteryManager extends BaseManager {

    private BatteryManager() {
    }

    private static class BatteryManagerHolder {
        private static final BatteryManager INSTANCE = new BatteryManager();
    }

    public static BatteryManager getInstance() {
        return BatteryManagerHolder.INSTANCE;
    }

    private boolean sendLowBatteryRTHPosition2Server;

    private static final long SECONDS_PER_DAY = 24 * 60 * 60; // 86400


    public void initBatteryInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(BatteryKey.KeyConnection, 0));
        if (isConnect != null && isConnect) {

            LowBatteryRTHInfo lowBatteryRTHInfo = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                    KeyLowBatteryRTHInfo));
            if (lowBatteryRTHInfo != null) {
                Movement.getInstance().setLanding_power(lowBatteryRTHInfo.getBatteryPercentNeededToLand());
                Movement.getInstance().setRemain_flight_time(lowBatteryRTHInfo.getRemainingFlightTime());
                Movement.getInstance().setReturn_home_power(lowBatteryRTHInfo.getBatteryPercentNeededToGoHome());
                if (lowBatteryRTHInfo != null) {
                    Movement.getInstance().setLowBatteryRTHState(lowBatteryRTHInfo.getLowBatteryRTHStatus().value());
                }
            }

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.
                    KeyLowBatteryRTHInfo), this, new CommonCallbacks.KeyListener<LowBatteryRTHInfo>() {
                @Override
                public void onValueChange(@Nullable LowBatteryRTHInfo lowBatteryRTHInfo, @Nullable LowBatteryRTHInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLanding_power(t1.getBatteryPercentNeededToLand());
                        Movement.getInstance().setRemain_flight_time(t1.getRemainingFlightTime());
                        Movement.getInstance().setReturn_home_power(t1.getBatteryPercentNeededToGoHome());
                        if (t1.getLowBatteryRTHStatus() != null) {
                            Movement.getInstance().setLowBatteryRTHState(t1.getLowBatteryRTHStatus().value());
                            if (t1.getLowBatteryRTHStatus().value() == 1 && !sendLowBatteryRTHPosition2Server) {
                                sendLowBatteryRTHPosition2Server = true;
                                sendLowBatteryRTHPosition2Server();
                            }
                        }
                    }
                }
            });
/**************************************************************************************************************/


            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyChargeRemainingInPercent, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setBattery_a_capacity_percent(newValue);
                        Movement.getInstance().setCapacity_percent(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyChargeRemainingInPercent, 1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setBattery_b_capacity_percent(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyVoltage, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setBattery_a_voltage(newValue);
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyVoltage, 1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    if (newValue != null) {
                        Movement.getInstance().setBattery_b_voltage(newValue);
                    }
                }
            });

            //电池A固件版本
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyFirmwareVersion, 0), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_battery_firmware_version(t1);
                    }
                }
            });
            //电池B固件版本
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyFirmwareVersion, 1), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_battery_firmware_version(t1);

                    }
                }
            });

            //电池A高电压存储天数
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryHighVoltageStorageTime, 0), this, new CommonCallbacks.KeyListener<Long>() {
                @Override
                public void onValueChange(@Nullable Long aLong, @Nullable Long t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_high_voltage_storage_days((int) (t1 / SECONDS_PER_DAY));
                    }
                }
            });

            //电池B高电压存储天数
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryHighVoltageStorageTime, 1), this, new CommonCallbacks.KeyListener<Long>() {
                @Override
                public void onValueChange(@Nullable Long aLong, @Nullable Long t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_high_voltage_storage_days((int) (t1 / SECONDS_PER_DAY));
                    }
                }
            });

            //电池A循环次数
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyNumberOfDischarges, 0), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_loop_times(t1);
                    }
                }
            });

            //电池B循环次数
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyNumberOfDischarges, 1), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_loop_times(t1);
                    }
                }
            });

            //电池A SN
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeySerialNumber, 0), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_battery_sn(t1);
                    }
                }
            });

            //电池B SN
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeySerialNumber, 1), this, new CommonCallbacks.KeyListener<String>() {
                @Override
                public void onValueChange(@Nullable String s, @Nullable String t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_battery_sn(t1);
                    }
                }
            });

            //电池A温度
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 0), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_temperature(t1);
                    }
                }
            });

            //电池B温度
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 1), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_temperature(t1);
                    }
                }
            });

            //电池A类型
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyIndustryBatteryType, 0), this, new CommonCallbacks.KeyListener<IndustryBatteryType>() {
                @Override
                public void onValueChange(@Nullable IndustryBatteryType industryBatteryType, @Nullable IndustryBatteryType t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_type(t1.value());
                    }
                }
            });

            //电池B类型
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyIndustryBatteryType, 1), this, new CommonCallbacks.KeyListener<IndustryBatteryType>() {
                @Override
                public void onValueChange(@Nullable IndustryBatteryType industryBatteryType, @Nullable IndustryBatteryType t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_type(t1.value());
                    }
                }
            });

            //电池A温度
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 0), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_a_temperature(t1);
                    }
                }
            });

            //电池B温度
            KeyManager.getInstance().listen(KeyTools.createKey(BatteryKey.
                    KeyBatteryTemperature, 1), this, new CommonCallbacks.KeyListener<Double>() {
                @Override
                public void onValueChange(@Nullable Double aDouble, @Nullable Double t1) {
                    if (t1 != null) {
                        Movement.getInstance().setBattery_b_temperature(t1);
                    }
                }
            });

        }
    }



    public void releaseBatteryKey() {
        KeyManager.getInstance().cancelListen(this);
    }
}
