package com.jby.apron.entity;

public class MessageDown {
    private String bid;
    private String tid;
    private long timestamp;
    private String method;
    private Data data;

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
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
        private String flight_id;
        private Data.File file;
        private Data.BreakPoint break_point;
        private Data.ExecutableConditions executable_conditions;
        private long execute_time;
        private int exit_wayline_when_rc_lost;
        private int flight_safety_advance_check;
        private int out_of_control_action;
        private Data.ReadyConditions ready_conditions;
        private int rth_altitude;
        private Data.SimulateMission simulate_mission;
        private int task_type;
        private int wayline_precision_type;

        public String getFlight_id() {
            return flight_id;
        }

        public void setFlight_id(String flight_id) {
            this.flight_id = flight_id;
        }

        public Data.File getFile() {
            return file;
        }

        public void setFile(Data.File file) {
            this.file = file;
        }

        public Data.BreakPoint getBreak_point() {
            return break_point;
        }

        public void setBreak_point(Data.BreakPoint break_point) {
            this.break_point = break_point;
        }

        public Data.ExecutableConditions getExecutable_conditions() {
            return executable_conditions;
        }

        public void setExecutable_conditions(Data.ExecutableConditions executable_conditions) {
            this.executable_conditions = executable_conditions;
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

        public Data.ReadyConditions getReady_conditions() {
            return ready_conditions;
        }

        public void setReady_conditions(Data.ReadyConditions ready_conditions) {
            this.ready_conditions = ready_conditions;
        }

        public int getRth_altitude() {
            return rth_altitude;
        }

        public void setRth_altitude(int rth_altitude) {
            this.rth_altitude = rth_altitude;
        }

        public Data.SimulateMission getSimulate_mission() {
            return simulate_mission;
        }

        public void setSimulate_mission(Data.SimulateMission simulate_mission) {
            this.simulate_mission = simulate_mission;
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

        public static class File {
            private String fingerprint;
            private String url;

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
    }
}
