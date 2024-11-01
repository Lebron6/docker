package com.aros.apron.entity;

public class H264Frame {

   private   int frameLen;
   private   byte[] frameData;
   private   long timestamp;
   private   int frameType;

    public H264Frame() {
    }

    public int getFrameLen() {
        return frameLen;
    }

    public void setFrameLen(int frameLen) {
        this.frameLen = frameLen;
    }

    public byte[] getFrameData() {
        return frameData;
    }

    public void setFrameData(byte[] frameData) {
        this.frameData = frameData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFrameType() {
        return frameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }
}