package com.bubelov.coins.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.bubelov.coins.Constants;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class DatabaseSync implements Runnable {
    private static final int MAX_PLACES_PER_REQUEST = 2500;

    @Inject
    CoinsApi api;

    @Inject
    SQLiteDatabase db;

    @Inject
    NotificationsController notificationsController;

    @Inject
    Context context;

    @Inject
    PlacesCache placesCache;

    public DatabaseSync() {
        Injector.INSTANCE.getAndroidComponent().inject(this);
    }

    @Override
    public void run() {
        try {
            if (Currency.getCount() == 0) {
                List<Currency> currencies = api.getCurrencies().execute().body();
                Currency.insert(currencies);
            }

            boolean initialSync = Place.getCount() == 0;

            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            while (true) {
                Date lastUpdate = getLatestPlaceUpdateDate(db);
                List<Place> places = api.getPlaces(dateFormat.format(lastUpdate), MAX_PLACES_PER_REQUEST).execute().body();

                for (Place place : places) {
                    if (!initialSync && notificationsController.shouldNotifyUser(place)) {
                        Place.insert(Collections.singletonList(place));
                        notificationsController.notifyUser(place.getId(), place.getName());
                    }
                }

                Place.insert(places);

                if (places.size() < MAX_PLACES_PER_REQUEST) {
                    break;
                }
            }

            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(PreferenceKeys.DATABASE_SYNC_DATE, System.currentTimeMillis())
                    .apply();

            placesCache.invalidate();
        } catch (Exception e) {
            Timber.e(e, "Couldn't sync database");
            Crashlytics.logException(e);
        }

        GcmNetworkManager.getInstance(context).schedule(new PeriodicTask.Builder()
                .setService(DatabaseGcmSyncService.class)
                .setTag(DatabaseGcmSyncService.TAG)
                .setPeriod(TimeUnit.DAYS.toSeconds(1))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPersisted(true)
                .build());
    }

    private Date getLatestPlaceUpdateDate(SQLiteDatabase db) {
        Cursor lastUpdateCursor = db.query(DbContract.Places.TABLE_NAME,
                new String[]{DbContract.Places._UPDATED_AT},
                null,
                null,
                null,
                null,
                DbContract.Places._UPDATED_AT + " DESC",
                "1");

        long lastUpdateMillis = 0;

        if (lastUpdateCursor.moveToNext()) {
            lastUpdateMillis = lastUpdateCursor.isNull(0) ? 0 : lastUpdateCursor.getLong(0);
        }

        lastUpdateCursor.close();
        return new Date(lastUpdateMillis);
    }
}
