package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.util.PlacesCache;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.bubelov.coins.util.MapMarkersCache;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {MainModule.class})
public interface MainComponent {
    Context context();

    DataManager dataManager();

    PlacesCache placesCache();

    MapMarkersCache markersCache();

    DatabaseSync databaseSync();

    FirebaseAnalytics analytics();

    PlaceNotificationManager notificationManager();

    Gson gson();

    void inject(DatabaseSync sync);
}