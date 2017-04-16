package com.bubelov.coins.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.model.NotificationArea;
import com.google.gson.Gson;

/**
 * Author: Igor Bubelov
 */

public class NotificationAreaProvider {
    private static final String TAG = NotificationAreaProvider.class.getSimpleName();

    private static final String AREA_KEY = TAG + ".area";

    private SharedPreferences preferences;

    public NotificationAreaProvider(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
}