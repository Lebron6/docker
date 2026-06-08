package com.aros.apron.entity;

import java.util.List;

/**
 * 飞向目标点进度上报事件
 * Method: fly_to_point_progress
 * Topic: thing/product/{gateway_sn}/events (up)
 */
public class FlyToPointProgress {

    private String bid;
    private String tid;
    private Long timestamp;
    private String method;
    private Integer needReply;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getNeedReply() {
        return needReply;
    }

    public void setNeedReply(Integer needReply) {
        this.needReply = needReply;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String fly_to_id;
        private String status;          // wayline_cancel | wayline_failed | wayline_ok | wayline_progress
        private int result;             // 非0代表错误
        private int way_point_index;
        private Float remaining_distance;  // 米
        private Float remaining_time;      // 秒
        private List<PlannedPathPoint> planned_path_points;

        public String getFly_to_id() {
            return fly_to_id;
        }

        public void setFly_to_id(String fly_to_id) {
            this.fly_to_id = fly_to_id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }

        public int getWay_point_index() {
            return way_point_index;
        }

        public void setWay_point_index(int way_point_index) {
            this.way_point_index = way_point_index;
        }

        public Float getRemaining_distance() {
            return remaining_distance;
        }

        public void setRemaining_distance(Float remaining_distance) {
            this.remaining_distance = remaining_distance;
        }

        public Float getRemaining_time() {
            return remaining_time;
        }

        public void setRemaining_time(Float remaining_time) {
            this.remaining_time = remaining_time;
        }

        public List<PlannedPathPoint> getPlanned_path_points() {
            return planned_path_points;
        }

        public void setPlanned_path_points(List<PlannedPathPoint> planned_path_points) {
            this.planned_path_points = planned_path_points;
        }
    }

    public static class PlannedPathPoint {
        private double latitude;
        private double longitude;
        private float height;

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

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }
    }
}