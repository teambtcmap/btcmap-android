package com.bubelov.coins.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.model.NotificationArea;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

/**
 * Author: Igor Bubelov
 * Date: 7/8/15 11:52 PM
 */

public class NotificationAreaProvider {
    private static final String TAG = NotificationAreaProvider.class.getSimpleName();

    private static final String AREA_KEY = TAG + ".area";

    private SharedPreferences preferences;

    public NotificationAreaProvider(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        migrateFromOldFormatIfNecessary();
    }

    public NotificationArea get() {
        if (!preferences.contains(AREA_KEY)) {
            return null;
        }

        return new Gson().fromJson(preferences.getString(AREA_KEY, ""), NotificationArea.class);
    }

    public void save(NotificationArea area) {
        preferences.edit().putString(AREA_KEY, new Gson().toJson(area)).apply();
    }

    /**
     * Workaround to upgrade from app version 16 and lower
     */
    private void migrateFromOldFormatIfNecessary() {
        if (!preferences.contains(AREA_KEY)) {
            float latitude = preferences.getFloat("latitude", 0);
            float longitude = preferences.getFloat("longitude", 0);
            int radius = preferences.getInt("radius", 0);

            if (latitude != 0 && longitude != 0 && radius != 0) {
                LatLng center = new LatLng(latitude, longitude);
                save(new NotificationArea(center, radius));
            }
        }
    }
}