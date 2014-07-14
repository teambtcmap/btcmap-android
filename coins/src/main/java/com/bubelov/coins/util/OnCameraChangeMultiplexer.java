package com.bubelov.coins.util;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;

/**
 * Author: Igor Bubelov
 * Date: 06/07/14 16:07
 */

public class OnCameraChangeMultiplexer implements GoogleMap.OnCameraChangeListener {
    private GoogleMap.OnCameraChangeListener[] listeners;

    public OnCameraChangeMultiplexer(GoogleMap.OnCameraChangeListener... listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        for (GoogleMap.OnCameraChangeListener listener : listeners) {
            if (listener == null) {
                continue;
            }

            listener.onCameraChange(cameraPosition);
        }
    }
}
