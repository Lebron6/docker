//package com.aros.apron.fly;
//
//import androidx.annotation.NonNull;
//
//import com.aros.apron.entity.Movement;
//import com.aros.apron.tools.LogUtil;
//import com.google.gson.Gson;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//import dji.sdk.keyvalue.key.FlightControllerKey;
//import dji.sdk.keyvalue.key.KeyTools;
//import dji.sdk.keyvalue.value.common.EmptyMsg;
//import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem;
//import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode;
//import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode;
//import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
//import dji.sdk.keyvalue.value.flightcontroller.YawControlMode;
//import dji.v5.common.callback.CommonCallbacks;
//import dji.v5.common.error.IDJIError;
//import dji.v5.manager.KeyManager;
//import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
//
//
//public class FlyToAlternatePointManager {
//    private String TAG = "FlyToAlternatePointManager";
//
//    private AlternatePoint mWaypoint = new AlternatePoint();
//    private float mSpeed = 3.0f;
//    private double aircraftLng, aircraftLat, aircraftAlt, aircraftPich, aircraftRoll, aircraftYaw;
//    //判定是否到点的误差
//    private double delta = 1;
//    private double heightDelta = 5;
//    //任务是否开始执行
//    private boolean isRunning = false;
//    //航点任务是否开始执行，到达第一个点后才开始
//    private boolean isMissionRunning = false;
//    private int currentIndex = 0;
//    private Timer mSendVirtualStickDataTimer;
//    private SendVirtualStickDataTask mSendVirtualStickDataTask;
//    private float mPitch;
//    private float mRoll;
//    private float mYaw;
//    private float mThrottle;
//    VirtualStickFlightControlParam param;
//
//    private FlyToAlternatePointManager() {
//    }
//
//    private static class FlyToAlternatePointHolder {
//        private static final FlyToAlternatePointManager INSTANCE = new FlyToAlternatePointManager();
//    }
//
//    public static FlyToAlternatePointManager getInstance() {
//        return FlyToAlternatePointManager.FlyToAlternatePointHolder.INSTANCE;
//    }
//
//    public void setAlternateCinfig(AlternatePoint waypoint, float speed) {
//        mWaypoint = waypoint;
//        mSpeed = speed;
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            if (param == null) {
//                param = new VirtualStickFlightControlParam();
//                param.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//                param.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//                param.setVerticalControlMode(VerticalControlMode.POSITION);
//                param.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
//            }
//            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
//        }
//    }
//
//    /**
//     * 这里开始任务
//     */
//    public void start() {
//        currentIndex = 0;
//        isRunning = true;
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
//                    flyToPoint(mWaypoint);
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG, "备降点控制权获取失败:" + error.description());
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "备降点控制权获取失败:飞控未连接");
//        }
//    }
//
//    /**
//     * 飞到指定点
//     * @param point
//     */
//    private void flyToPoint(AlternatePoint point) {
//        //还没启动就不管
//        if (!isRunning) {
//            return;
//        }
//        //这两个固定，全程使用前进，全程保持高度
//        mPitch = 0;
//        mThrottle = point.getAltitude();
//        //还没获取到飞机当前状态时不管
//        if (Double.isNaN(aircraftLng) || Double.isNaN((aircraftLat))) {
//            return;
//        }
//        //先飞到指定高度再继续，比较安全
//        if (Math.abs(aircraftAlt - point.getAltitude()) > heightDelta) {
//            executeVirtualStickDataTask();
//            return;
//        }
//        double[] targetXY = GaussKruegerConverter.LngLat2XY(point.getLongitude(), point.getLatitude());
//        double[] nowXY = GaussKruegerConverter.LngLat2XY(aircraftLng, aircraftLat);
//        double dx = targetXY[0] - nowXY[0];
//        double dy = targetXY[1] - nowXY[1];
//        double d = Math.sqrt(dx * dx + dy * dy);
//        //到点
//        if (d < delta) {
//            //这里可以判断是否飞到备降点上方
//            LogUtil.log(TAG, "到达备降点上方");
//            return;
//        }
//        //没到点继续飞
//        mYaw = (float) calcDeg(nowXY, targetXY);
//        mRoll = (float) calcSpeed(nowXY, targetXY);
//        executeVirtualStickDataTask();
//    }
//
//    /***
//     * 结束任务
//     */
//    public void stop() {
//        isMissionRunning = false;
//        isRunning = false;
//        land();
//        disableVirtualStickMode();
//    }
//
//    /**
//     * 禁止虚拟摇杆
//     */
//    private void disableVirtualStickMode() {
//        if (mSendVirtualStickDataTimer != null) {
//            mSendVirtualStickDataTimer.cancel();
//        }
//        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
//        VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onSuccess() {
//                LogUtil.log(TAG, "备降点取消控制权成功");
//            }
//
//            @Override
//            public void onFailure(@NonNull IDJIError error) {
//                LogUtil.log(TAG, "备降点取消控制权失败:" + new Gson().toJson(error));
//            }
//        });
//    }
//
//    /**
//     * 降落
//     */
//    private void land() {
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
//                @Override
//                public void onSuccess(EmptyMsg emptyMsg) {
//                    LogUtil.log(TAG, "备降点降落调用成功");
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError error) {
//                    LogUtil.log(TAG, "备降点降落调用失败:" + new Gson().toJson(error));
//                }
//            });
//        } else {
//            LogUtil.log(TAG, "备降点降落调用失败:飞控未连接");
//        }
//    }
//
//
//    /**
//     * 飞行到下一个点
//     * 如果
//     */
////    public void goNextPoint() {
////        //任务执行时，或者起飞到第一个点时
////        if (isMissionRunning || currentIndex == 0) {
////            //如果是第一个点，就任务开始
////            if (currentIndex == 0) {
////                isMissionRunning = true;
////            }
////            //如果是正在执行任务并且最后一个点，就任务结束，进入停止流程
////            if (isMissionRunning && currentIndex == mWaypointList.size() - 1) {
////                isMissionRunning = false;
////                currentIndex = -1;
////                stop();
////            }
////        }
////    }
//
//    /**
//     * 计算飞行速度
//     *
//     * @param nowXY
//     * @param targetXY
//     * @return
//     */
//    private double calcSpeed(double[] nowXY, double[] targetXY) {
//        double speed = mSpeed;
//        double dx = targetXY[0] - nowXY[0];
//        double dy = targetXY[1] - nowXY[1];
//        double d = Math.sqrt(dx * dx + dy * dy);
//
//        //根据距离目标的位置计算速度，快到点了速度降下来
//        if (d < mSpeed * 2) {
//            speed = (float) d / 2;
//        }
//        return speed;
//    }
//
//    /**
//     * 计算飞行角度
//     *
//     * @param nowXY    当前飞机的位置
//     * @param targetXY 目标位置
//     * @return
//     */
//    private double calcDeg(double[] nowXY, double[] targetXY) {
//        double deg = 0;
//        double dx = targetXY[0] - nowXY[0];
//        double dy = targetXY[1] - nowXY[1];
//        //根据目标位置和飞机位置计算角度
//        if (dy > 0) {
//            deg = Math.toDegrees(Math.atan(dx / dy));
//        } else if (dy == 0) {
//            if (dx > 0) {
//                deg = 90;
//            } else if (dx == 0) {
//                deg = 0;
//            } else {
//                deg = -90;
//            }
//        } else {
//            if (dx > 0) {
//                deg = 180 + Math.toDegrees(Math.atan((targetXY[0] - nowXY[0]) / (targetXY[1] - nowXY[1])));
//            } else if (dx == 0) {
//                deg = -180;
//            } else {
//                deg = Math.toDegrees(Math.atan((targetXY[0] - nowXY[0]) / (targetXY[1] - nowXY[1]))) - 180;
//            }
//        }
//        return deg;
//    }
//
//
//    /**
//     * 执行虚拟摇杆发送任务
//     */
//    private void executeVirtualStickDataTask() {
//        if (null == mSendVirtualStickDataTimer) {
//            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
//            mSendVirtualStickDataTimer = new Timer();
//            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
//        }
//    }
//
//    /**
//     * 更新无人机状态
//     */
//    public void updateAircraftSate() {
//        aircraftLng = Double.parseDouble(Movement.getInstance().getCurrentLongitude());
//        aircraftLat = Double.parseDouble(Movement.getInstance().getCurrentLatitude());
//        aircraftAlt = Movement.getInstance().getFlyingHeight();
//        aircraftPich = Double.parseDouble(Movement.getInstance().getPitch());
//        aircraftRoll = Double.parseDouble(Movement.getInstance().getRoll());
//        aircraftYaw = Double.parseDouble(Movement.getInstance().getYaw());
//        if (isRunning) {
//            //在任务中就飞任务点
//            if (isMissionRunning || currentIndex == 0) {
//                flyToPoint(mWaypoint);
//            }
//        }
//    }
//
//    class SendVirtualStickDataTask extends TimerTask {
//        @Override
//        public void run() {
//            param.setPitch((double) mPitch);//左右
//            param.setRoll((double) mRoll);//前后
//            param.setYaw((double) mYaw);//旋转
//            param.setVerticalThrottle((double) mThrottle);//上下
//            VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param);
//        }
//    }
//}
