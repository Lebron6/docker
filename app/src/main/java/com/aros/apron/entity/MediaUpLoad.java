package com.aros.apron.entity;

import com.google.gson.annotations.SerializedName;

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
        private Metadata metadata;

        public Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
        }

        public static class Metadata {
            private String absolute_altitude;
            private String created_time;
            private String gimbal_yaw_degree;
            private String relative_altitude;
            private Metadata.ShootPosition shoot_position;

            public String getAbsolute_altitude() {
                return absolute_altitude;
            }

            public void setAbsolute_altitude(String absolute_altitude) {
                this.absolute_altitude = absolute_altitude;
            }

            public String getCreated_time() {
                return created_time;
            }

            public void setCreated_time(String created_time) {
                this.created_time = created_time;
            }

            public String getGimbal_yaw_degree() {
                return gimbal_yaw_degree;
            }

            public void setGimbal_yaw_degree(String gimbal_yaw_degree) {
                this.gimbal_yaw_degree = gimbal_yaw_degree;
            }

            public String getRelative_altitude() {
                return relative_altitude;
            }

            public void setRelative_altitude(String relative_altitude) {
                this.relative_altitude = relative_altitude;
            }

            public Metadata.ShootPosition getShoot_position() {
                return shoot_position;
            }

            public void setShoot_position(Metadata.ShootPosition shoot_position) {
                this.shoot_position = shoot_position;
            }

            public static class ShootPosition {
                private String lat;
                private String lng;

                public String getLat() {
                    return lat;
                }

                public void setLat(String lat) {
                    this.lat = lat;
                }

                public String getLng() {
                    return lng;
                }

                public void setLng(String lng) {
                    this.lng = lng;
                }
            }
        }

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
