package com.bubelov.coins.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.util.PlacesCache;
import com.bubelov.coins.data.db.AssetDbHelper;
import com.bubelov.coins.data.db.DbHelper;
import com.bubelov.coins.data.gson.AutoValueAdapterFactory;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.bubelov.coins.util.MapMarkersCache;
import com.bubelov.coins.util.StringAdapter;
import com.bubelov.coins.util.UtcDateTypeAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Igor Bubelov
 */

@Module
public class MainModule {
    private Context context;

    public MainModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context context() {
        return context;
    }

    @Provides
    @Singleton
    DataManager dataManager(SQLiteDatabase db, SharedPreferences preferences, Gson gson) {
        return new DataManager(db, preferences, gson);
    }

    @Provides
    @Singleton
    PlacesCache placesCache(DataManager dataManager) {
        return new PlacesCache(dataManager);
    }

    @Provides
    @Singleton
    MapMarkersCache markersCache() {
        return new MapMarkersCache();
    }

    @Provides
    @Singleton
    DatabaseSync databaseSync() {
        return new DatabaseSync();
    }

    @Provides
    @Singleton
    FirebaseAnalytics analytics(Context context) {
        return FirebaseAnalytics.getInstance(context);
    }

    @Provides
    @Singleton
    PlaceNotificationManager placeNotificationManager(Context context, DataManager dataManager) {
        return new PlaceNotificationManager(context, dataManager);
    }

    @Provides
    @Singleton
    SQLiteDatabase database(Context context) {
        SQLiteOpenHelper helper = BuildConfig.USE_DB_SNAPSHOT ? new AssetDbHelper(context) : new DbHelper(context);
        return helper.getWritableDatabase();
    }

    @Provides
    @Singleton
    SharedPreferences preferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    Gson gson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new AutoValueAdapterFactory())
                .registerTypeAdapter(Date.class, new UtcDateTypeAdapter())
                .registerTypeAdapter(String.class, new StringAdapter())
                .create();
    }
}