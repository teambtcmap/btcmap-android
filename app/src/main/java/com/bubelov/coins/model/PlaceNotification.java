package com.bubelov.coins.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.dagger.Injector;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Igor Bubelov
 */

public class PlaceNotification {
    private static final String KEY_PENDING_PLACES = "pending_places";

    private long placeId;

    public long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(long placeId) {
        this.placeId = placeId;
    }

    public static List<PlaceNotification> queryForAll() {
        Context context = Injector.INSTANCE.mainComponent().context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.contains(KEY_PENDING_PLACES)) {
            TypeToken typeToken = new TypeToken<ArrayList<PlaceNotification>>(){};
            return new Gson().fromJson(preferences.getString(KEY_PENDING_PLACES, "[]"), typeToken.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public static void insert(PlaceNotification place) {
        List<PlaceNotification> allPlaces = queryForAll();
        allPlaces.add(place);
        replaceWith(allPlaces);
    }

    public static void deleteAll() {
        Context context = Injector.INSTANCE.mainComponent().context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(KEY_PENDING_PLACES).apply();
    }

    private static void replaceWith(List<PlaceNotification> places) {
        Context context = Injector.INSTANCE.mainComponent().context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(KEY_PENDING_PLACES, new Gson().toJson(places)).apply();
    }
}