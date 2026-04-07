package com.jby.apron.entity;

public enum ArucoMarkerDimensions {
    ID_1(1, 0.18f),
    ID_2(2, 0.18f),
    ID_3(3, 0.18f),
    ID_4(4, 0.18f),
    ID_5(5, 0.15f),
    ID_6(6, 0.24f),

    ID_11(11, 0.03f),
    ID_12(12, 0.03f),
    ID_13(13, 0.03f),
    ID_14(14, 0.03f),
    ID_15(15, 0.03f),
    ID_16(16, 0.03f),
    ID_17(17, 0.03f),
    ID_18(18, 0.03f),
    ID_19(19, 0.03f);

    private final int id;
    private final float size;

    ArucoMarkerDimensions(int id, float size) {
        this.id = id;
        this.size = size;
    }

    public int getId() {
        return id;
    }

    public float getSize() {
        return size;
    }

    public static float getSizeById(int id) {
        for (ArucoMarkerDimensions dimension : ArucoMarkerDimensions.values()) {
            if (dimension.getId() == id) {
                return dimension.getSize();
            }
        }
        throw new IllegalArgumentException("No dimension found for ID: " + id);
    }
}
