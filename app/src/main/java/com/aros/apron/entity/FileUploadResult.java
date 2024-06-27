package com.aros.apron.entity;

public class FileUploadResult {
    private int msg_type;
    private String fileName;
    private String objectKey;
    private String buckName;
    private int offIndex;
    private int fileNum;
    private String sortiesId;
    private String url;
    private Long fileSize;
    private String flightId;

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(int msg_type) {
        this.msg_type = msg_type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getBuckName() {
        return buckName;
    }

    public void setBuckName(String buckName) {
        this.buckName = buckName;
    }

    public int getOffIndex() {
        return offIndex;
    }

    public void setOffIndex(int offIndex) {
        this.offIndex = offIndex;
    }

    public int getFileNum() {
        return fileNum;
    }

    public void setFileNum(int fileNum) {
        this.fileNum = fileNum;
    }

    public String getSortiesId() {
        return sortiesId;
    }

    public void setSortiesId(String sortiesId) {
        this.sortiesId = sortiesId;
    }
}
