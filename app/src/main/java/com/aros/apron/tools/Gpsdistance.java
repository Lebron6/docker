package com.aros.apron.tools;

public class Gpsdistance {
    private static final double EARTH_RADIUS = 6371000;

    /**
	     * 水平距离（Haversine）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * 三维直线距离（水平 + 高度差）
     * 参数：经纬度（度），高度（米）
     */
    public static double calculate3DDistance(double lat1, double lon1, double alt1,
                                             double lat2, double lon2, double alt2) {
        // 1. 算水平距离
        double horizontal = calculateDistance(lat1, lon1, lat2, lon2);

        // 2. 算垂直距离（高度差）
        double vertical = Math.abs(alt2 - alt1);

        // 3. 勾股定理算斜距
        return Math.sqrt(horizontal * horizontal + vertical * vertical);
    }

    /**
     * 重载：直接用 DJI 的 LocationCoordinate3D
     */
    public static double calculate3DDistance(dji.sdk.keyvalue.value.common.LocationCoordinate3D current,
                                             dji.sdk.keyvalue.value.common.LocationCoordinate3D target) {
        return calculate3DDistance(
                current.getLatitude(), current.getLongitude(), current.getAltitude(),
                target.getLatitude(), target.getLongitude(), target.getAltitude()
        );
    }
}