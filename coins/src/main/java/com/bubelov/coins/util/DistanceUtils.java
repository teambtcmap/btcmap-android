package com.bubelov.coins.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Author: Igor Bubelov
 * Date: 10/16/15 6:55 PM
 */

public class DistanceUtils {
    public static float getDistance(LatLng point1, LatLng point2) {
        return getDistance(point1.latitude, point1.longitude, point2.latitude, point2.longitude);
    }

    public static float getDistance(Location point1, Location point2) {
        return getDistance(point1.getLatitude(), point1.getLongitude(), point2.getLatitude(), point2.getLongitude());
    }

    public static float getDistance(LatLng point1, Location point2) {
        return getDistance(point1.latitude, point1.longitude, point2.getLatitude(), point2.getLongitude());
    }

    public static float getDistance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        float[] distance = new float[1];
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance);
        return distance[0];
    }
}
