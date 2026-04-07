package com.jby.apron.entity;

public class FlightTaskProgress {


    private String bid;
    private Data data;
    private String tid;
    private long timestamp;
    private String method;

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static class Data {
        private Output output;
        private int result;

        public Output getOutput() {
            return output;
        }

        public void setOutput(Output output) {
            this.output = output;
        }

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }

        public static class Output {
            private Ext ext;
            private Progress progress;
            private String status;

            public Ext getExt() {
                return ext;
            }

            public void setExt(Ext ext) {
                this.ext = ext;
            }

            public Progress getProgress() {
                return progress;
            }

            public void setProgress(Progress progress) {
                this.progress = progress;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public static class Ext {
                private BreakPoint break_point;
                private int current_waypoint_index;
                private String flight_id;
                private int media_count;
                private String track_id;
                private int wayline_id;
                private int wayline_mission_state;

                public BreakPoint getBreak_point() {
                    return break_point;
                }

                public void setBreak_point(BreakPoint break_point) {
                    this.break_point = break_point;
                }

                public int getCurrent_waypoint_index() {
                    return current_waypoint_index;
                }

                public void setCurrent_waypoint_index(int current_waypoint_index) {
                    this.current_waypoint_index = current_waypoint_index;
                }

                public String getFlight_id() {
                    return flight_id;
                }

                public void setFlight_id(String flight_id) {
                    this.flight_id = flight_id;
                }

                public int getMedia_count() {
                    return media_count;
                }

                public void setMedia_count(int media_count) {
                    this.media_count = media_count;
                }

                public String getTrack_id() {
                    return track_id;
                }

                public void setTrack_id(String track_id) {
                    this.track_id = track_id;
                }

                public int getWayline_id() {
                    return wayline_id;
                }

                public void setWayline_id(int wayline_id) {
                    this.wayline_id = wayline_id;
                }

                public int getWayline_mission_state() {
                    return wayline_mission_state;
                }

                public void setWayline_mission_state(int wayline_mission_state) {
                    this.wayline_mission_state = wayline_mission_state;
                }

                public static class BreakPoint {
                    private double attitude_head;
                    private int break_reason;
                    private double height;
                    private int index;
                    private double latitude;
                    private double longitude;
                    private double progress;
                    private int state;
                    private int wayline_id;

                    public double getAttitude_head() {
                        return attitude_head;
                    }

                    public void setAttitude_head(double attitude_head) {
                        this.attitude_head = attitude_head;
                    }

                    public int getBreak_reason() {
                        return break_reason;
                    }

                    public void setBreak_reason(int break_reason) {
                        this.break_reason = break_reason;
                    }

                    public double getHeight() {
                        return height;
                    }

                    public void setHeight(double height) {
                        this.height = height;
                    }

                    public int getIndex() {
                        return index;
                    }

                    public void setIndex(int index) {
                        this.index = index;
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
            }

            public static class Progress {
                private int current_step;
                private int percent;

                public int getCurrent_step() {
                    return current_step;
                }

                public void setCurrent_step(int current_step) {
                    this.current_step = current_step;
                }

                public int getPercent() {
                    return percent;
                }

                public void setPercent(int percent) {
                    this.percent = percent;
                }
            }
        }
    }
}
