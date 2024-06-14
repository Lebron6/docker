package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;
import androidx.annotation.NonNull;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickState;
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener;

public class StickManager extends BaseManager {

    MqttAndroidClient client;


    private StickManager() {
    }

    private static class StickHolder {
        private static final StickManager INSTANCE = new StickManager();
    }

    public static StickManager getInstance() {
        return StickHolder.INSTANCE;
    }

    public void initStickInfo(MqttAndroidClient client) {
        this.client = client;
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect!=null&&isConnect) {

            Boolean isVirtualStickControlModeEnabled = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyVirtualStickControlModeEnabled));
            if (isVirtualStickControlModeEnabled!=null){
                Movement.getInstance().setIsVirtualStickEnable(isVirtualStickControlModeEnabled?1:0);
            }
            VirtualStickManager.getInstance().setVirtualStickStateListener(new VirtualStickStateListener() {
                @Override
                public void onVirtualStickStateUpdate(@NonNull VirtualStickState stickState) {
                    if (stickState!=null){
                        LogUtil.log(TAG,"控制权获取状态:"+stickState.isVirtualStickEnable());
                        Movement.getInstance().setIsVirtualStickEnable(stickState.isVirtualStickEnable()?1:0);
                    }
                }

                @Override
                public void onChangeReasonUpdate(@NonNull FlightControlAuthorityChangeReason reason) {

                }
            });
        }
    }

    //设置虚拟摇杆控制权
    public void setVirtualStickModeEnabled(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                    LogUtil.log(TAG,"控制权设置成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"控制权设置失败:"+error.description());
                    sendMsg2Server(mqttAndroidClient, message, "控制权设置失败:" + new Gson().toJson(error));
                }
            });
            VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);

        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    //设置虚拟摇杆控制权
    public void setVirtualStickModeDisable(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(mqttAndroidClient, message);
                    LogUtil.log(TAG,"控制权取消成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG,"控制权取消失败:"+new Gson().toJson(error));
                    sendMsg2Server(mqttAndroidClient, message, "控制权取消失败:" + new Gson().toJson(error));
                }
            });

        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

    VirtualStickFlightControlParam param;

    //飞行器虚拟摇杆
    public void sendVirtualStickAdvancedParam(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (param == null) {
                param = new VirtualStickFlightControlParam();
                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                param.setVerticalControlMode(VerticalControlMode.VELOCITY);
                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            }
            param.setPitch(Double.valueOf(message.getY()));//左右
            param.setRoll(Double.valueOf(message.getX()));//前后
            param.setYaw(Double.valueOf(message.getR())*10);//旋转
            param.setVerticalThrottle(Double.valueOf(message.getZ()));//上下
            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
//            sendMsg2Server(mqttAndroidClient, message, "移动...");
        } else {
            sendMsg2Server(mqttAndroidClient, message, "飞控未连接");
        }
    }

//    private void publishStickState2Server() {
//        if (isFlyClickTime()) {
//            //推送飞行状态
//            MqttMessage flightMessage = null;
//            try {
//                StickStateEntity.getInstance().setTimeStamp(String.valueOf(System.currentTimeMillis()));
//                flightMessage = new MqttMessage(new Gson().toJson(StickStateEntity.getInstance()).getBytes("UTF-8"));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            flightMessage.setQos(1);
//            publish(client, MqttConfig.MQTT_STICK_TOPIC, flightMessage);
//        }
//    }

    public void releaseStick(){
        VirtualStickManager.getInstance().setVirtualStickStateListener(null);
    }

}
