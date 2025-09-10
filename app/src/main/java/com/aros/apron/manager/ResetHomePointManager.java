package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.Utils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
import dji.sdk.keyvalue.value.flightcontroller.RemoteControllerFlightMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;


/**
 * 刷新返航点
 */
public class ResetHomePointManager extends BaseManager {


    private ResetHomePointManager() {
    }

    private static class ResetHomePointHolder {
        private static final ResetHomePointManager INSTANCE = new ResetHomePointManager();
    }

    public static ResetHomePointManager getInstance() {
        return ResetHomePointHolder.INSTANCE;
    }


    public void startTaskProcess(MqttAndroidClient client, MQMessage message) {
        if (TextUtils.isEmpty(message.getOffSitePointLat()) || TextUtils.isEmpty(message.getOffSitePointLon())) {
            sendMissionExecuteEvents( "重置返航点经纬度有误");
            LogUtil.log(TAG, "重置返航点经纬度有误");
            return;
        }

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            Boolean areMotorOn = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn));
            Boolean isFlying = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyIsFlying));
            if ((areMotorOn != null && areMotorOn) && (isFlying != null && isFlying)) {
                RemoteControllerFlightMode remoteControllerFlightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyRemoteControllerFlightMode));
                if (remoteControllerFlightMode != null && remoteControllerFlightMode == RemoteControllerFlightMode.P) {
                    checkDroneState(client, message);
                } else {
                    if (message != null) {
                        sendMsg2Server( message, "挡位不正确,不刷新返航点");
                    }
                    sendMissionExecuteEvents( "挡位不正确,不刷新返航点");
                    LogUtil.log(TAG, "检测到挡位不正确,不刷新返航点");
                }
            } else {
                if (message != null) {
                    sendMsg2Server( message, "飞机未起飞,不刷新返航点");
                }
                sendMissionExecuteEvents( "飞机未起飞,不刷新返航点");
            }
        }
    }
private int droneStatus;
    private void checkDroneState(MqttAndroidClient client, MQMessage message) {
        FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
        if (flightMode != null) {
            switch (flightMode) {
                case GO_HOME:
                    droneStatus = 1;
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消返航,刷新返航点成功");
                            resetHomePoint(client, message);
                            sendMissionExecuteEvents( "取消返航:刷新返航点");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消返航失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents( "取消返航失败:" + Utils.getIDJIErrorMsg(error));
                        }
                    });
                    break;
                case AUTO_LANDING:
                    droneStatus = 2;
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            LogUtil.log(TAG, "取消降落,刷新返航点成功");
                            sendMissionExecuteEvents( "取消降落成功:刷新返航点");
                            resetHomePoint(client, message);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "取消降落失败:" + new Gson().toJson(error));
                            sendMissionExecuteEvents( "取消降落失败:" +Utils.getIDJIErrorMsg(error));
                        }
                    });
                    break;
                default:
                    droneStatus = 0;
                    resetHomePoint(client, message);
                    break;
            }
        }
    }


    public void resetHomePoint(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LocationCoordinate2D homeLocation = new LocationCoordinate2D();
                    homeLocation.setLatitude(Utils.parseLatLon(message.getOffSitePointLat()));
                    homeLocation.setLongitude(Utils.parseLatLon(message.getOffSitePointLon()));
                    KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), homeLocation, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            sendMissionExecuteEvents( "刷新返航点成功");
                            LogUtil.log(TAG, "刷新返航点成功");
                            if (droneStatus==1||droneStatus==2){
                                startGoHome(client,message);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "刷新返航点失败:" + new Gson().toJson(error));
                            sendMsg2Server( message, "刷新返航点失败:"  + getIDJIErrorMsg(error));
                        }
                    });
                }
            }, 500);

        } else {
            LogUtil.log(TAG, "刷新返航点失败:飞控未连接");
            sendMsg2Server( message, "刷新返航点失败:飞控未连接");
        }
    }

    //返航
    public void startGoHome(MqttAndroidClient mqttAndroidClient, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                        @Override
                        public void onSuccess(EmptyMsg emptyMsg) {
                            sendMsg2Server( message);
                            LogUtil.log(TAG, "执行继续返航");
                            sendMissionExecuteEvents( "刷新返航点成功");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendMsg2Server( message, "继续返航执行失败:" + getIDJIErrorMsg(error));
                            LogUtil.log(TAG, "继续返航执行失败：" + new Gson().toJson(error));
                        }
                    });
                }
            },500);

        } else {
            sendMsg2Server( message, "继续返航执行失败：飞控未连接");
            LogUtil.log(TAG, "返航执行失败：飞控未连接");

        }
    }

}
