package com.aros.apron.entity;

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
        private int rth_altitude;
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
        private boolean locked;
        private String payload_index;


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

    }
}
