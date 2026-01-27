package com.aros.apron.entity;


import java.util.List;

public class Movement {

    private static class MovementHolder {
        private static final Movement INSTANCE = new Movement();
    }

    private Movement() {
    }

    public static final Movement getInstance() {
        return MovementHolder.INSTANCE;
    }

    private int msg_type = 60013;

    private int ultrasonicHeight;//超声波测高，只在飞行高度5m时奏效,单位/分米
    private int remoteControlSignal;//遥控器信号
    private int pictureBiographySignal;//图传信号
    private double batteryTemperatureA;//A电池温度
    private int electricityInfoB;//B电量信息
    private double batteryTemperatureB;//B电池温度
    private int currentView = 0;//当前视角 1FPV 0云台
    private String planeMessage;//飞机提示信息
    private String warningMessage;//飞机警告信息
    private int angleYaw;//飞机飞行机头角度
    private String flightPathName;//航线名称
    private String planeMode;//飞机模式字符串
    private int missionStateCode;//航线任务状态
    private boolean isMissionFinish;//用来标识航线in_progress还是ok

    private int cameraMode;//相机模式(拍照/录像) 0拍照 1录像
    private int isShootingPhoto;//是否正在拍照 1正在拍照 0未在拍照
    private int isRecording;//是否正在录像 1正在录像 0未录像
    private int recordingTime;//录像时间 单位s
    private int cameraVideoStreamSource;//当前视频源 1广角 2变焦 3红外
    private double cameraZoomRatios;//镜头变焦倍数
    private double thermalZoomRatios;//红外镜头变焦倍数
    private boolean continuous;//变焦档位是否连续 (true表示gears最小值-最大值都可使用，false表示只支持数组内关键档位)
    private int[] gears;//变焦倍率范围的关键档位
    private int isVirtualStickEnable;//是否获取控制权 0未获取控制权  1已获取控制权
    private int isVirtualStickAdvancedModeEnabled;//是否处于虚拟摇杆高级模式
    private boolean isDistanceLimitEnabled;//是否启用限远
    private int thermalDisplayMode;//红外模式 1仅红外  2红外分屏
    private String flightId;//航线id
    private String waypointMissionExecuteState;//航线执行状态
    private String waylineExecutingInterruptReason;//航线暂停原因
    private int goHomeState;//返航执行状态  0未触发返航 1返航中 2返航下降中 3返航完成
    private int returnHomePower;//返航电量所需百分比
    private int landingPower;//降落电量所需百分比
    private int lowBatteryRTHState;//智能低电量返航状态 0未触发智能低电量返航 1触发智能低电量返航，飞行器正在倒计时 2执行智能低电量返航 3智能低电量返航被取消
    private String missionName;//当前正在执行的航线名
    private int currentWaypointIndex=-1;//当前航点下标

    private boolean planeWing;//飞机是否在飞
    private boolean isMotorsOn;//电机是否起转
    private String aircraftTotalFlightDistance;//总体飞行距离，单位：米。飞行器断电后不会清零。
    private String aircraftTotalFlightTimes;//总体飞行次数，飞行器断电后不会清零。
    private String aircraftTotalFlightDuration;//总体飞行时长，单位：秒。飞行器断电后不会清零。


    public int flightControlAuthority;//当前控制权所属
    private String alternatePointLat;//设置备降点经纬度
    private String alternatePointLon;
    private int thermalTemperatureMeasureMode;//测温模式
    private String spotMetersureTemperature;//测温点温度
    private String averageAreaTemperature;//平均温度
    private String minAreaTemperature;//最小温度
    private String maxAreaTemperature;//最大温度
    private String maxTemperaturePointX;//最大温度的位置
    private String maxTemperaturePointY;
    private String minTemperaturePointX;//最小温度的位置
    private String minTemperaturePointY;
    private String LTELinkType;//图传类型  1cusync 图传 3LTE 增强图传
    private int goHomeHeight;//返航高度
    private int failsafeAction;//失控动作
    private int heightLimit;//限高
    private int distanceLimit;//限远
    private int distanceLimitEnabled;//限远是否启用 0未启用 1启用
    private int lowBatteryWarningThreshold;//低电量报警阈值
    private int seriousLowBatteryWarningThreshold;//严重低电量报警阈值
    private int lowBatteryRTHEnabled;//智能低电量返航是否启用 0 未启用 1启用
    private String taskId;
    private String version;
    private int virtualStickEnableReason;//0未启用 虚拟摇杆启用原因 1异常拉高返航触发 2视觉降落触发 3手动触发
    private int photoIntervalCount;//当前定时拍照默认设置张数
    private double photoInterval;//当前定时拍照默认时间间隔
    private boolean isShootingPhotoPanorama;//当前是否正在全景拍照
    private int photoPanoramaProgress;//全景拍照进度百分比
    private int navigationSatelliteSystem;//当前卫星地位系统 0GPS 1北斗

    private String productName;//产品类型 例:M350_RTK
    private String serialNumber;//飞控序列号
    private String remoteSerialNumber;//遥控器序列号
    private String remoteBatteryPercent;//遥控器剩余电量百分比
    private String remoteSecondBatteryPercent;//遥控器外接电池剩余电量百分比
    private List<PayloadInfo> payloadInfos;//当前飞机负载设备信息
    private boolean propellerRotation;//飞机是否正在低速慢转桨
    private boolean vtx;//检查图传是否正常
    private int missionType;//任务类型 一键起飞航线，定频推送只会推送3
    private boolean virtualStickStatus;//前端判断是否显示/隐藏手控按钮
    private boolean cancelVirtualStickStatus;//前端判断是否显示/隐藏取消手控按钮
    private boolean pauseMissionStautus;//前端判断是否显示/隐藏暂停航线
    private boolean resumeMissionStatus;//前端判断是否显示/隐藏继续航线
    private boolean isVirtualStickQuitMission;//用户手动后退出航线


    //适配上云格式参数，拿到后再进行组装
    private double measure_target_altitude;
    private int measure_target_distance;
    private int measure_target_error_state;
    private double measure_target_latitude;
    private double measure_target_longitude;
    private String payload_index;
    private int thermal_current_palette_style;
    private int thermal_gain_mode;
    private double thermal_global_temperature_max;
    private double thermal_global_temperature_min;
    private int thermal_isotherm_lower_limit;
    private int thermal_isotherm_state;
    private int thermal_isotherm_upper_limit;

    private int gimbal_pitch;
    private int gimbal_roll;
    private double gimbal_yaw;

    private int capacity_percent;
    private int battery_a_capacity_percent;
    private String battery_a_battery_firmware_version;
    private int battery_a_high_voltage_storage_days;
    private int battery_a_index;
    private int battery_a_loop_times;
    private String battery_a_battery_sn;
    private int battery_a_sub_type;
    private double battery_a_temperature;
    private int battery_a_type;
    private int battery_a_voltage;

    private int battery_b_capacity_percent;
    private String battery_b_battery_firmware_version;
    private int battery_b_high_voltage_storage_days;
    private int battery_b_index;
    private int battery_b_loop_times;
    private String battery_b_battery_sn;
    private int battery_b_sub_type;
    private double battery_b_temperature;
    private int battery_b_type;
    private int battery_b_voltage;


    private int landing_power;
    private int remain_flight_time;
    private int return_home_power;

    private double attitude_head;
    private double attitude_pitch;
    private double attitude_roll;
    private String country;
    private double elevation;
    private double rtk_takeoff_altitude;
    private double homepoint_latitude;
    private double homepoint_longitude;
    private String firmware_version;
    private int gear;
    private double height;
    private int height_limit;
    private double home_distance;
    private double horizontal_speed;
    private int is_near_area_limit;
    private int is_near_height_limit;
    private double latitude;
    private double longitude;
    private int mode_code;
    private int night_lights_state;
    private int rc_lost_action;
    private boolean rid_state;
    private int rth_altitude;
    private double total_flight_distance;
    private int total_flight_sorties;
    private double total_flight_time;
    private String track_id;
    private double vertical_speed;
    private int wind_direction;
    private int wind_speed;

    private int camera_mode;
    private int ir_metering_mode;
    private int temperature;
    private double x;
    private double y;
    private int ir_zoom_factor;
    private double bottom;
    private double left;
    private double right;
    private double top;
    private int photo_state;
    private List<String> photo_storage_settings;
    private int record_time;
    private int recording_state;
    private int remain_photo_num;
    private int remain_record_duration;
    private boolean screen_split_enable;
    private int wide_exposure_mode;
    private int wide_exposure_value;
    private int wide_iso;
    private int wide_shutter_speed;
    private int zoom_calibrate_farthest_focus_value;
    private int zoom_calibrate_nearest_focus_value;
    private int zoom_exposure_mode;
    private int zoom_exposure_value;
    private double zoom_factor;
    private int zoom_focus_mode;
    private int zoom_focus_state;
    private int zoom_focus_value;
    private int zoom_iso;
    private int zoom_max_focus_value;
    private int zoom_min_focus_value;
    private int zoom_shutter_speed;

    private int distance_limit;
    private int is_near_distance_limit;
    private int state;

    private int downside;
    private int horizon;
    private int upside;

    private int gps_number;
    private int is_fixed;
    private int quality;
    private int rtk_number;

    private int total;
    private int used;

    private double task_attitude_head;
    private int task_break_reason;
    private double task_height;
    private int task_index;
    private double task_latitude;
    private double task_longitude;
    private double task_progress;
    private int task_state;
    private String task_status;
    private int task_wayline_id;

    private int task_current_waypoint_index;
    private String task_flight_id;
    private int task_media_count;
    private String task_track_id;
    private int task_out_task_wayline_id;
    private int task_wayline_mission_state;

    private int task_current_step;
    private int task_percent;

    private String dongle_number;
    private String link_state_4g;
    private String sdr_link_state;
    private String link_workmode;
    private String sdr_quality;
    private String quality_4g;
    private String uav_quality_4g;
    private String gnd_quality_4g;
    private String sdr_freq_band;
    private String freq_band_4g;

    public String getDongle_number() {
        return dongle_number;
    }

    public void setDongle_number(String dongle_number) {
        this.dongle_number = dongle_number;
    }

    public String getLink_state_4g() {
        return link_state_4g;
    }

    public void setLink_state_4g(String link_state_4g) {
        this.link_state_4g = link_state_4g;
    }

    public String getSdr_link_state() {
        return sdr_link_state;
    }

    public void setSdr_link_state(String sdr_link_state) {
        this.sdr_link_state = sdr_link_state;
    }

    public String getLink_workmode() {
        return link_workmode;
    }

    public void setLink_workmode(String link_workmode) {
        this.link_workmode = link_workmode;
    }

    public String getSdr_quality() {
        return sdr_quality;
    }

    public void setSdr_quality(String sdr_quality) {
        this.sdr_quality = sdr_quality;
    }

    public String getQuality_4g() {
        return quality_4g;
    }

    public void setQuality_4g(String quality_4g) {
        this.quality_4g = quality_4g;
    }

    public String getUav_quality_4g() {
        return uav_quality_4g;
    }

    public void setUav_quality_4g(String uav_quality_4g) {
        this.uav_quality_4g = uav_quality_4g;
    }

    public String getGnd_quality_4g() {
        return gnd_quality_4g;
    }

    public void setGnd_quality_4g(String gnd_quality_4g) {
        this.gnd_quality_4g = gnd_quality_4g;
    }

    public String getSdr_freq_band() {
        return sdr_freq_band;
    }

    public void setSdr_freq_band(String sdr_freq_band) {
        this.sdr_freq_band = sdr_freq_band;
    }

    public String getFreq_band_4g() {
        return freq_band_4g;
    }

    public void setFreq_band_4g(String freq_band_4g) {
        this.freq_band_4g = freq_band_4g;
    }

    public String getTask_status() {
        return task_status;
    }

    public void setTask_status(String task_status) {
        this.task_status = task_status;
    }

    public boolean isMissionFinish() {
        return isMissionFinish;
    }

    public void setMissionFinish(boolean missionFinish) {
        isMissionFinish = missionFinish;
    }

    public double getTask_attitude_head() {
        return task_attitude_head;
    }

    public void setTask_attitude_head(double task_attitude_head) {
        this.task_attitude_head = task_attitude_head;
    }

    public int getTask_break_reason() {
        return task_break_reason;
    }

    public void setTask_break_reason(int task_break_reason) {
        this.task_break_reason = task_break_reason;
    }

    public double getTask_height() {
        return task_height;
    }

    public void setTask_height(double task_height) {
        this.task_height = task_height;
    }

    public int getTask_index() {
        return task_index;
    }

    public void setTask_index(int task_index) {
        this.task_index = task_index;
    }

    public double getTask_latitude() {
        return task_latitude;
    }

    public void setTask_latitude(double task_latitude) {
        this.task_latitude = task_latitude;
    }

    public double getTask_longitude() {
        return task_longitude;
    }

    public void setTask_longitude(double task_longitude) {
        this.task_longitude = task_longitude;
    }

    public double getTask_progress() {
        return task_progress;
    }

    public void setTask_progress(double task_progress) {
        this.task_progress = task_progress;
    }

    public int getTask_state() {
        return task_state;
    }

    public void setTask_state(int task_state) {
        this.task_state = task_state;
    }

    public int getTask_wayline_id() {
        return task_wayline_id;
    }

    public void setTask_wayline_id(int task_wayline_id) {
        this.task_wayline_id = task_wayline_id;
    }

    public int getTask_current_waypoint_index() {
        return task_current_waypoint_index;
    }

    public void setTask_current_waypoint_index(int task_current_waypoint_index) {
        this.task_current_waypoint_index = task_current_waypoint_index;
    }

    public String getTask_flight_id() {
        return task_flight_id;
    }

    public void setTask_flight_id(String task_flight_id) {
        this.task_flight_id = task_flight_id;
    }

    public int getTask_media_count() {
        return task_media_count;
    }

    public void setTask_media_count(int task_media_count) {
        this.task_media_count = task_media_count;
    }

    public String getTask_track_id() {
        return task_track_id;
    }

    public void setTask_track_id(String task_track_id) {
        this.task_track_id = task_track_id;
    }

    public int getTask_out_task_wayline_id() {
        return task_out_task_wayline_id;
    }

    public void setTask_out_task_wayline_id(int task_out_task_wayline_id) {
        this.task_out_task_wayline_id = task_out_task_wayline_id;
    }

    public int getTask_wayline_mission_state() {
        return task_wayline_mission_state;
    }

    public void setTask_wayline_mission_state(int task_wayline_mission_state) {
        this.task_wayline_mission_state = task_wayline_mission_state;
    }

    public int getTask_current_step() {
        return task_current_step;
    }

    public void setTask_current_step(int task_current_step) {
        this.task_current_step = task_current_step;
    }

    public int getTask_percent() {
        return task_percent;
    }

    public void setTask_percent(int task_percent) {
        this.task_percent = task_percent;
    }

    public int getMissionStateCode() {
        return missionStateCode;
    }

    public void setMissionStateCode(int missionStateCode) {
        this.missionStateCode = missionStateCode;
    }

    public String getPlaneMode() {
        return planeMode;
    }

    public void setPlaneMode(String planeMode) {
        this.planeMode = planeMode;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }

    public int getGps_number() {
        return gps_number;
    }

    public void setGps_number(int gps_number) {
        this.gps_number = gps_number;
    }

    public int getIs_fixed() {
        return is_fixed;
    }

    public void setIs_fixed(int is_fixed) {
        this.is_fixed = is_fixed;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getRtk_number() {
        return rtk_number;
    }

    public void setRtk_number(int rtk_number) {
        this.rtk_number = rtk_number;
    }

    public int getDownside() {
        return downside;
    }

    public void setDownside(int downside) {
        this.downside = downside;
    }

    public int getHorizon() {
        return horizon;
    }

    public void setHorizon(int horizon) {
        this.horizon = horizon;
    }

    public int getUpside() {
        return upside;
    }

    public void setUpside(int upside) {
        this.upside = upside;
    }

    public int getDistance_limit() {
        return distance_limit;
    }

    public void setDistance_limit(int distance_limit) {
        this.distance_limit = distance_limit;
    }

    public int getIs_near_distance_limit() {
        return is_near_distance_limit;
    }

    public void setIs_near_distance_limit(int is_near_distance_limit) {
        this.is_near_distance_limit = is_near_distance_limit;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getBottom() {
        return bottom;
    }

    public void setBottom(double bottom) {
        this.bottom = bottom;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getRight() {
        return right;
    }

    public void setRight(double right) {
        this.right = right;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }

    public int getCamera_mode() {
        return camera_mode;
    }

    public void setCamera_mode(int camera_mode) {
        this.camera_mode = camera_mode;
    }

    public int getIr_metering_mode() {
        return ir_metering_mode;
    }

    public void setIr_metering_mode(int ir_metering_mode) {
        this.ir_metering_mode = ir_metering_mode;
    }


    public int getIr_zoom_factor() {
        return ir_zoom_factor;
    }

    public void setIr_zoom_factor(int ir_zoom_factor) {
        this.ir_zoom_factor = ir_zoom_factor;
    }


    public int getPhoto_state() {
        return photo_state;
    }

    public void setPhoto_state(int photo_state) {
        this.photo_state = photo_state;
    }

    public List<String> getPhoto_storage_settings() {
        return photo_storage_settings;
    }

    public void setPhoto_storage_settings(List<String> photo_storage_settings) {
        this.photo_storage_settings = photo_storage_settings;
    }

    public int getRecord_time() {
        return record_time;
    }

    public void setRecord_time(int record_time) {
        this.record_time = record_time;
    }

    public int getRecording_state() {
        return recording_state;
    }

    public void setRecording_state(int recording_state) {
        this.recording_state = recording_state;
    }

    public int getRemain_photo_num() {
        return remain_photo_num;
    }

    public void setRemain_photo_num(int remain_photo_num) {
        this.remain_photo_num = remain_photo_num;
    }

    public int getRemain_record_duration() {
        return remain_record_duration;
    }

    public void setRemain_record_duration(int remain_record_duration) {
        this.remain_record_duration = remain_record_duration;
    }

    public boolean isScreen_split_enable() {
        return screen_split_enable;
    }

    public void setScreen_split_enable(boolean screen_split_enable) {
        this.screen_split_enable = screen_split_enable;
    }

    public int getWide_exposure_mode() {
        return wide_exposure_mode;
    }

    public void setWide_exposure_mode(int wide_exposure_mode) {
        this.wide_exposure_mode = wide_exposure_mode;
    }

    public int getWide_exposure_value() {
        return wide_exposure_value;
    }

    public void setWide_exposure_value(int wide_exposure_value) {
        this.wide_exposure_value = wide_exposure_value;
    }

    public int getWide_iso() {
        return wide_iso;
    }

    public void setWide_iso(int wide_iso) {
        this.wide_iso = wide_iso;
    }

    public int getWide_shutter_speed() {
        return wide_shutter_speed;
    }

    public void setWide_shutter_speed(int wide_shutter_speed) {
        this.wide_shutter_speed = wide_shutter_speed;
    }

    public int getZoom_calibrate_farthest_focus_value() {
        return zoom_calibrate_farthest_focus_value;
    }

    public void setZoom_calibrate_farthest_focus_value(int zoom_calibrate_farthest_focus_value) {
        this.zoom_calibrate_farthest_focus_value = zoom_calibrate_farthest_focus_value;
    }

    public int getZoom_calibrate_nearest_focus_value() {
        return zoom_calibrate_nearest_focus_value;
    }

    public void setZoom_calibrate_nearest_focus_value(int zoom_calibrate_nearest_focus_value) {
        this.zoom_calibrate_nearest_focus_value = zoom_calibrate_nearest_focus_value;
    }

    public int getZoom_exposure_mode() {
        return zoom_exposure_mode;
    }

    public void setZoom_exposure_mode(int zoom_exposure_mode) {
        this.zoom_exposure_mode = zoom_exposure_mode;
    }

    public int getZoom_exposure_value() {
        return zoom_exposure_value;
    }

    public void setZoom_exposure_value(int zoom_exposure_value) {
        this.zoom_exposure_value = zoom_exposure_value;
    }

    public double getZoom_factor() {
        return zoom_factor;
    }

    public void setZoom_factor(double zoom_factor) {
        this.zoom_factor = zoom_factor;
    }

    public int getZoom_focus_mode() {
        return zoom_focus_mode;
    }

    public void setZoom_focus_mode(int zoom_focus_mode) {
        this.zoom_focus_mode = zoom_focus_mode;
    }

    public int getZoom_focus_state() {
        return zoom_focus_state;
    }

    public void setZoom_focus_state(int zoom_focus_state) {
        this.zoom_focus_state = zoom_focus_state;
    }

    public int getZoom_focus_value() {
        return zoom_focus_value;
    }

    public void setZoom_focus_value(int zoom_focus_value) {
        this.zoom_focus_value = zoom_focus_value;
    }

    public int getZoom_iso() {
        return zoom_iso;
    }

    public void setZoom_iso(int zoom_iso) {
        this.zoom_iso = zoom_iso;
    }

    public int getZoom_max_focus_value() {
        return zoom_max_focus_value;
    }

    public void setZoom_max_focus_value(int zoom_max_focus_value) {
        this.zoom_max_focus_value = zoom_max_focus_value;
    }

    public int getZoom_min_focus_value() {
        return zoom_min_focus_value;
    }

    public void setZoom_min_focus_value(int zoom_min_focus_value) {
        this.zoom_min_focus_value = zoom_min_focus_value;
    }

    public int getZoom_shutter_speed() {
        return zoom_shutter_speed;
    }

    public void setZoom_shutter_speed(int zoom_shutter_speed) {
        this.zoom_shutter_speed = zoom_shutter_speed;
    }

    public int getCapacity_percent() {
        return capacity_percent;
    }

    public void setCapacity_percent(int capacity_percent) {
        this.capacity_percent = capacity_percent;
    }

    public int getBattery_a_capacity_percent() {
        return battery_a_capacity_percent;
    }

    public void setBattery_a_capacity_percent(int battery_a_capacity_percent) {
        this.battery_a_capacity_percent = battery_a_capacity_percent;
    }

    public String getBattery_a_battery_firmware_version() {
        return battery_a_battery_firmware_version;
    }

    public void setBattery_a_battery_firmware_version(String battery_a_battery_firmware_version) {
        this.battery_a_battery_firmware_version = battery_a_battery_firmware_version;
    }

    public int getBattery_a_high_voltage_storage_days() {
        return battery_a_high_voltage_storage_days;
    }

    public void setBattery_a_high_voltage_storage_days(int battery_a_high_voltage_storage_days) {
        this.battery_a_high_voltage_storage_days = battery_a_high_voltage_storage_days;
    }

    public int getBattery_b_capacity_percent() {
        return battery_b_capacity_percent;
    }

    public void setBattery_b_capacity_percent(int battery_b_capacity_percent) {
        this.battery_b_capacity_percent = battery_b_capacity_percent;
    }

    public String getBattery_b_battery_firmware_version() {
        return battery_b_battery_firmware_version;
    }

    public void setBattery_b_battery_firmware_version(String battery_b_battery_firmware_version) {
        this.battery_b_battery_firmware_version = battery_b_battery_firmware_version;
    }

    public int getBattery_b_high_voltage_storage_days() {
        return battery_b_high_voltage_storage_days;
    }

    public void setBattery_b_high_voltage_storage_days(int battery_b_high_voltage_storage_days) {
        this.battery_b_high_voltage_storage_days = battery_b_high_voltage_storage_days;
    }

    public int getBattery_a_index() {
        return battery_a_index;
    }

    public void setBattery_a_index(int battery_a_index) {
        this.battery_a_index = battery_a_index;
    }

    public int getBattery_a_loop_times() {
        return battery_a_loop_times;
    }

    public void setBattery_a_loop_times(int battery_a_loop_times) {
        this.battery_a_loop_times = battery_a_loop_times;
    }

    public String getBattery_a_battery_sn() {
        return battery_a_battery_sn;
    }

    public void setBattery_a_battery_sn(String battery_a_battery_sn) {
        this.battery_a_battery_sn = battery_a_battery_sn;
    }

    public int getBattery_a_sub_type() {
        return battery_a_sub_type;
    }

    public void setBattery_a_sub_type(int battery_a_sub_type) {
        this.battery_a_sub_type = battery_a_sub_type;
    }

    public double getBattery_a_temperature() {
        return battery_a_temperature;
    }

    public void setBattery_a_temperature(double battery_a_temperature) {
        this.battery_a_temperature = battery_a_temperature;
    }

    public int getBattery_a_type() {
        return battery_a_type;
    }

    public void setBattery_a_type(int battery_a_type) {
        this.battery_a_type = battery_a_type;
    }

    public int getBattery_a_voltage() {
        return battery_a_voltage;
    }

    public void setBattery_a_voltage(int battery_a_voltage) {
        this.battery_a_voltage = battery_a_voltage;
    }

    public int getBattery_b_index() {
        return battery_b_index;
    }

    public void setBattery_b_index(int battery_b_index) {
        this.battery_b_index = battery_b_index;
    }

    public int getBattery_b_loop_times() {
        return battery_b_loop_times;
    }

    public void setBattery_b_loop_times(int battery_b_loop_times) {
        this.battery_b_loop_times = battery_b_loop_times;
    }

    public String getBattery_b_battery_sn() {
        return battery_b_battery_sn;
    }

    public void setBattery_b_battery_sn(String battery_b_battery_sn) {
        this.battery_b_battery_sn = battery_b_battery_sn;
    }

    public int getBattery_b_sub_type() {
        return battery_b_sub_type;
    }

    public void setBattery_b_sub_type(int battery_b_sub_type) {
        this.battery_b_sub_type = battery_b_sub_type;
    }

    public double getBattery_b_temperature() {
        return battery_b_temperature;
    }

    public void setBattery_b_temperature(double battery_b_temperature) {
        this.battery_b_temperature = battery_b_temperature;
    }

    public int getBattery_b_type() {
        return battery_b_type;
    }

    public void setBattery_b_type(int battery_b_type) {
        this.battery_b_type = battery_b_type;
    }

    public int getBattery_b_voltage() {
        return battery_b_voltage;
    }

    public void setBattery_b_voltage(int battery_b_voltage) {
        this.battery_b_voltage = battery_b_voltage;
    }

    public int getLanding_power() {
        return landing_power;
    }

    public void setLanding_power(int landing_power) {
        this.landing_power = landing_power;
    }

    public int getRemain_flight_time() {
        return remain_flight_time;
    }

    public void setRemain_flight_time(int remain_flight_time) {
        this.remain_flight_time = remain_flight_time;
    }

    public int getReturn_home_power() {
        return return_home_power;
    }

    public void setReturn_home_power(int return_home_power) {
        this.return_home_power = return_home_power;
    }

    public double getAttitude_head() {
        return attitude_head;
    }

    public void setAttitude_head(double attitude_head) {
        this.attitude_head = attitude_head;
    }

    public double getAttitude_pitch() {
        return attitude_pitch;
    }

    public void setAttitude_pitch(double attitude_pitch) {
        this.attitude_pitch = attitude_pitch;
    }

    public double getAttitude_roll() {
        return attitude_roll;
    }

    public void setAttitude_roll(double attitude_roll) {
        this.attitude_roll = attitude_roll;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }


    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getRtk_takeoff_altitude() {
        return rtk_takeoff_altitude;
    }

    public void setRtk_takeoff_altitude(double rtk_takeoff_altitude) {
        this.rtk_takeoff_altitude = rtk_takeoff_altitude;
    }

    public double getHomepoint_latitude() {
        return homepoint_latitude;
    }

    public void setHomepoint_latitude(double homepoint_latitude) {
        this.homepoint_latitude = homepoint_latitude;
    }

    public double getHomepoint_longitude() {
        return homepoint_longitude;
    }

    public void setHomepoint_longitude(double homepoint_longitude) {
        this.homepoint_longitude = homepoint_longitude;
    }

    public int getGear() {
        return gear;
    }

    public void setGear(int gear) {
        this.gear = gear;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public int getHeight_limit() {
        return height_limit;
    }

    public void setHeight_limit(int height_limit) {
        this.height_limit = height_limit;
    }

    public double getHome_distance() {
        return home_distance;
    }

    public void setHome_distance(double home_distance) {
        this.home_distance = home_distance;
    }

    public double getHorizontal_speed() {
        return horizontal_speed;
    }

    public void setHorizontal_speed(double horizontal_speed) {
        this.horizontal_speed = horizontal_speed;
    }

    public int getIs_near_area_limit() {
        return is_near_area_limit;
    }

    public void setIs_near_area_limit(int is_near_area_limit) {
        this.is_near_area_limit = is_near_area_limit;
    }

    public int getIs_near_height_limit() {
        return is_near_height_limit;
    }

    public void setIs_near_height_limit(int is_near_height_limit) {
        this.is_near_height_limit = is_near_height_limit;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getMode_code() {
        return mode_code;
    }

    public void setMode_code(int mode_code) {
        this.mode_code = mode_code;
    }

    public int getNight_lights_state() {
        return night_lights_state;
    }

    public void setNight_lights_state(int night_lights_state) {
        this.night_lights_state = night_lights_state;
    }

    public int getRc_lost_action() {
        return rc_lost_action;
    }

    public void setRc_lost_action(int rc_lost_action) {
        this.rc_lost_action = rc_lost_action;
    }

    public boolean isRid_state() {
        return rid_state;
    }

    public void setRid_state(boolean rid_state) {
        this.rid_state = rid_state;
    }

    public int getRth_altitude() {
        return rth_altitude;
    }

    public void setRth_altitude(int rth_altitude) {
        this.rth_altitude = rth_altitude;
    }

    public double getTotal_flight_distance() {
        return total_flight_distance;
    }

    public void setTotal_flight_distance(double total_flight_distance) {
        this.total_flight_distance = total_flight_distance;
    }

    public int getTotal_flight_sorties() {
        return total_flight_sorties;
    }

    public void setTotal_flight_sorties(int total_flight_sorties) {
        this.total_flight_sorties = total_flight_sorties;
    }

    public double getTotal_flight_time() {
        return total_flight_time;
    }

    public void setTotal_flight_time(double total_flight_time) {
        this.total_flight_time = total_flight_time;
    }

    public String getTrack_id() {
        return track_id;
    }

    public void setTrack_id(String track_id) {
        this.track_id = track_id;
    }

    public double getVertical_speed() {
        return vertical_speed;
    }

    public void setVertical_speed(double vertical_speed) {
        this.vertical_speed = vertical_speed;
    }

    public int getWind_direction() {
        return wind_direction;
    }

    public void setWind_direction(int wind_direction) {
        this.wind_direction = wind_direction;
    }

    public int getWind_speed() {
        return wind_speed;
    }

    public void setWind_speed(int wind_speed) {
        this.wind_speed = wind_speed;
    }

    public double getMeasure_target_altitude() {
        return measure_target_altitude;
    }

    public void setMeasure_target_altitude(double measure_target_altitude) {
        this.measure_target_altitude = measure_target_altitude;
    }

    public int getMeasure_target_distance() {
        return measure_target_distance;
    }

    public void setMeasure_target_distance(int measure_target_distance) {
        this.measure_target_distance = measure_target_distance;
    }

    public int getMeasure_target_error_state() {
        return measure_target_error_state;
    }

    public void setMeasure_target_error_state(int measure_target_error_state) {
        this.measure_target_error_state = measure_target_error_state;
    }

    public double getMeasure_target_latitude() {
        return measure_target_latitude;
    }

    public void setMeasure_target_latitude(double measure_target_latitude) {
        this.measure_target_latitude = measure_target_latitude;
    }

    public double getMeasure_target_longitude() {
        return measure_target_longitude;
    }

    public void setMeasure_target_longitude(double measure_target_longitude) {
        this.measure_target_longitude = measure_target_longitude;
    }

    public String getPayload_index() {
        return payload_index;
    }

    public void setPayload_index(String payload_index) {
        this.payload_index = payload_index;
    }

    public int getThermal_current_palette_style() {
        return thermal_current_palette_style;
    }

    public void setThermal_current_palette_style(int thermal_current_palette_style) {
        this.thermal_current_palette_style = thermal_current_palette_style;
    }

    public int getThermal_gain_mode() {
        return thermal_gain_mode;
    }

    public void setThermal_gain_mode(int thermal_gain_mode) {
        this.thermal_gain_mode = thermal_gain_mode;
    }

    public double getThermal_global_temperature_max() {
        return thermal_global_temperature_max;
    }

    public void setThermal_global_temperature_max(double thermal_global_temperature_max) {
        this.thermal_global_temperature_max = thermal_global_temperature_max;
    }

    public double getThermal_global_temperature_min() {
        return thermal_global_temperature_min;
    }

    public void setThermal_global_temperature_min(double thermal_global_temperature_min) {
        this.thermal_global_temperature_min = thermal_global_temperature_min;
    }

    public int getThermal_isotherm_lower_limit() {
        return thermal_isotherm_lower_limit;
    }

    public void setThermal_isotherm_lower_limit(int thermal_isotherm_lower_limit) {
        this.thermal_isotherm_lower_limit = thermal_isotherm_lower_limit;
    }

    public int getThermal_isotherm_state() {
        return thermal_isotherm_state;
    }

    public void setThermal_isotherm_state(int thermal_isotherm_state) {
        this.thermal_isotherm_state = thermal_isotherm_state;
    }

    public int getThermal_isotherm_upper_limit() {
        return thermal_isotherm_upper_limit;
    }

    public void setThermal_isotherm_upper_limit(int thermal_isotherm_upper_limit) {
        this.thermal_isotherm_upper_limit = thermal_isotherm_upper_limit;
    }

    public int getGimbal_pitch() {
        return gimbal_pitch;
    }

    public void setGimbal_pitch(int gimbal_pitch) {
        this.gimbal_pitch = gimbal_pitch;
    }

    public int getGimbal_roll() {
        return gimbal_roll;
    }

    public void setGimbal_roll(int gimbal_roll) {
        this.gimbal_roll = gimbal_roll;
    }

    public double getGimbal_yaw() {
        return gimbal_yaw;
    }

    public void setGimbal_yaw(double gimbal_yaw) {
        this.gimbal_yaw = gimbal_yaw;
    }



    public String getFirmware_version() {
        return firmware_version;
    }

    public void setFirmware_version(String firmware_version) {
        this.firmware_version = firmware_version;
    }

    public boolean isVirtualStickQuitMission() {
        return isVirtualStickQuitMission;
    }

    public void setVirtualStickQuitMission(boolean virtualStickQuitMission) {
        isVirtualStickQuitMission = virtualStickQuitMission;
    }

    public int getNavigationSatelliteSystem() {
        return navigationSatelliteSystem;
    }

    public void setNavigationSatelliteSystem(int navigationSatelliteSystem) {
        this.navigationSatelliteSystem = navigationSatelliteSystem;
    }

    public int getMissionType() {
        return missionType;
    }

    public void setMissionType(int missionType) {
        this.missionType = missionType;
    }

    public boolean isVirtualStickStatus() {
        return virtualStickStatus;
    }

    public void setVirtualStickStatus(boolean virtualStickStatus) {
        this.virtualStickStatus = virtualStickStatus;
    }

    public boolean isCancelVirtualStickStatus() {
        return cancelVirtualStickStatus;
    }

    public void setCancelVirtualStickStatus(boolean cancelVirtualStickStatus) {
        this.cancelVirtualStickStatus = cancelVirtualStickStatus;
    }

    public boolean isPauseMissionStautus() {
        return pauseMissionStautus;
    }

    public void setPauseMissionStautus(boolean pauseMissionStautus) {
        this.pauseMissionStautus = pauseMissionStautus;
    }

    public boolean isResumeMissionStatus() {
        return resumeMissionStatus;
    }

    public void setResumeMissionStatus(boolean resumeMissionStatus) {
        this.resumeMissionStatus = resumeMissionStatus;
    }

    public boolean isVtx() {
        return vtx;
    }

    public void setVtx(boolean vtx) {
        this.vtx = vtx;
    }

    public boolean isPropellerRotation() {
        return propellerRotation;
    }

    public void setPropellerRotation(boolean propellerRotation) {
        this.propellerRotation = propellerRotation;
    }

    public List<PayloadInfo> getPayloadInfos() {
        return payloadInfos;
    }

    public boolean isMotorsOn() {
        return isMotorsOn;
    }

    public void setMotorsOn(boolean motorsOn) {
        isMotorsOn = motorsOn;
    }

    public void setPayloadInfos(List<PayloadInfo> payloadInfos) {
        this.payloadInfos = payloadInfos;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getRemoteSecondBatteryPercent() {
        return remoteSecondBatteryPercent;
    }

    public void setRemoteSecondBatteryPercent(String remoteSecondBatteryPercent) {
        this.remoteSecondBatteryPercent = remoteSecondBatteryPercent;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getRemoteSerialNumber() {
        return remoteSerialNumber;
    }

    public void setRemoteSerialNumber(String remoteSerialNumber) {
        this.remoteSerialNumber = remoteSerialNumber;
    }

    public String getRemoteBatteryPercent() {
        return remoteBatteryPercent;
    }

    public void setRemoteBatteryPercent(String remoteBatteryPercent) {
        this.remoteBatteryPercent = remoteBatteryPercent;
    }

    public int getPhotoPanoramaProgress() {
        return photoPanoramaProgress;
    }

    public void setPhotoPanoramaProgress(int photoPanoramaProgress) {
        this.photoPanoramaProgress = photoPanoramaProgress;
    }

    public boolean isShootingPhotoPanorama() {
        return isShootingPhotoPanorama;
    }

    public void setShootingPhotoPanorama(boolean shootingPhotoPanorama) {
        isShootingPhotoPanorama = shootingPhotoPanorama;
    }

    public int getPhotoIntervalCount() {
        return photoIntervalCount;
    }

    public void setPhotoIntervalCount(int photoIntervalCount) {
        this.photoIntervalCount = photoIntervalCount;
    }

    public double getPhotoInterval() {
        return photoInterval;
    }

    public void setPhotoInterval(double photoInterval) {
        this.photoInterval = photoInterval;
    }

    public int getVirtualStickEnableReason() {
        return virtualStickEnableReason;
    }

    public void setVirtualStickEnableReason(int virtualStickEnableReason) {
        this.virtualStickEnableReason = virtualStickEnableReason;
    }

    public int getLowBatteryRTHState() {
        return lowBatteryRTHState;
    }

    public void setLowBatteryRTHState(int lowBatteryRTHState) {
        this.lowBatteryRTHState = lowBatteryRTHState;
    }


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }


    public int getLowBatteryRTHEnabled() {
        return lowBatteryRTHEnabled;
    }

    public void setLowBatteryRTHEnabled(int lowBatteryRTHEnabled) {
        this.lowBatteryRTHEnabled = lowBatteryRTHEnabled;
    }

    public int getSeriousLowBatteryWarningThreshold() {
        return seriousLowBatteryWarningThreshold;
    }

    public void setSeriousLowBatteryWarningThreshold(int seriousLowBatteryWarningThreshold) {
        this.seriousLowBatteryWarningThreshold = seriousLowBatteryWarningThreshold;
    }

    public int getLowBatteryWarningThreshold() {
        return lowBatteryWarningThreshold;
    }

    public void setLowBatteryWarningThreshold(int lowBatteryWarningThreshold) {
        this.lowBatteryWarningThreshold = lowBatteryWarningThreshold;
    }

    public int getDistanceLimitEnabled() {
        return distanceLimitEnabled;
    }

    public void setDistanceLimitEnabled(int distanceLimitEnabled) {
        this.distanceLimitEnabled = distanceLimitEnabled;
    }

    public int getDistanceLimit() {
        return distanceLimit;
    }

    public void setDistanceLimit(int distanceLimit) {
        this.distanceLimit = distanceLimit;
    }

    public int getHeightLimit() {
        return heightLimit;
    }

    public void setHeightLimit(int heightLimit) {
        this.heightLimit = heightLimit;
    }

    public int getFailsafeAction() {
        return failsafeAction;
    }

    public void setFailsafeAction(int failsafeAction) {
        this.failsafeAction = failsafeAction;
    }

    public int getGoHomeHeight() {
        return goHomeHeight;
    }

    public void setGoHomeHeight(int goHomeHeight) {
        this.goHomeHeight = goHomeHeight;
    }


    public String getLTELinkType() {
        return LTELinkType;
    }

    public void setLTELinkType(String LTELinkType) {
        this.LTELinkType = LTELinkType;
    }

    public int getIsVirtualStickAdvancedModeEnabled() {
        return isVirtualStickAdvancedModeEnabled;
    }

    public void setIsVirtualStickAdvancedModeEnabled(int isVirtualStickAdvancedModeEnabled) {
        this.isVirtualStickAdvancedModeEnabled = isVirtualStickAdvancedModeEnabled;
    }

    public String getSpotMetersureTemperature() {
        return spotMetersureTemperature;
    }

    public void setSpotMetersureTemperature(String spotMetersureTemperature) {
        this.spotMetersureTemperature = spotMetersureTemperature;
    }

    public String getAverageAreaTemperature() {
        return averageAreaTemperature;
    }

    public void setAverageAreaTemperature(String averageAreaTemperature) {
        this.averageAreaTemperature = averageAreaTemperature;
    }

    public String getMinAreaTemperature() {
        return minAreaTemperature;
    }

    public void setMinAreaTemperature(String minAreaTemperature) {
        this.minAreaTemperature = minAreaTemperature;
    }

    public String getMaxAreaTemperature() {
        return maxAreaTemperature;
    }

    public void setMaxAreaTemperature(String maxAreaTemperature) {
        this.maxAreaTemperature = maxAreaTemperature;
    }

    public String getMaxTemperaturePointX() {
        return maxTemperaturePointX;
    }

    public void setMaxTemperaturePointX(String maxTemperaturePointX) {
        this.maxTemperaturePointX = maxTemperaturePointX;
    }

    public String getMaxTemperaturePointY() {
        return maxTemperaturePointY;
    }

    public void setMaxTemperaturePointY(String maxTemperaturePointY) {
        this.maxTemperaturePointY = maxTemperaturePointY;
    }

    public String getMinTemperaturePointX() {
        return minTemperaturePointX;
    }

    public void setMinTemperaturePointX(String minTemperaturePointX) {
        this.minTemperaturePointX = minTemperaturePointX;
    }

    public String getMinTemperaturePointY() {
        return minTemperaturePointY;
    }

    public void setMinTemperaturePointY(String minTemperaturePointY) {
        this.minTemperaturePointY = minTemperaturePointY;
    }

    public int getThermalTemperatureMeasureMode() {
        return thermalTemperatureMeasureMode;
    }

    public void setThermalTemperatureMeasureMode(int thermalTemperatureMeasureMode) {
        this.thermalTemperatureMeasureMode = thermalTemperatureMeasureMode;
    }


    public String getAlternatePointLat() {
        return alternatePointLat;
    }

    public void setAlternatePointLat(String alternatePointLat) {
        this.alternatePointLat = alternatePointLat;
    }

    public String getAlternatePointLon() {
        return alternatePointLon;
    }

    public void setAlternatePointLon(String alternatePointLon) {
        this.alternatePointLon = alternatePointLon;
    }


    public int getFlightControlAuthority() {
        return flightControlAuthority;
    }

    public void setFlightControlAuthority(int flightControlAuthority) {
        this.flightControlAuthority = flightControlAuthority;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public void setCurrentWaypointIndex(int currentWaypointIndex) {
        this.currentWaypointIndex = currentWaypointIndex;
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public double getBatteryTemperatureA() {
        return batteryTemperatureA;
    }

    public void setBatteryTemperatureA(double batteryTemperatureA) {
        this.batteryTemperatureA = batteryTemperatureA;
    }

    public double getBatteryTemperatureB() {
        return batteryTemperatureB;
    }

    public void setBatteryTemperatureB(double batteryTemperatureB) {
        this.batteryTemperatureB = batteryTemperatureB;
    }

    public String getAircraftTotalFlightDistance() {
        return aircraftTotalFlightDistance;
    }

    public void setAircraftTotalFlightDistance(String aircraftTotalFlightDistance) {
        this.aircraftTotalFlightDistance = aircraftTotalFlightDistance;
    }

    public String getAircraftTotalFlightTimes() {
        return aircraftTotalFlightTimes;
    }

    public void setAircraftTotalFlightTimes(String aircraftTotalFlightTimes) {
        this.aircraftTotalFlightTimes = aircraftTotalFlightTimes;
    }

    public String getAircraftTotalFlightDuration() {
        return aircraftTotalFlightDuration;
    }

    public void setAircraftTotalFlightDuration(String aircraftTotalFlightDuration) {
        this.aircraftTotalFlightDuration = aircraftTotalFlightDuration;
    }



    public boolean isPlaneWing() {
        return planeWing;
    }

    public void setPlaneWing(boolean planeWing) {
        this.planeWing = planeWing;
    }


    public int getReturnHomePower() {
        return returnHomePower;
    }

    public void setReturnHomePower(int returnHomePower) {
        this.returnHomePower = returnHomePower;
    }

    public int getLandingPower() {
        return landingPower;
    }

    public void setLandingPower(int landingPower) {
        this.landingPower = landingPower;
    }

    public int getIsVirtualStickEnable() {
        return isVirtualStickEnable;
    }

    public void setIsVirtualStickEnable(int isVirtualStickEnable) {
        this.isVirtualStickEnable = isVirtualStickEnable;
    }

    public int getGoHomeState() {
        return goHomeState;
    }

    public void setGoHomeState(int goHomeState) {
        this.goHomeState = goHomeState;
    }

    public String getWaylineExecutingInterruptReason() {
        return waylineExecutingInterruptReason;
    }

    public void setWaylineExecutingInterruptReason(String waylineExecutingInterruptReason) {
        this.waylineExecutingInterruptReason = waylineExecutingInterruptReason;
    }

    public String getWaypointMissionExecuteState() {
        return waypointMissionExecuteState;
    }

    public void setWaypointMissionExecuteState(String waypointMissionExecuteState) {
        this.waypointMissionExecuteState = waypointMissionExecuteState;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public int getThermalDisplayMode() {
        return thermalDisplayMode;
    }

    public void setThermalDisplayMode(int thermalDisplayMode) {
        this.thermalDisplayMode = thermalDisplayMode;
    }

    public boolean isDistanceLimitEnabled() {
        return isDistanceLimitEnabled;
    }

    public void setDistanceLimitEnabled(boolean distanceLimitEnabled) {
        isDistanceLimitEnabled = distanceLimitEnabled;
    }


    private boolean thermalContinuous;
    private int[] thermalGears;

    public boolean isThermalContinuous() {
        return thermalContinuous;
    }

    public void setThermalContinuous(boolean thermalContinuous) {
        this.thermalContinuous = thermalContinuous;
    }

    public int[] getThermalGears() {
        return thermalGears;
    }

    public void setThermalGears(int[] thermalGears) {
        this.thermalGears = thermalGears;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public int[] getGears() {
        return gears;
    }

    public void setGears(int[] gears) {
        this.gears = gears;
    }

    public double getCameraZoomRatios() {
        return cameraZoomRatios;
    }

    public void setCameraZoomRatios(double cameraZoomRatios) {
        this.cameraZoomRatios = cameraZoomRatios;
    }

    public double getThermalZoomRatios() {
        return thermalZoomRatios;
    }

    public void setThermalZoomRatios(double thermalZoomRatios) {
        this.thermalZoomRatios = thermalZoomRatios;
    }

    public int getIsShootingPhoto() {
        return isShootingPhoto;
    }

    public void setIsShootingPhoto(int isShootingPhoto) {
        this.isShootingPhoto = isShootingPhoto;
    }

    public int getIsRecording() {
        return isRecording;
    }

    public void setIsRecording(int isRecording) {
        this.isRecording = isRecording;
    }

    public int getRecordingTime() {
        return recordingTime;
    }

    public void setRecordingTime(int recordingTime) {
        this.recordingTime = recordingTime;
    }


    public int getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(int msg_type) {
        this.msg_type = msg_type;
    }

    public int getUltrasonicHeight() {
        return ultrasonicHeight;
    }

    public void setUltrasonicHeight(int ultrasonicHeight) {
        this.ultrasonicHeight = ultrasonicHeight;
    }

    public int getCameraVideoStreamSource() {
        return cameraVideoStreamSource;
    }

    public void setCameraVideoStreamSource(int cameraVideoStreamSource) {
        this.cameraVideoStreamSource = cameraVideoStreamSource;
    }



    public String getFlightPathName() {
        return flightPathName;
    }

    public void setFlightPathName(String flightPathName) {
        this.flightPathName = flightPathName;
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int cameraMode) {
        this.cameraMode = cameraMode;
    }

    public int getAngleYaw() {
        return angleYaw;
    }

    public void setAngleYaw(int angleYaw) {
        this.angleYaw = angleYaw;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public int getLiveStatus() {
        return liveStatus;
    }

    public void setLiveStatus(int liveStatus) {
        this.liveStatus = liveStatus;
    }

    private int liveStatus;//推流状态 0失败  1成功


    public int getRemoteControlSignal() {
        return remoteControlSignal;
    }

    public void setRemoteControlSignal(int remoteControlSignal) {
        this.remoteControlSignal = remoteControlSignal;
    }

    public int getPictureBiographySignal() {
        return pictureBiographySignal;
    }

    public void setPictureBiographySignal(int pictureBiographySignal) {
        this.pictureBiographySignal = pictureBiographySignal;
    }

    public int getElectricityInfoB() {
        return electricityInfoB;
    }

    public void setElectricityInfoB(int electricityInfoB) {
        this.electricityInfoB = electricityInfoB;
    }

    public int getCurrentView() {
        return currentView;
    }

    public void setCurrentView(int currentView) {
        this.currentView = currentView;
    }


    public String getPlaneMessage() {
        return planeMessage;
    }

    public void setPlaneMessage(String planeMessage) {
        this.planeMessage = planeMessage;
    }


}
