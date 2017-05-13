package com.bubelov.coins.repository.area;

import android.content.SharedPreferences;

import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.model.NotificationArea;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class NotificationAreaRepository {
    private SharedPreferences preferences;

    private Gson gson;

    @Inject
    NotificationAreaRepository(SharedPreferences preferences, Gson gson) {
        this.preferences = preferences;
        this.gson = gson;
    }

    public NotificationArea getNotificationArea() {
        if (!preferences.contains(PreferenceKeys.NOTIFICATION_AREA)) {
            return null;
        }

        return gson.fromJson(preferences.getString(PreferenceKeys.NOTIFICATION_AREA, ""), NotificationArea.class);
    }

    public void setNotificationArea(NotificationArea area) {
        preferences.edit().putString(PreferenceKeys.NOTIFICATION_AREA, gson.toJson(area)).apply();
    }
}