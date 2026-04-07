package com.jby.apron.entity;

public class MediaUpLoad {


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

        private String bucket_name;
        private String object_key;
        private String flight_id;
        private String file_name;
        private int uploaded_file_count;
        private int expected_file_count;

        public String getBucket_name() {
            return bucket_name;
        }

        public void setBucket_name(String bucket_name) {
            this.bucket_name = bucket_name;
        }

        public String getObject_key() {
            return object_key;
        }

        public void setObject_key(String object_key) {
            this.object_key = object_key;
        }

        public String getFlight_id() {
            return flight_id;
        }

        public void setFlight_id(String flight_id) {
            this.flight_id = flight_id;
        }

        public String getFile_name() {
            return file_name;
        }

        public void setFile_name(String file_name) {
            this.file_name = file_name;
        }

        public int getUploaded_file_count() {
            return uploaded_file_count;
        }

        public void setUploaded_file_count(int uploaded_file_count) {
            this.uploaded_file_count = uploaded_file_count;
        }

        public int getExpected_file_count() {
            return expected_file_count;
        }

        public void setExpected_file_count(int expected_file_count) {
            this.expected_file_count = expected_file_count;
        }
    }
}
