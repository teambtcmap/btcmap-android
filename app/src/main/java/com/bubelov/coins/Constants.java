package com.bubelov.coins;

import com.google.android.gms.maps.model.LatLng;

/**
 * Author: Igor Bubelov
 */

public interface Constants {
    String DATE_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'";

    double SAN_FRANCISCO_LATITUDE = 34.05;
    double SAN_FRANCISCO_LONGITUDE = -118.25;

    LatLng DEFAULT_LOCATION = new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE);

    float MAP_MARKER_ANCHOR_U = 0.5f;
    float MAP_MARKER_ANCHOR_V = 0.91145f;
}