package com.jby.apron.manager;

import static com.jby.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jby.apron.base.BaseManager;
import com.jby.apron.callback.MVirtualStickStateListener;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.Utils;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
import dji.sdk.keyvalue.value.flightcontroller.FlightMode;
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
        if (isConnect != null && isConnect) {
            VirtualStickManager.getInstance().setVirtualStickStateListener(new MVirtualStickStateListener());
        }
    }

    //取消虚拟摇杆控制权
    public void enableVirtualStick(MessageDown message) {
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
        VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (message != null) {
                    sendMsg2Server(message);
                }
                LogUtil.log(TAG, "控制权获取成功");
                VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);

            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                if (message != null) {
                    sendFailMsg2Server(message, "控制权获取失败:" + Utils.getIDJIErrorMsg(error));
                }
            }
        });
    }

    //取消虚拟摇杆控制权
    public void disableVirtualStick(MessageDown message) {
        VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (message != null) {
                    sendMsg2Server(message);
                }
                LogUtil.log(TAG, "控制权已取消");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                if (message!=null){
                    sendFailMsg2Server(message,"控制权释放失败:"+ Utils.getIDJIErrorMsg(error));
                }
                LogUtil.log(TAG, "控制权释放失败:"+Utils.getIDJIErrorMsg(error));
            }
        });
    }

    //飞行控制权抢夺
    public void setVirtualStickModeEnabled(MessageDown message) {
        sendMsg2Server(message);
//////        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//////        if (isConnect != null && isConnect) {
//////            FlightMode flightMode = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyFlightMode));
//////            LogUtil.log(TAG, "控制权抢夺当前飞机状态:" + flightMode.name());
//////            if (flightMode != null) {
//////                switch (flightMode) {
//////                    case GO_HOME:
//////                        KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopGoHome), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//////                            @Override
//////                            public void onSuccess(EmptyMsg emptyMsg) {
//////                                sendMsg2Server(message);
//////                            }
//////                            @Override
//////                            public void onFailure(@NonNull IDJIError error) {
//////                                sendFailMsg2Server(message, "取消返航执行失败:" + getIDJIErrorMsg(error));
//////                            }
//////                        });
//////                        break;
//////                    case WAYPOINT:
//////                        IWaypointMissionManager missionManager = WaypointMissionManager.getInstance();
//////                        missionManager.stopMission(TextUtils.isEmpty(Movement.getInstance().getMissionName())
//////                                ? "aros" : Movement.getInstance().getMissionName(), new CommonCallbacks.CompletionCallback() {
//////                            @Override
//////                            public void onSuccess() {
//////                                new Handler().postDelayed(new Runnable() {
//////                                    @Override
//////                                    public void run() {
//////                                        VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
//////                                            @Override
//////                                            public void onSuccess() {
//////                                                sendMsg2Server( message);
//////                                                LogUtil.log(TAG, "终止任务,控制权设置成功");
//////                                                Movement.getInstance().setWaylineCanResume(true);
//////                                                Movement.getInstance().setVirtualStickEnableReason(3);
//////                                                Movement.getInstance().setVirtualStickQuitMission(true);
//////                                            }
//////                                            @Override
//////                                            public void onFailure(@NonNull IDJIError error) {
//////                                                sendFailMsg2Server( message, "控制权设置失败:" + getIDJIErrorMsg(error));
//////                                            }
//////                                        });
//////                                        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
//////                                    }
//////                                }, 400);
//////                            }
//////
//////                            @Override
//////                            public void onFailure(@NonNull IDJIError error) {
//////                                sendFailMsg2Server(message, "终止任务以获取控制权失败:" + getIDJIErrorMsg(error));
//////                            }
//////                        });
//////                        break;
//////                    case AUTO_LANDING:
//////                        KeyManager.getInstance().performAction(createKey(FlightControllerKey.KeyStopAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//////                            @Override
//////                            public void onSuccess(EmptyMsg emptyMsg) {
//////                                sendMsg2Server(message);
//////                            }
//////
//////                            @Override
//////                            public void onFailure(@NonNull IDJIError error) {
//////                                sendFailMsg2Server(message, "取消降落执行失败:" + getIDJIErrorMsg(error));
//////                            }
//////                        });
//////                        break;
//////                    case VIRTUAL_STICK:
//////                        sendFailMsg2Server(message, "已获取控制权,无需重复获取");
//////                        break;
//////                    default:
//////                        sendMsg2Server(message);
//////                        break;
//////                }
////            }
//        }
//
    }


    //飞行器虚拟摇杆
    public void sendVirtualStickAdvancedParam(MessageDown message) {
        Boolean isConnect = KeyManager.getInstance().getValue(
                KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect ) {
            if (!getGimbalAndCameraEnabled()){
                return;
            }
            if (!Movement.getInstance().isPlaneWing()){
                LogUtil.log(TAG,"飞机未起飞:禁止手控");
                return;
            }
            VirtualStickFlightControlParam param = new VirtualStickFlightControlParam();
                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);//
                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                param.setVerticalControlMode(VerticalControlMode.VELOCITY);
                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            if (!TextUtils.isEmpty(message.getData().getY())){
                param.setPitch(Double.valueOf(message.getData().getY()));//左右(速度模式-10m/s-10m/s)
            }
            if (!TextUtils.isEmpty(message.getData().getX())){
                param.setRoll(Double.valueOf(message.getData().getX()));//前后(速度模式-10m/s-10m/s)

            }
            if (!TextUtils.isEmpty(message.getData().getW())){
                param.setYaw(Double.valueOf(message.getData().getW()));//旋转(角速度模式-100-100)

            }
            if (!TextUtils.isEmpty(message.getData().getH())){
                param.setVerticalThrottle(Double.valueOf(message.getData().getH()));//上下(速度模式-4m/s-4m/s)

            }
            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);

            Movement.getInstance().setVirtualStickEnableReason(3);
//            sendMsg2Server( message, "移动...");
        }
//        else {
//            sendFailMsg2Server( message, "飞控未连接");
//        }
    }
//
//
//    public void releaseStick(){
//        VirtualStickManager.getInstance().setVirtualStickStateListener(null);
//    }

}
