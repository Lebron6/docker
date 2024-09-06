package com.aros.apron.manager;

import android.os.Handler;
import androidx.annotation.NonNull;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;


public class WayLineExecutingInterruptManager extends BaseManager {

    private MqttAndroidClient client;

    private WayLineExecutingInterruptManager() {
    }

    private static class WayLineExecutingInterruptHolder {
        private static final WayLineExecutingInterruptManager INSTANCE = new WayLineExecutingInterruptManager();
    }

    public static WayLineExecutingInterruptManager getInstance() {
        return WayLineExecutingInterruptHolder.INSTANCE;
    }
    public void initWayLineExecutingInterruptInfo(MqttAndroidClient mqttAndroidClient) {
        this.client = mqttAndroidClient;}

    public void onExecutingInterruptToDo() {

        if (Movement.getInstance().getFlyingHeight() < 90) {
            LogUtil.log(TAG, "航线中断,拉高" + Movement.getInstance().getFlyingHeight());
            raiseTheReturnFlight();
            sendMissionExecuteEvents(client,"航线中断:拉高后返航");
        } else {
            LogUtil.log(TAG, "航线中断,返航" + Movement.getInstance().getFlyingHeight() );
            FlightManager.getInstance().startGoHome(null, null);
            sendMissionExecuteEvents(client,"航线中断:直接返航");

        }

    }

    //航线因多种原因触发悬停后，拉高返航
    public void raiseTheReturnFlight() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "失控拉高,控制权获取成功");
                    VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
                    pullUp();
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "失控拉高,控制权获取失败:" + error.description());
                    sendMissionExecuteEvents(client,"航线中断:执行拉高失败");

                }
            });

        } else {
            LogUtil.log(TAG, "失控拉高,飞控未连接");
            sendMissionExecuteEvents(client,"航线中断:飞控未连接");

        }

    }

    Handler handler = new Handler();

    public void pullUp() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (Movement.getInstance().getFlyingHeight() < 100) {

                    if (Movement.getInstance().getGoHomeState()==1||Movement.getInstance().getGoHomeState()==2){
                        handler.removeCallbacks(this);
                    }else{
                        sendVirtualStickAdvancedParam();
                        handler.postDelayed(this, 200);
                    }
                } else {
                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "到达100米,取消虚拟摇杆控制并返航");
                            sendMissionExecuteEvents(client,"航线中断:到达指定高度,开始返航");
                            FlightManager.getInstance().startGoHome(null, null);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            sendMissionExecuteEvents(client,"航线中断:释放控制权失败,开始返航");
                            LogUtil.log(TAG, "到达80米,取消虚拟摇杆控制返航失败:" + new Gson().toJson(idjiError));
                            FlightManager.getInstance().startGoHome(null, null);
                        }
                    });
                    handler.removeCallbacks(this);
                }
            }
        };
        // 开始循环
        handler.post(runnable);

    }

    VirtualStickFlightControlParam param;

    //飞行器虚拟摇杆
    public void sendVirtualStickAdvancedParam() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (param == null) {
                param = new VirtualStickFlightControlParam();
                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                param.setVerticalControlMode(VerticalControlMode.VELOCITY);
                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            }
            param.setPitch(0.0);//左右
            param.setRoll(0.0);//前后
            param.setYaw(0.0);//旋转
            param.setVerticalThrottle(4.0);//上下
            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
        }
    }

}
