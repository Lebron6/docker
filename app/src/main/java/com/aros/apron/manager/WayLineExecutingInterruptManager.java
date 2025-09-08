package com.aros.apron.manager;

import static com.aros.apron.manager.FlightManager.FLAG_STOP_ARUCO;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.greenrobot.eventbus.EventBus;

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
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager;
import dji.v5.manager.interfaces.IWaypointMissionManager;


public class WayLineExecutingInterruptManager extends BaseManager {


    private WayLineExecutingInterruptManager() {
    }

    private static class WayLineExecutingInterruptHolder {
        private static final WayLineExecutingInterruptManager INSTANCE = new WayLineExecutingInterruptManager();
    }

    public static WayLineExecutingInterruptManager getInstance() {
        return WayLineExecutingInterruptHolder.INSTANCE;
    }

    public void initWayLineExecutingInterruptInfo() {
    }

    public void onExecutingInterruptToDo() {
        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
        missionManager.stopMission(TextUtils.isEmpty(Movement.getInstance().getMissionName())
                ? "aros" : Movement.getInstance().getMissionName(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "终止任务成功");
                resetAircrftLandingStatus();

            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "终止任务失败:" + new Gson().toJson(error));
            }
        });

        if (Movement.getInstance().getFlyingHeight() < 90) {
            LogUtil.log(TAG, "航线中断,拉高" + Movement.getInstance().getFlyingHeight());
            raiseTheReturnFlight();
            sendMissionExecuteEvents( "航线中断:拉高后返航");
        } else {
            LogUtil.log(TAG, "航线中断,返航" + Movement.getInstance().getFlyingHeight());
            FlightManager.getInstance().startGoHome(null);
            sendMissionExecuteEvents( "航线中断:直接返航");

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
                    Movement.getInstance().setVirtualStickEnableReason(1);
                    VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
                    pullUp();
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "失控拉高,控制权获取失败:" + error.description());
                    sendMissionExecuteEvents( "航线中断:执行拉高失败");

                }
            });

        } else {
            LogUtil.log(TAG, "失控拉高,飞控未连接");
            sendMissionExecuteEvents( "航线中断:飞控未连接");

        }

    }

    Handler handler = new Handler();

    public void pullUp() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (Movement.getInstance().getFlyingHeight() < 100) {
                    Movement.getInstance().setVirtualStickEnableReason(1);
                    if (Movement.getInstance().getGoHomeState() == 1 || Movement.getInstance().getGoHomeState() == 2) {
                        handler.removeCallbacks(this);
                    } else {
                        sendVirtualStickAdvancedParam();
                        handler.postDelayed(this, 200);
                    }
                } else {
                    VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "到达100米,取消虚拟摇杆控制并返航");
                            sendMissionExecuteEvents( "航线中断:到达指定高度,开始返航");
                            FlightManager.getInstance().startGoHome(null);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            sendMissionExecuteEvents( "航线中断:释放控制权失败,开始返航");
                            LogUtil.log(TAG, "到达80米,取消虚拟摇杆控制返航失败:" + new Gson().toJson(idjiError));
                            FlightManager.getInstance().startGoHome( null);
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
            Movement.getInstance().setVirtualStickEnableReason(1);
        }
    }


    private void resetAircrftLandingStatus() {
        // 避免在下次起飞时触发视觉识别
        PreferenceUtils.getInstance().setNeedTriggerApronArucoLand(false);
        PreferenceUtils.getInstance().setNeedTriggerAlterArucoLand(false);
        PreferenceUtils.getInstance().setTriggerToAlternatePoint(false);
        //设置为未触发开始识别二维码状态
        FlightManager.getInstance().setSendDetect(false);
        EventBus.getDefault().post(FLAG_STOP_ARUCO);
    }
}
