package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.callback.MVirtualStickStateListener;
import com.aros.apron.tools.LogUtil;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;

public class StickManager extends BaseManager {

    private StickManager() {
    }

    private static class StickHolder {
        private static final StickManager INSTANCE = new StickManager();
    }

    public static StickManager getInstance() {
        return StickHolder.INSTANCE;
    }

    public void initStickInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect!=null&&isConnect) {
            VirtualStickManager.getInstance().setVirtualStickStateListener(new MVirtualStickStateListener());
        }
    }

    //取消虚拟摇杆控制权
    public void disableVirtualStick() {
        VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "控制权已取消");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "取消控制权失败:" + error.toString());
            }

        });
    }

//    //设置虚拟摇杆控制权
//    public void setVirtualStickModeEnabled(MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
//            if (flightMode != null) {
//                switch (flightMode) {
//                    case GO_HOME:
//                        LogUtil.log(TAG, "返航时无法手控");
//                        sendMsg2Server( message, "返航时无法手控");
//                        break;
//                    case WAYPOINT:
//                        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
//                        missionManager.stopMission(TextUtils.isEmpty(Movement.getInstance().getMissionName())
//                                ? "aros" : Movement.getInstance().getMissionName(), new CommonCallbacks.CompletionCallback() {
//                            @Override
//                            public void onSuccess() {
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                                            @Override
//                                            public void onSuccess() {
//                                                sendMsg2Server( message);
//                                                LogUtil.log(TAG, "终止任务,控制权设置成功");
//                                                Movement.getInstance().setWaylineCanResume(true);
//                                                Movement.getInstance().setVirtualStickEnableReason(3);
//                                                Movement.getInstance().setVirtualStickQuitMission(true);
//                                            }
//
//                                            @Override
//                                            public void onFailure(@NonNull IDJIError error) {
//                                                LogUtil.log(TAG, "终止任务,控制权设置失败:" + error.description());
//                                                sendMsg2Server( message, "控制权设置失败:" + getIDJIErrorMsg(error));
//                                            }
//                                        });
//                                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
//                                    }
//                                }, 400);
//
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull IDJIError error) {
//                                LogUtil.log(TAG, "终止任务以获取控制权失败:" + new Gson().toJson(error));
//                                sendMsg2Server( message, "终止任务以获取控制权失败:" + getIDJIErrorMsg(error));
//                            }
//                        });
//                        break;
//                    case AUTO_LANDING:
//                        LogUtil.log(TAG, "降落时无法手控");
//                        sendMsg2Server( message, "降落时无法手控");
//                        break;
//                    case VIRTUAL_STICK:
//                        LogUtil.log(TAG, "已获取控制权,无需重复获取");
//                        sendMsg2Server( message, "已获取控制权,无需重复获取");
//                        break;
//                    default:
//                        VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                            @Override
//                            public void onSuccess() {
//                                sendMsg2Server( message);
//                                LogUtil.log(TAG, "控制权设置成功");
//                                Movement.getInstance().setWaylineCanResume(true);
//                                Movement.getInstance().setVirtualStickEnableReason(3);
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull IDJIError error) {
//                                LogUtil.log(TAG, "控制权设置失败:" + error.description());
//                                sendMsg2Server( message, "控制权设置失败:" + getIDJIErrorMsg(error));
//                            }
//                        });
//                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
//                        break;
//                }
//            }
//        }
//
//    }
//
//    //设置虚拟摇杆控制权
//    public void setVirtualStickModeDisable(MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    sendMsg2Server( message);
//                    LogUtil.log(TAG,"控制权取消成功");
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG,"控制权取消失败:"+new Gson().toJson(error));
//                    sendMsg2Server( message, "控制权取消失败:" + getIDJIErrorMsg(error));
//                }
//            });
//
//        } else {
//            sendMsg2Server( message, "飞控未连接");
//        }
//    }
//
//    //参数
//    //模式
//    //数值限制
//    //x
//    //速度模式
//    //[-10 m/s, +10 m/s] 超过最大值，仍按最大值运动
//    //角度模式
//    //[-30°, +30 °]
//    //y
//    //速度模式
//    //[-10 m/s, +10 m/s] 超过最大值，仍按最大值运动
//    //角度模式
//    //[-30°, +30 °]
//    //z
//    //速度模式
//    //[-4 m/s, +4 m/s] 超过最大值，仍按最大值运动
//    //位置模式
//    //[0m, 100m]
//    //yaw
//    //角度模式
//    //[-180°, +180 °]
//    //角速度模式
//    //[-100°/s, +100 °/s]
//    VirtualStickFlightControlParam param;
//
//    //飞行器虚拟摇杆
//    public void sendVirtualStickAdvancedParam(MQMessage message) {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect ) {
//            if (!getGimbalAndCameraEnabled()){
//                return;
//            }
//            if (!Movement.getInstance().isPlaneWing()){
//                LogUtil.log(TAG,"飞机未起飞:禁止手控");
//                return;
//            }
//            if (param == null) {
//                param = new VirtualStickFlightControlParam();
//                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);//
//                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//                param.setVerticalControlMode(VerticalControlMode.VELOCITY);
//                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
//            }
//            param.setPitch(Double.valueOf(message.getY()));//左右(速度模式-10m/s-10m/s)
//            param.setRoll(Double.valueOf(message.getX()));//前后(速度模式-10m/s-10m/s)
//            param.setYaw(Double.valueOf(message.getR()));//旋转(角速度模式-100-100)
//            param.setVerticalThrottle(Double.valueOf(message.getZ()));//上下(速度模式-4m/s-4m/s)
//            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
//            Movement.getInstance().setVirtualStickEnableReason(3);
//
////            sendMsg2Server( message, "移动...");
//        } else {
//            sendMsg2Server( message, "飞控未连接");
//        }
//    }
//
//
//    public void releaseStick(){
//        VirtualStickManager.getInstance().setVirtualStickStateListener(null);
//    }

}
