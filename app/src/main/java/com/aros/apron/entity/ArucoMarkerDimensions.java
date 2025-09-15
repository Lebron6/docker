package com.aros.apron.entity;

public enum ArucoMarkerDimensions {
    ID_1(1, 0.15f),
    ID_2(2, 0.24f),
    ID_3(3, 0.15f),
    ID_4(4, 0.09f),
    ID_5(5, 0.09f),
    ID_6(6, 0.09f),
    ID_7(7, 0.09f),
    ID_8(8, 0.035f),
    ID_9(9, 0.09f),
    ID_10(10, 0.035f),
    ID_11(11, 0.035f),
    ID_12(12, 0.035f),
    ID_13(13, 0.035f),
    ID_14(14, 0.035f),
    ID_15(15, 0.035f),
    ID_16(16, 0.035f),
    ID_17(17, 0.035f),
    ID_18(18, 0.035f),
    ID_19(19, 0.035f);

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
