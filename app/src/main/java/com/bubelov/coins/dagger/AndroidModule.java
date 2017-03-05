package com.bubelov.coins.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.R;
import com.bubelov.coins.database.AssetDbHelper;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.service.NotificationsController;
import com.bubelov.coins.util.MapMarkersCache;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Igor Bubelov
 */

@Module
public class AndroidModule {
    private Context context;

    public AndroidModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context context() {
        return context;
    }

    @Provides
    @Singleton
    PlacesCache placesCache() {
        return new PlacesCache();
    }

    @Provides
    @Singleton
    MapMarkersCache markersCache() {
        return new MapMarkersCache();
    }

    @Provides
    @Singleton
    SQLiteDatabase database(Context context) {
        SQLiteOpenHelper helper = new AssetDbHelper(context, context.getResources().getString(R.string.database_name), context.getResources().getInteger(R.integer.database_version));
        return helper.getWritableDatabase();
    }

    @Provides
    @Singleton
    DatabaseSync databaseSync() {
        return new DatabaseSync();
    }

    @Provides
    @Singleton
    SharedPreferences preferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    NotificationsController notificationsController(Context context) {
        return new NotificationsController(context);
    }
}
