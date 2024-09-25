package com.aros.apron.manager;

import static com.aros.apron.manager.FlightManager.FLAG_STOP_ARUCO;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.greenrobot.eventbus.EventBus;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.RemoteControllerFlightMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;


/**
 * 刷新返航点
 */
public class ResetHomePointManager extends BaseManager {

    MqttAndroidClient mqttClient;

    private ResetHomePointManager() {
    }

    private static class ResetHomePointHolder {
        private static final ResetHomePointManager INSTANCE = new ResetHomePointManager();
    }

    public static ResetHomePointManager getInstance() {
        return ResetHomePointHolder.INSTANCE;
    }


    public void startTaskProcess(MQMessage message) {
        if (TextUtils.isEmpty(message.getOffSitePointLat()) || TextUtils.isEmpty(message.getOffSitePointLon())) {
            sendMissionExecuteEvents(mqttClient, "重置返航点经纬度有误");
            LogUtil.log(TAG, "重置返航点经纬度有误");
            return;
        }
        //飞往异地降落点,关闭视觉识别
        EventBus.getDefault().post(FLAG_STOP_ARUCO);
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            Boolean areMotorOn = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn));
            Boolean isFlying = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyIsFlying));
            if ((areMotorOn != null && areMotorOn) && (isFlying != null && isFlying)) {
                RemoteControllerFlightMode remoteControllerFlightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
                if (remoteControllerFlightMode != null && remoteControllerFlightMode == RemoteControllerFlightMode.P) {
                    checkDroneState(message);
                } else {
                    if (message != null) {
                        sendMsg2Server(mqttClient, message, "挡位不正确,不刷新返航点");
                    }
                    sendMissionExecuteEvents(mqttClient, "挡位不正确,不刷新返航点");
                    LogUtil.log(TAG, "检测到挡位不正确,不刷新返航点");
                }
            } else {
                if (message != null) {
                    sendMsg2Server(mqttClient, message, "飞机未起飞,不刷新返航点");
                }
                sendMissionExecuteEvents(mqttClient, "飞机未起飞,不刷新返航点");
            }
        }
    }

    private void checkDroneState(MQMessage message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
        if (flightMode != null) {
            switch (flightMode) {
                case GO_HOME:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消返航成功");
                            resetHomePoint(message);
                            sendMissionExecuteEvents(mqttClient, "取消返航:刷新返航点");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消返航失败:" + new Gson().toJson(error));
                            resetHomePoint(message);
                            sendMissionExecuteEvents(mqttClient, "取消返航失败:刷新返航点");
                        }
                    });
                    break;
                case AUTO_LANDING:
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消降落成功");
                            sendMissionExecuteEvents(mqttClient, "取消降落成功:去异地降落");
                            resetHomePoint(message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消降落失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents(mqttClient, "取消降落失败:去异地降落");
                            resetHomePoint(message);
                        }
                    });
                    break;
                default:
                    resetHomePoint(message);
                    break;
            }
        }
    }


    public void resetHomePoint(MQMessage message) {

    }

}
