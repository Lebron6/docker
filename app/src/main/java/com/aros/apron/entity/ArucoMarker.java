package com.aros.apron.entity;

import org.opencv.core.Mat;

import java.util.Objects;

public class ArucoMarker {
    private int id;
    private Mat conner;



    public ArucoMarker(int id, Mat conner) {
        this.id = id;
        this.conner = conner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Mat getConner() {
        return conner;
    }

    public void setConner(Mat conner) {
        this.conner = conner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArucoMarker)) return false;
        ArucoMarker that = (ArucoMarker) o;
        return getId() == that.getId() && getConner().equals(that.getConner());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getConner());
    }
}
