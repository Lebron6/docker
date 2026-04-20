package com.jby.apron.entity;

import java.util.List;

public class MessageDown {

    private String tid;
    private String bid;
    private long timestamp;
    private String method;
    private Data data;

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private int result;
        private String url;
        private int video_quality;
        private int ideo_quality;
        private AlternateLandPoint alternate_land_point;
        private int rth_mode;
        private int task_type;
        private int wayline_precision_type;
        private String video_type;
        private long execute_time;
        private int exit_wayline_when_rc_lost;
        private File file;
        private String flight_id;
        private int flight_safety_advance_check;
        private int out_of_control_action;
        private boolean takeoffToPointTask;
        private BreakPoint break_point;
        private int camera_mode;
        private String h;
        private String seq;
        private String w;
        //摇杆和云台
        private String x;
        private String y;

        private String camera_type;
        private String payload_index;

        private int height;
        private double latitude;
        private boolean locked;
        private double longitude;
        private int zoom_factor;
        private boolean enable;
        private int exposure_mode;
        private int exposure_value;
        private int focus_mode;
        private int focus_value;

        private int commander_flight_height;
        private int commander_mode_lost_action;
        private String max_speed;
        private int rc_lost_action;
        private int rth_altitude;
        private String security_takeoff_height;
        private int target_height;
        private double target_latitude;
        private double target_longitude;
        private String fly_to_id;
        private List<Points> points;
        private int reset_mode;
        private int psdk_index;
        private int play_mode;
        private int play_volume;
        private int volume;
        private int type;
        private int language;
        private int speed;


        private ExecutableConditions executable_conditions;
        private ReadyConditions ready_conditions;
        private SimulateMission simulate_mission;

        public ExecutableConditions getExecutable_conditions() {
            return executable_conditions;
        }

        public void setExecutable_conditions(ExecutableConditions executable_conditions) {
            this.executable_conditions = executable_conditions;
        }

        public ReadyConditions getReady_conditions() {
            return ready_conditions;
        }

        public void setReady_conditions(ReadyConditions ready_conditions) {
            this.ready_conditions = ready_conditions;
        }

        public SimulateMission getSimulate_mission() {
            return simulate_mission;
        }

        public void setSimulate_mission(SimulateMission simulate_mission) {
            this.simulate_mission = simulate_mission;
        }

        public int getVolume() {
            return volume;
        }

        public void setVolume(int volume) {
            this.volume = volume;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getLanguage() {
            return language;
        }

        public void setLanguage(int language) {
            this.language = language;
        }

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public int getPlay_volume() {
            return play_volume;
        }

        public void setPlay_volume(int play_volume) {
            this.play_volume = play_volume;
        }

        public int getPlay_mode() {
            return play_mode;
        }

        public void setPlay_mode(int play_mode) {
            this.play_mode = play_mode;
        }

        private Tts tts;

        public int getPsdk_index() {
            return psdk_index;
        }

        public void setPsdk_index(int psdk_index) {
            this.psdk_index = psdk_index;
        }

        public Tts getTts() {
            return tts;
        }

        public void setTts(Tts tts) {
            this.tts = tts;
        }

        public static class Tts {
            private String md5;
            private String name;
            private String text;

            public String getMd5() {
                return md5;
            }

            public void setMd5(String md5) {
                this.md5 = md5;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }


        public int getReset_mode() {
            return reset_mode;
        }

        public void setReset_mode(int reset_mode) {
            this.reset_mode = reset_mode;
        }

        public String getFly_to_id() {
            return fly_to_id;
        }

        public void setFly_to_id(String fly_to_id) {
            this.fly_to_id = fly_to_id;
        }

        public List<Points> getPoints() {
            return points;
        }

        public void setPoints(List<Points> points) {
            this.points = points;
        }

        public static class Points {
            private int height;
            private double latitude;
            private double longitude;

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
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
        }
        public int getCommander_flight_height() {
            return commander_flight_height;
        }

        public void setCommander_flight_height(int commander_flight_height) {
            this.commander_flight_height = commander_flight_height;
        }

        public int getCommander_mode_lost_action() {
            return commander_mode_lost_action;
        }

        public void setCommander_mode_lost_action(int commander_mode_lost_action) {
            this.commander_mode_lost_action = commander_mode_lost_action;
        }

        public String getMax_speed() {
            return max_speed;
        }

        public void setMax_speed(String max_speed) {
            this.max_speed = max_speed;
        }

        public int getRc_lost_action() {
            return rc_lost_action;
        }

        public void setRc_lost_action(int rc_lost_action) {
            this.rc_lost_action = rc_lost_action;
        }

        public String getSecurity_takeoff_height() {
            return security_takeoff_height;
        }

        public void setSecurity_takeoff_height(String security_takeoff_height) {
            this.security_takeoff_height = security_takeoff_height;
        }

        public int getTarget_height() {
            return target_height;
        }

        public void setTarget_height(int target_height) {
            this.target_height = target_height;
        }

        public double getTarget_latitude() {
            return target_latitude;
        }

        public void setTarget_latitude(double target_latitude) {
            this.target_latitude = target_latitude;
        }

        public double getTarget_longitude() {
            return target_longitude;
        }

        public void setTarget_longitude(double target_longitude) {
            this.target_longitude = target_longitude;
        }

        public int getFocus_value() {
            return focus_value;
        }

        public void setFocus_value(int focus_value) {
            this.focus_value = focus_value;
        }

        public int getFocus_mode() {
            return focus_mode;
        }

        public void setFocus_mode(int focus_mode) {
            this.focus_mode = focus_mode;
        }

        public int getExposure_value() {
            return exposure_value;
        }

        public void setExposure_value(int exposure_value) {
            this.exposure_value = exposure_value;
        }

        public int getExposure_mode() {
            return exposure_mode;
        }

        public void setExposure_mode(int exposure_mode) {
            this.exposure_mode = exposure_mode;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public int getZoom_factor() {
            return zoom_factor;
        }

        public void setZoom_factor(int zoom_factor) {
            this.zoom_factor = zoom_factor;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
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

        public String getCamera_type() {
            return camera_type;
        }

        public void setCamera_type(String camera_type) {
            this.camera_type = camera_type;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public String getPayload_index() {
            return payload_index;
        }

        public void setPayload_index(String payload_index) {
            this.payload_index = payload_index;
        }

        public String getH() {
            return h;
        }

        public void setH(String h) {
            this.h = h;
        }

        public String getSeq() {
            return seq;
        }

        public void setSeq(String seq) {
            this.seq = seq;
        }

        public String getW() {
            return w;
        }

        public void setW(String w) {
            this.w = w;
        }

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getY() {
            return y;
        }

        public void setY(String y) {
            this.y = y;
        }

        public int getCamera_mode() {
            return camera_mode;
        }

        public void setCamera_mode(int camera_mode) {
            this.camera_mode = camera_mode;
        }

        public BreakPoint getBreak_point() {
            return break_point;
        }

        public void setBreak_point(BreakPoint break_point) {
            this.break_point = break_point;
        }
        public int getIdeo_quality() {
            return ideo_quality;
        }

        public void setIdeo_quality(int ideo_quality) {
            this.ideo_quality = ideo_quality;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getVideo_quality() {
            return video_quality;
        }

        public void setVideo_quality(int video_quality) {
            this.video_quality = video_quality;
        }

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }

        public String getVideo_type() {
            return video_type;
        }

        public void setVideo_type(String video_type) {
            this.video_type = video_type;
        }


        public static class File {
            private String fingerprint;
            private String url;
            private String format;
            private String md5;
            private String name;

            public String getFormat() {
                return format;
            }

            public void setFormat(String format) {
                this.format = format;
            }

            public String getMd5() {
                return md5;
            }

            public void setMd5(String md5) {
                this.md5 = md5;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getFingerprint() {
                return fingerprint;
            }

            public void setFingerprint(String fingerprint) {
                this.fingerprint = fingerprint;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }


        public static class BreakPoint {
            private int index;
            private double progress;
            private int state;
            private int wayline_id;

            public int getIndex() {
                return index;
            }

            public void setIndex(int index) {
                this.index = index;
            }

            public double getProgress() {
                return progress;
            }

            public void setProgress(double progress) {
                this.progress = progress;
            }

            public int getState() {
                return state;
            }

            public void setState(int state) {
                this.state = state;
            }

            public int getWayline_id() {
                return wayline_id;
            }

            public void setWayline_id(int wayline_id) {
                this.wayline_id = wayline_id;
            }
        }

        public AlternateLandPoint getAlternate_land_point() {
            return alternate_land_point;
        }

        public void setAlternate_land_point(AlternateLandPoint alternate_land_point) {
            this.alternate_land_point = alternate_land_point;
        }

        public long getExecute_time() {
            return execute_time;
        }

        public void setExecute_time(long execute_time) {
            this.execute_time = execute_time;
        }

        public int getExit_wayline_when_rc_lost() {
            return exit_wayline_when_rc_lost;
        }

        public void setExit_wayline_when_rc_lost(int exit_wayline_when_rc_lost) {
            this.exit_wayline_when_rc_lost = exit_wayline_when_rc_lost;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getFlight_id() {
            return flight_id;
        }

        public void setFlight_id(String flight_id) {
            this.flight_id = flight_id;
        }

        public int getFlight_safety_advance_check() {
            return flight_safety_advance_check;
        }

        public void setFlight_safety_advance_check(int flight_safety_advance_check) {
            this.flight_safety_advance_check = flight_safety_advance_check;
        }

        public int getOut_of_control_action() {
            return out_of_control_action;
        }

        public void setOut_of_control_action(int out_of_control_action) {
            this.out_of_control_action = out_of_control_action;
        }

        public int getRth_altitude() {
            return rth_altitude;
        }

        public void setRth_altitude(int rth_altitude) {
            this.rth_altitude = rth_altitude;
        }

        public int getRth_mode() {
            return rth_mode;
        }

        public void setRth_mode(int rth_mode) {
            this.rth_mode = rth_mode;
        }

        public boolean isTakeoffToPointTask() {
            return takeoffToPointTask;
        }

        public void setTakeoffToPointTask(boolean takeoffToPointTask) {
            this.takeoffToPointTask = takeoffToPointTask;
        }

        public int getTask_type() {
            return task_type;
        }

        public void setTask_type(int task_type) {
            this.task_type = task_type;
        }

        public int getWayline_precision_type() {
            return wayline_precision_type;
        }

        public void setWayline_precision_type(int wayline_precision_type) {
            this.wayline_precision_type = wayline_precision_type;
        }

        public static class AlternateLandPoint {
            private double latitude;
            private double longitude;
            private double safe_land_height;

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

            public double getSafe_land_height() {
                return safe_land_height;
            }

            public void setSafe_land_height(double safe_land_height) {
                this.safe_land_height = safe_land_height;
            }
        }
        public static class ExecutableConditions {
            private int storage_capacity;

            public int getStorage_capacity() {
                return storage_capacity;
            }

            public void setStorage_capacity(int storage_capacity) {
                this.storage_capacity = storage_capacity;
            }
        }

        public static class ReadyConditions {
            private int battery_capacity;
            private long begin_time;
            private long end_time;

            public int getBattery_capacity() {
                return battery_capacity;
            }

            public void setBattery_capacity(int battery_capacity) {
                this.battery_capacity = battery_capacity;
            }

            public long getBegin_time() {
                return begin_time;
            }

            public void setBegin_time(long begin_time) {
                this.begin_time = begin_time;
            }

            public long getEnd_time() {
                return end_time;
            }

            public void setEnd_time(long end_time) {
                this.end_time = end_time;
            }
        }

        public static class SimulateMission {
            private int is_enable;
            private double latitude;
            private double longitude;
            private double altitude;

            public int getIs_enable() {
                return is_enable;
            }

            public void setIs_enable(int is_enable) {
                this.is_enable = is_enable;
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

            public double getAltitude() {
                return altitude;
            }

            public void setAltitude(double altitude) {
                this.altitude = altitude;
            }
        }
        private ObstacleAvoidance obstacle_avoidance;
        private int low_battery_warning_threshold;
        private int serious_low_battery_warning_threshold;
        private String fail_safe_action;
        private Rth rth;
        private int height_limit;
        private int distance_limit;
        private int night_lights_state;
        private boolean measure_enable;

        public ObstacleAvoidance getObstacle_avoidance() {
            return obstacle_avoidance;
        }

        public void setObstacle_avoidance(ObstacleAvoidance obstacle_avoidance) {
            this.obstacle_avoidance = obstacle_avoidance;
        }

        public int getLow_battery_warning_threshold() {
            return low_battery_warning_threshold;
        }

        public void setLow_battery_warning_threshold(int low_battery_warning_threshold) {
            this.low_battery_warning_threshold = low_battery_warning_threshold;
        }

        public int getSerious_low_battery_warning_threshold() {
            return serious_low_battery_warning_threshold;
        }

        public void setSerious_low_battery_warning_threshold(int serious_low_battery_warning_threshold) {
            this.serious_low_battery_warning_threshold = serious_low_battery_warning_threshold;
        }

        public String getFail_safe_action() {
            return fail_safe_action;
        }

        public void setFail_safe_action(String fail_safe_action) {
            this.fail_safe_action = fail_safe_action;
        }

        public Rth getRth() {
            return rth;
        }

        public void setRth(Rth rth) {
            this.rth = rth;
        }

        public int getHeight_limit() {
            return height_limit;
        }

        public void setHeight_limit(int height_limit) {
            this.height_limit = height_limit;
        }

        public int getDistance_limit() {
            return distance_limit;
        }

        public void setDistance_limit(int distance_limit) {
            this.distance_limit = distance_limit;
        }

        public int getNight_lights_state() {
            return night_lights_state;
        }

        public void setNight_lights_state(int night_lights_state) {
            this.night_lights_state = night_lights_state;
        }

        public boolean isMeasure_enable() {
            return measure_enable;
        }

        public void setMeasure_enable(boolean measure_enable) {
            this.measure_enable = measure_enable;
        }

        public static class ObstacleAvoidance {
            private boolean enable;
            private Horizon horizon;
            private Horizon upside;
            private Horizon downside;

            public boolean isEnable() {
                return enable;
            }

            public void setEnable(boolean enable) {
                this.enable = enable;
            }

            public Horizon getHorizon() {
                return horizon;
            }

            public void setHorizon(Horizon horizon) {
                this.horizon = horizon;
            }

            public Horizon getUpside() {
                return upside;
            }

            public void setUpside(Horizon upside) {
                this.upside = upside;
            }

            public Horizon getDownside() {
                return downside;
            }

            public void setDownside(Horizon downside) {
                this.downside = downside;
            }

            public static class Horizon {
                private boolean enable;
                private int warning_distance;
                private int braking_distance;

                public boolean isEnable() {
                    return enable;
                }

                public void setEnable(boolean enable) {
                    this.enable = enable;
                }

                public int getWarning_distance() {
                    return warning_distance;
                }

                public void setWarning_distance(int warning_distance) {
                    this.warning_distance = warning_distance;
                }

                public int getBraking_distance() {
                    return braking_distance;
                }

                public void setBraking_distance(int braking_distance) {
                    this.braking_distance = braking_distance;
                }
            }
        }

        public static class Rth {
            private int rth_mode;
            private int rth_altitude;

            public int getRth_mode() {
                return rth_mode;
            }

            public void setRth_mode(int rth_mode) {
                this.rth_mode = rth_mode;
            }

            public int getRth_altitude() {
                return rth_altitude;
            }

            public void setRth_altitude(int rth_altitude) {
                this.rth_altitude = rth_altitude;
            }
        }
    }
}