package com.bubelov.coins.data.repository.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.domain.PlaceNotification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceNotificationsRepository {
    private static final String PLACE_NOTIFICATIONS = "place_notifications";

    private final SharedPreferences preferences;

    private final Gson gson;

    @Inject
    PlaceNotificationsRepository(SharedPreferences preferences, Gson gson) {
        this.preferences = preferences;
        this.gson = gson;
    }

    public Collection<PlaceNotification> getNotifications() {
        if (preferences.contains(PLACE_NOTIFICATIONS)) {
            TypeToken typeToken = new TypeToken<ArrayList<PlaceNotification>>() {
            };
            return gson.fromJson(preferences.getString(PLACE_NOTIFICATIONS, "[]"), typeToken.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public void addNotification(PlaceNotification notification) {
        Collection<PlaceNotification> notifications = getNotifications();
        notifications.add(notification);
        setNotifications(notifications);
    }

    public void clear() {
        Context context = Injector.INSTANCE.mainComponent().context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(PLACE_NOTIFICATIONS).apply();
    }

    private void setNotifications(Collection<PlaceNotification> places) {
        preferences.edit().putString(PLACE_NOTIFICATIONS, gson.toJson(places)).apply();
    }
}