package com.bubelov.coins.model;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Author: Igor Bubelov
 */

public class NotificationArea implements Serializable {
    public static final int DEFAULT_RADIUS_METERS = 50_000;

    private double centerLatitude;

    private double centerLongitude;

    private int radiusMeters;

    public NotificationArea(LatLng center) {
        this(center, DEFAULT_RADIUS_METERS);
    }

    public NotificationArea(LatLng center, int radiusMeters) {
        this.centerLatitude = center.latitude;
        this.centerLongitude = center.longitude;
        this.radiusMeters = radiusMeters;
    }

    public LatLng getCenter() {
        return new LatLng(centerLatitude, centerLongitude);
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        NotificationArea otherArea = (NotificationArea) other;

        return Double.compare(otherArea.centerLatitude, centerLatitude) == 0
                && Double.compare(otherArea.centerLongitude, centerLongitude) == 0
                && radiusMeters == otherArea.radiusMeters;

    }
}