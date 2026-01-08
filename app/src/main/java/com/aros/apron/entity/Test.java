package com.aros.apron.entity;

public class Test {
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
        private AlternateLandPoint alternate_land_point;
        private long execute_time;
        private int exit_wayline_when_rc_lost;
        private File file;
        private String flight_id;
        private int flight_safety_advance_check;
        private int out_of_control_action;
        private int rth_altitude;
        private int rth_mode;
        private boolean takeoffToPointTask;
        private int task_type;
        private int wayline_precision_type;

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
            private int safe_land_height;

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

            public int getSafe_land_height() {
                return safe_land_height;
            }

            public void setSafe_land_height(int safe_land_height) {
                this.safe_land_height = safe_land_height;
            }
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
    }
}
