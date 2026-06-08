package com.aros.apron.entity;

import com.aros.apron.tools.PreferenceUtils;

public class Synchronizedstatus {
    // 1. 状态变量：保持原样 (建议用 volatile 保证可见性，虽然 synchronized 内也能保证，但加上更保险)
    private static volatile boolean FLIGHTTASK_EXECUTE_STATUS = false;
    
    // 2. 初始化状态变量：用于跟踪自检状态
    private static volatile boolean INIT_STATUS = false;
    
    // 3. 初始化运行状态变量：用于跟踪初始化是否正在进行
    private static volatile boolean INIT_RUNNING = false;


    private static volatile  boolean isruning=false;

    private static  volatile  boolean isruningframe=false;

    private  static  volatile boolean aprongim=true;


    private  static  volatile boolean switchtime=true;

    //探照灯线程
    private  static  volatile  boolean light_brightnessrunning=false;

    private  static  volatile  boolean drc_light_mode_set=false;


    private  static  volatile  boolean drc_light_fine_tuning_set=false;
    private  static  volatile  boolean  drc_light_calibration=false;

    //喊话器线程
    private  static  volatile  boolean speakrunning=false;


    private  static  volatile  boolean speakTTSrunning=false;
    private  static  volatile  boolean speaksetrunning=false;
    private  static  volatile  boolean takeoff_to_point=false;



    //flyto
    private  static volatile boolean flyto=false;




    public static boolean isFlyto() {
        return flyto;
    }

    public static void setFlyto(boolean flyto) {
        Synchronizedstatus.flyto = flyto;
    }

    public static boolean isTakeoff_to_point() {
        return takeoff_to_point;
    }

    public static void setTakeoff_to_point(boolean takeoff_to_point) {
        Synchronizedstatus.takeoff_to_point = takeoff_to_point;
    }

    public static boolean isDrc_light_mode_set() {
        return drc_light_mode_set;
    }

    public static void setDrc_light_mode_set(boolean drc_light_mode_set) {
        Synchronizedstatus.drc_light_mode_set = drc_light_mode_set;
    }

    public static boolean isDrc_light_fine_tuning_set() {
        return drc_light_fine_tuning_set;
    }

    public static void setDrc_light_fine_tuning_set(boolean drc_light_fine_tuning_set) {
        Synchronizedstatus.drc_light_fine_tuning_set = drc_light_fine_tuning_set;
    }

    public static boolean isDrc_light_calibration() {
        return drc_light_calibration;
    }

    public static void setDrc_light_calibration(boolean drc_light_calibration) {
        Synchronizedstatus.drc_light_calibration = drc_light_calibration;
    }

    public static boolean isSpeaksetrunning() {
        return speaksetrunning;
    }

    public static void setSpeaksetrunning(boolean speaksetrunning) {
        Synchronizedstatus.speaksetrunning = speaksetrunning;
    }

    public static boolean isSpeakTTSrunning() {
        return speakTTSrunning;
    }

    public static void setSpeakTTSrunning(boolean speakTTSrunning) {
        Synchronizedstatus.speakTTSrunning = speakTTSrunning;
    }

    public static boolean isSpeakrunning() {
        return speakrunning;
    }

    public static void setSpeakrunning(boolean speakrunning) {
        Synchronizedstatus.speakrunning = speakrunning;
    }

    public static boolean isLight_brightnessrunning() {
        return light_brightnessrunning;
    }

    public static void setLight_brightnessrunning(boolean light_brightnessrunning) {
        Synchronizedstatus.light_brightnessrunning = light_brightnessrunning;
    }

    public static boolean isSwitchtime() {
        return switchtime;
    }

    public static void setSwitchtime(boolean switchtime) {
        Synchronizedstatus.switchtime = switchtime;
    }

    public static boolean isAprongim() {
        return aprongim;
    }

    public static void setAprongim(boolean aprongim) {
        Synchronizedstatus.aprongim = aprongim;
    }

    public static boolean isIsruningframe() {
        return isruningframe;
    }



    public static void setIsruningframe(boolean isruningframe) {
        Synchronizedstatus.isruningframe = isruningframe;
    }

    // 4. 【新增】专门的锁对象：必须是 Object 类型，且唯一
    // 这个对象什么都不存，只用来当“钥匙”
    public static final Object LOCK_OBJ = new Object();

    // 提供 getter/setter 方便访问状态


    public static boolean isIsruning() {
        return isruning;
    }

    public static void setIsruning(boolean isruning) {
        Synchronizedstatus.isruning = isruning;
    }

    public static boolean getFlighttaskExecuteStatus() {
        return FLIGHTTASK_EXECUTE_STATUS;
    }

    public static void setFlighttaskExecuteStatus(boolean status) {
        FLIGHTTASK_EXECUTE_STATUS = status;
    }
    
    // 提供 getter/setter 用于初始化状态
    public static boolean getInitStatus() {
        return INIT_STATUS;
    }
    
    public static void setInitStatus(boolean status) {
        INIT_STATUS = status;
    }
    
    // 提供 getter/setter 用于第一次收到航线指令标志位（持久化存储）
    public static boolean isFirstMissionReceived() {
        return PreferenceUtils.getInstance().isFirstMissionReceived();
    }
    
    public static void setFirstMissionReceived(boolean status) {
        PreferenceUtils.getInstance().setFirstMissionReceived(status);
    }
    
    // 提供 getter/setter 用于初始化运行状态
    public static boolean isInitRunning() {
        return INIT_RUNNING;
    }
    
    public static void setInitRunning(boolean running) {
        INIT_RUNNING = running;
    }



}