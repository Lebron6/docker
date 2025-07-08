package com.aros.apron.manager;


import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MessageReply;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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

    private boolean sendLowBatteryRTHPosition2Server;

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
                if (lowBatteryRTHInfo != null) {
                    Movement.getInstance().setLowBatteryRTHState(lowBatteryRTHInfo.getLowBatteryRTHStatus().value());
                }
            }

            KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.
                    KeyLowBatteryRTHInfo), this, new CommonCallbacks.KeyListener<LowBatteryRTHInfo>() {
                @Override
                public void onValueChange(@Nullable LowBatteryRTHInfo lowBatteryRTHInfo, @Nullable LowBatteryRTHInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setRemainFlightTime(t1.getRemainingFlightTime());
                        Movement.getInstance().setReturnHomePower(t1.getBatteryPercentNeededToGoHome());
                        Movement.getInstance().setLandingPower(t1.getBatteryPercentNeededToLand());
                        if (t1.getLowBatteryRTHStatus() != null) {
                            Movement.getInstance().setLowBatteryRTHState(t1.getLowBatteryRTHStatus().value());
                            if (t1.getLowBatteryRTHStatus().value()==1&&!sendLowBatteryRTHPosition2Server){
                                sendLowBatteryRTHPosition2Server = true;
                                sendLowBatteryRTHPosition2Server(client);
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

    //收到暂停航线命令后，发送经纬度给后端
    public void sendLowBatteryRTHPosition2Server(MqttAndroidClient client) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = null;
                MessageReply message = new MessageReply();
                message.setMsg_type(60201);
                message.setResult(1);
                message.setLat(Movement.getInstance().getCurrentLatitude());
                message.setLon(Movement.getInstance().getCurrentLongitude());
                message.setTask_id(PreferenceUtils.getInstance().getTaskId());
                message.setFlyingHeight(Movement.getInstance().getFlyingHeight());
                message.setWaypointIndex(Movement.getInstance().getCurrentWaypointIndex()+"");
                mqttMessage = new MqttMessage(new Gson().toJson(message).getBytes("UTF-8"));
                mqttMessage.setQos(0);
                client.publish(AMSConfig.getInstance().getMqttMsdkReplyMessage2ServerTopic(), mqttMessage);

            } else {
                LogUtil.log(TAG, "触发低电量返航发送失败：mqtt 未连接");
            }
        } catch (Exception e) {
            LogUtil.log(TAG, "触发低电量返航发送失败：mqtt 未连接");
            e.printStackTrace();
        }
    }

    public void releaseBatteryKey() {
        KeyManager.getInstance().cancelListen(this);
    }
}
