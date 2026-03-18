package com.aros.apron.constant;

//接收云端下发的指令集合
public class Constant {

    /**
     * 航线下发
     */
    public static final String FLIGHTTASK_EXECUTE="flighttask_execute";

    /**
     * 查询遥控器是否开机
     */
    public static final String PILOT_ON="pilot_on";

    /**
     * 查询飞机是否开机
     */
    public static final String AIRCRAFT_ON="aircraft_on";

    /**
     * 文件上传结束
     */
    public static final String MEDIA_UPLOAD_COMPLETE="media_upload_complete";

    /**
     * 打开慢转浆
     */
    public static final String OPEN_SLOW_PROPELLER_ROTATION="open_slow_propeller_rotation";

    /**
     * 关闭慢转浆
     */
    public static final String CLOSE_SLOW_PROPELLER_ROTATION="close_slow_propeller_rotation";

    /**
     * 开始直播
     */
    public static final String LIVE_START_PUSH="live_start_push";

    /**
     * 停止直播
     */
    public static final String LIVE_STOP_PUSH="live_stop_push";

    /**
     * 设置直播清晰度
     */
    public static final String LIVE_SET_QUALITY="live_set_quality";

    /**
     * 设置直播镜头
     */
    public static final String LIVE_LENS_CHANGE="live_lens_change";

    /**
     * 航线暂停
     */
    public static final String FLIGHTTASK_PAUSE="flighttask_pause";

    /**
     * 航线继续
     */
    public static final String FLIGHTTASK_RECOVERY="flighttask_recovery";

    /**
     * 返航
     */
    public static final String RETURN_HOME="return_home";

    /**
     * 取消返航
     */
    public static final String RETURN_HOME_CANCEL="return_home_cancel";

    /**
     * 任务失败(服务端收到后自动执行入库)
     */
    public static final String TASK_FAIL="task_fail";

    /**
     * 开舱门
     */
    public static final String OPEN_DOOR="open_door";

    /**
     * 关舱门
     */
    public static final String CLOSE_DOOR="close_door";

    /**
     * 入库
     */
    public static final String INBOUND="inbound";

    /**
     * 一键起飞
     */
    public static final String TAKEOFF_TO_POINT="takeoff_to_point";

    /**
     * 飞向目标点
     */
    public static final String FLY_TO_POINT="fly_to_point";

    /**
     * 结束 flyto 飞向目标点任务
     */
    public static final String FLY_TO_POINT_STOP="fly_to_point_stop";

    /**
     * 更新 flyto 目标点
     */
    public static final String FLY_TO_POINT_STOP_UPDATE="fly_to_point_update";

    /**
     * 飞行控制权抢夺
     */
    public static final String FLIGHT_AUTHORITY_GRAB="flight_authority_grab";

    /**
     * 负载控制权抢夺
     */
    public static final String PAYLOAD_AUTHORITY_GRAB="payload_authority_grab";

    /**
     * 进入指令飞行控制模式
     */
    public static final String DRC_MODE_ENTER="drc_mode_enter";

    /**
     * 退出指令飞行控制模式
     */
    public static final String DRC_MODE_EXIT="drc_mode_exit";

    /**
     * DRC-飞行控制
     */
    public static final String DRONE_CONTROL="drone_control";

    /**
     * DRC-飞行器急停
     */
    public static final String DRONE_EMERGENCY_STOP="drone_emergency_stop";

    /**
     * 负载控制—切换相机模式
     */
    public static final String CAMERA_MODE_SWITCH="camera_mode_switch";

    /**
     * 负载控制—开始拍照
     */
    public static final String CAMERA_PHOTO_TAKE="camera_photo_take";

    /**
     * 负载控制—停止拍照
     */
    public static final String CAMERA_PHOTO_STOP="camera_photo_stop";

    /**
     * 负载控制—开始录像
     */
    public static final String CAMERA_RECORDING_START="camera_recording_start";

    /**
     * 负载控制—停止录像
     */
    public static final String CAMERA_RECORDING_STOP="camera_recording_stop";

    /**
     * 负载控制—画面拖动控制
     */
    public static final String CAMERA_SCREEN_DRAG="camera_screen_drag";

    /**
     * 负载控制—双击成为 AIM
     */
    public static final String CAMERA_AIM="camera_aim";

    /**
     * 负载控制—变焦
     */
    public static final String CAMERA_FOCAL_LENGTH_SET="camera_focal_length_set";

    /**
     * 负载控制—重置云台
     */
    public static final String GIMBAL_RESET="gimbal_reset";

    /**
     * 负载控制—Look At
     */
    public static final String CAMERA_LOOK_AT="camera_look_at";

    /**
     * 负载控制—分屏
     */
    public static final String CAMERA_SCREEN_SPLIT="camera_screen_split";

    /**
     * 负载控制—照片存储设置
     */
    public static final String PHOTO_STORAGE_SET="photo_storage_set";

    /**
     * 负载控制—视频存储设置
     */
    public static final String VIDEO_STORAGE_SET="video_storage_set";

    /**
     * 负载控制—相机曝光模式设置
     */
    public static final String CAMERA_EXPOSURE_MODE_SET="camera_exposure_mode_set";

    /**
     * 负载控制—相机曝光值调节
     */
    public static final String CAMERA_EXPOSURE_SET="camera_exposure_set";

    /**
     * 负载控制—相机对焦模式
     */
    public static final String CAMERA_FOCUS_MODE_SET="camera_focus_mode_set";

    /**
     * 负载控制—相机对焦值设置
     */
    public static final String CAMERA_FOCUS_MODE_VALUE_SET="camera_focus_value_set";

    /**
     * 负载控制—点对焦
     */
    public static final String CAMERA_POINT_FOCUS_ACTION="camera_point_focus_action";

    /**
     * 负载控制—红外测温模式设置
     */
    public static final String IR_METERING_MODE_SET="ir_metering_mode_set";

    /**
     * 负载控制—红外测温点设置
     */
    public static final String IR_METERING_POINT_SET="ir_metering_point_set";

    /**
     * 负载控制—红外测温区域设置
     */
    public static final String IR_METERING_AREA_SET="ir_metering_area_set";

    /**
     * 飞行控制—进入 POI 环绕模式
     */
    public static final String POI_MODE_ENTER="poi_mode_enter";

    /**
     * 飞行控制—退出 POI 环绕模式
     */
    public static final String POI_MODE_EXIT="poi_mode_exit";

    /**
     * 飞行控制—POI 环绕速度设置
     */
    public static final String POI_CIRCLE_SPEED_SET="poi_circle_speed_set";

    /**
     *  媒体上传事件
     */
    public static final String FILE_UPLOAD_CALLBACK="file_upload_callback";

    /**
     *  上报航线任务进度
     */
    public static final String FLIGHT_TASK_PROGRESS="flighttask_progress";

    /**
     *  遥控器上报sdr和4g状态
     */
    public static final String WIRELESS_LINK="wireless_link";

    /**
     *  喊话器-开始播放音频
     */
    public static final String SPEAKER_AUDIO_PLAY_START="speaker_audio_play_start";

    /**
     *  喊话器-开始播放TTS文本
     */
    public static final String SPEAKER_TTS_PLAY_START="speaker_tts_play_start";

    /**
     *  喊话器-重新播放
     */
    public static final String SPEAKER_REPLAY="speaker_replay";

    /**
     *  喊话器-停止播放
     */
    public static final String SPEAKER_PLAY_STOP="speaker_play_stop";

    /**
     *  喊话器-设置播放模式
     */
    public static final String SPEAKER_PLAY_MODE_SET="speaker_play_mode_set";

    /**
     *  喊话器-设置音量
     */
    public static final String SPEAKER_PLAY_VOLUME_SET="speaker_play_volume_set";

    /**
     *  喊话器—TTS喊话设置
     */
    public static final String DRC_SPEAKER_TTS_SET="drc_speaker_tts_set";


}
