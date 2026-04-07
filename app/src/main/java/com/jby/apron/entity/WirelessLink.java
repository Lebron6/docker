package com.jby.apron.entity;

public class WirelessLink {


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

        private String dongle_number;
        private String link_state_4g;
        private String sdr_link_state;
        private String link_workmode;
        private String sdr_quality;
        private String quality_4g;
        private String uav_quality_4g;
        private String gnd_quality_4g;
        private String sdr_freq_band;
        private String freq_band_4g;

        public String getDongle_number() {
            return dongle_number;
        }

        public void setDongle_number(String dongle_number) {
            this.dongle_number = dongle_number;
        }

        public String getLink_state_4g() {
            return link_state_4g;
        }

        public void setLink_state_4g(String link_state_4g) {
            this.link_state_4g = link_state_4g;
        }

        public String getSdr_link_state() {
            return sdr_link_state;
        }

        public void setSdr_link_state(String sdr_link_state) {
            this.sdr_link_state = sdr_link_state;
        }

        public String getLink_workmode() {
            return link_workmode;
        }

        public void setLink_workmode(String link_workmode) {
            this.link_workmode = link_workmode;
        }

        public String getSdr_quality() {
            return sdr_quality;
        }

        public void setSdr_quality(String sdr_quality) {
            this.sdr_quality = sdr_quality;
        }

        public String getQuality_4g() {
            return quality_4g;
        }

        public void setQuality_4g(String quality_4g) {
            this.quality_4g = quality_4g;
        }

        public String getUav_quality_4g() {
            return uav_quality_4g;
        }

        public void setUav_quality_4g(String uav_quality_4g) {
            this.uav_quality_4g = uav_quality_4g;
        }

        public String getGnd_quality_4g() {
            return gnd_quality_4g;
        }

        public void setGnd_quality_4g(String gnd_quality_4g) {
            this.gnd_quality_4g = gnd_quality_4g;
        }

        public String getSdr_freq_band() {
            return sdr_freq_band;
        }

        public void setSdr_freq_band(String sdr_freq_band) {
            this.sdr_freq_band = sdr_freq_band;
        }

        public String getFreq_band_4g() {
            return freq_band_4g;
        }

        public void setFreq_band_4g(String freq_band_4g) {
            this.freq_band_4g = freq_band_4g;
        }
    }
}
