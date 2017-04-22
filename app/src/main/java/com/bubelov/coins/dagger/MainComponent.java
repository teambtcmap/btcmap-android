package com.bubelov.coins.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.DataStorage;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.AuthController;
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

    CoinsApi api();

    Gson gson();

    SQLiteDatabase database();

    PlacesCache placesCache();

    MapMarkersCache markersCache();

    DatabaseSync databaseSync();

    FirebaseAnalytics analytics();

    AuthController authController();

    DataStorage dataStorage();

    void inject(DatabaseSync sync);
}