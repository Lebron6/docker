package com.aros.apron.entity;

public class TaskFailEvent {
    private String tid;
    private String bid;
    private long timestamp;
    private String method;
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

       private int reason_code;

        public int getReason_code() {
            return reason_code;
        }

        public void setReason_code(int reason_code) {
            this.reason_code = reason_code;
        }
    }
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


}
