package com.bubelov.coins.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.AuthController;
import com.bubelov.coins.util.MapMarkersCache;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {CoreModule.class, AndroidModule.class})
public interface AndroidComponent {
    Context context();

    SQLiteDatabase database();

    PlacesCache placesCache();

    MapMarkersCache markersCache();

    DatabaseSync databaseSync();

    FirebaseAnalytics analytics();

    void inject(DatabaseSync sync);

    void inject(PlacesCache cache);

    void inject(AuthController authController);
}