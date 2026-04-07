package com.jby.apron.entity;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Osd {


    private static class OsdHolder {
        private static final Osd INSTANCE = new Osd();
    }

    private Osd() {
    }

    public static final Osd getInstance() {
        return OsdHolder.INSTANCE;
    }

    private String bid;
    private Data data;
    private String tid;
    private String method;
    private long timestamp;
    private String gateway;

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static class Data {
        @SerializedName("53-0-0")
        private Data._$5300 _$5300;
        private int activation_time;
        private double attitude_head;
        private double attitude_pitch;
        private double attitude_roll;
        private Battery battery;
        private List<Cameras> cameras;
        private String country;
        private DistanceLimitStatus distance_limit_status;
        private double elevation;
        private double rtk_takeoff_altitude;
        private double homepoint_latitude;
        private double homepoint_longitude;
        private String firmware_version;
        private String product_name;//产品类型 例:M350_RTK
        private String serial_number;//飞控序列号
        private int gear;
        private double height;
        private int height_limit;
        private double home_distance;
        private double horizontal_speed;
        private int is_near_area_limit;
        private int is_near_height_limit;
        private double latitude;
        private double longitude;
        private MaintainStatus maintain_status;
        private int mode_code;
        private int night_lights_state;
        private ObstacleAvoidance obstacle_avoidance;
        private PositionState position_state;
        private int rc_lost_action;
        private boolean rid_state;
        private int rth_altitude;
        private Storage storage;
        private double total_flight_distance;
        private int total_flight_sorties;
        private double total_flight_time;
        private String track_id;
        private double vertical_speed;
        private int wind_direction;
        private int wind_speed;

        public String getProduct_name() {
            return product_name;
        }

        public void setProduct_name(String product_name) {
            this.product_name = product_name;
        }

        public String getSerial_number() {
            return serial_number;
        }

        public void setSerial_number(String serial_number) {
            this.serial_number = serial_number;
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

        public _$5300 get_$5300() {
            return _$5300;
        }

        public void set_$5300(_$5300 _$5300) {
            this._$5300 = _$5300;
        }

        public int getActivation_time() {
            return activation_time;
        }

        public void setActivation_time(int activation_time) {
            this.activation_time = activation_time;
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

        public Battery getBattery() {
            return battery;
        }

        public void setBattery(Battery battery) {
            this.battery = battery;
        }

        public List<Cameras> getCameras() {
            return cameras;
        }

        public void setCameras(List<Cameras> cameras) {
            this.cameras = cameras;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public DistanceLimitStatus getDistance_limit_status() {
            return distance_limit_status;
        }

        public void setDistance_limit_status(DistanceLimitStatus distance_limit_status) {
            this.distance_limit_status = distance_limit_status;
        }

        public double getElevation() {
            return elevation;
        }

        public void setElevation(double elevation) {
            this.elevation = elevation;
        }

        public String getFirmware_version() {
            return firmware_version;
        }

        public void setFirmware_version(String firmware_version) {
            this.firmware_version = firmware_version;
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

        public MaintainStatus getMaintain_status() {
            return maintain_status;
        }

        public void setMaintain_status(MaintainStatus maintain_status) {
            this.maintain_status = maintain_status;
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

        public ObstacleAvoidance getObstacle_avoidance() {
            return obstacle_avoidance;
        }

        public void setObstacle_avoidance(ObstacleAvoidance obstacle_avoidance) {
            this.obstacle_avoidance = obstacle_avoidance;
        }

        public PositionState getPosition_state() {
            return position_state;
        }

        public void setPosition_state(PositionState position_state) {
            this.position_state = position_state;
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

        public Storage getStorage() {
            return storage;
        }

        public void setStorage(Storage storage) {
            this.storage = storage;
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

        public static class _$5300 {
            private int gimbal_pitch;
            private int gimbal_roll;
            private double gimbal_yaw;
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
            private int version;


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

            public int getVersion() {
                return version;
            }

            public void setVersion(int version) {
                this.version = version;
            }
        }

        public static class Battery {
            private List<Batteries> batteries;
            private int capacity_percent;
            private int landing_power;
            private int remain_flight_time;
            private int return_home_power;

            public List<Batteries> getBatteries() {
                return batteries;
            }

            public void setBatteries(List<Batteries> batteries) {
                this.batteries = batteries;
            }

            public int getCapacity_percent() {
                return capacity_percent;
            }

            public void setCapacity_percent(int capacity_percent) {
                this.capacity_percent = capacity_percent;
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

            public static class Batteries {
                private int capacity_percent;
                private String firmware_version;
                private int high_voltage_storage_days;
                private int index;
                private int loop_times;
                private String sn;
                private int sub_type;
                private double temperature;
                private int type;
                private int voltage;

                public int getCapacity_percent() {
                    return capacity_percent;
                }

                public void setCapacity_percent(int capacity_percent) {
                    this.capacity_percent = capacity_percent;
                }

                public String getFirmware_version() {
                    return firmware_version;
                }

                public void setFirmware_version(String firmware_version) {
                    this.firmware_version = firmware_version;
                }

                public int getHigh_voltage_storage_days() {
                    return high_voltage_storage_days;
                }

                public void setHigh_voltage_storage_days(int high_voltage_storage_days) {
                    this.high_voltage_storage_days = high_voltage_storage_days;
                }

                public int getIndex() {
                    return index;
                }

                public void setIndex(int index) {
                    this.index = index;
                }

                public int getLoop_times() {
                    return loop_times;
                }

                public void setLoop_times(int loop_times) {
                    this.loop_times = loop_times;
                }

                public String getSn() {
                    return sn;
                }

                public void setSn(String sn) {
                    this.sn = sn;
                }

                public int getSub_type() {
                    return sub_type;
                }

                public void setSub_type(int sub_type) {
                    this.sub_type = sub_type;
                }

                public double getTemperature() {
                    return temperature;
                }

                public void setTemperature(double temperature) {
                    this.temperature = temperature;
                }

                public int getType() {
                    return type;
                }

                public void setType(int type) {
                    this.type = type;
                }

                public int getVoltage() {
                    return voltage;
                }

                public void setVoltage(int voltage) {
                    this.voltage = voltage;
                }
            }
        }

        public static class DistanceLimitStatus {
            private int distance_limit;
            private int is_near_distance_limit;
            private int state;

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
        }

        public static class MaintainStatus {
            private List<MaintainStatusArray> maintain_status_array;

            public List<MaintainStatusArray> getMaintain_status_array() {
                return maintain_status_array;
            }

            public void setMaintain_status_array(List<MaintainStatusArray> maintain_status_array) {
                this.maintain_status_array = maintain_status_array;
            }

            public static class MaintainStatusArray {
                private int last_maintain_flight_sorties;
                private int last_maintain_flight_time;
                private int last_maintain_time;
                private int last_maintain_type;
                private int state;

                public int getLast_maintain_flight_sorties() {
                    return last_maintain_flight_sorties;
                }

                public void setLast_maintain_flight_sorties(int last_maintain_flight_sorties) {
                    this.last_maintain_flight_sorties = last_maintain_flight_sorties;
                }

                public int getLast_maintain_flight_time() {
                    return last_maintain_flight_time;
                }

                public void setLast_maintain_flight_time(int last_maintain_flight_time) {
                    this.last_maintain_flight_time = last_maintain_flight_time;
                }

                public int getLast_maintain_time() {
                    return last_maintain_time;
                }

                public void setLast_maintain_time(int last_maintain_time) {
                    this.last_maintain_time = last_maintain_time;
                }

                public int getLast_maintain_type() {
                    return last_maintain_type;
                }

                public void setLast_maintain_type(int last_maintain_type) {
                    this.last_maintain_type = last_maintain_type;
                }

                public int getState() {
                    return state;
                }

                public void setState(int state) {
                    this.state = state;
                }
            }
        }

        public static class ObstacleAvoidance {
            private int downside;
            private int horizon;
            private int upside;

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
        }

        public static class PositionState {
            private int gps_number;
            private int is_fixed;
            private int quality;
            private int rtk_number;

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
        }

        public static class Storage {
            private int total;
            private int used;

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
        }

        public static class Cameras {
            private int camera_mode;
            private int ir_metering_mode;
            private IrMeteringPoint ir_metering_point;
            private int ir_zoom_factor;
            private LiveviewWorldRegion liveview_world_region;
            private String payload_index;
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

            public IrMeteringPoint getIr_metering_point() {
                return ir_metering_point;
            }

            public void setIr_metering_point(IrMeteringPoint ir_metering_point) {
                this.ir_metering_point = ir_metering_point;
            }

            public int getIr_zoom_factor() {
                return ir_zoom_factor;
            }

            public void setIr_zoom_factor(int ir_zoom_factor) {
                this.ir_zoom_factor = ir_zoom_factor;
            }

            public LiveviewWorldRegion getLiveview_world_region() {
                return liveview_world_region;
            }

            public void setLiveview_world_region(LiveviewWorldRegion liveview_world_region) {
                this.liveview_world_region = liveview_world_region;
            }

            public String getPayload_index() {
                return payload_index;
            }

            public void setPayload_index(String payload_index) {
                this.payload_index = payload_index;
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

            public static class IrMeteringPoint {
                private int temperature;
                private double x;
                private double y;

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
            }

            public static class LiveviewWorldRegion {
                private double bottom;
                private double left;
                private double right;
                private double top;

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
            }
        }
    }
}
