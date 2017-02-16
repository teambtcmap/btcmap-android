package com.bubelov.coins.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.bubelov.coins.Constants;
import com.bubelov.coins.EventBus;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.DatabaseSyncStartedEvent;
import com.bubelov.coins.event.DatabaseSyncedEvent;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class DatabaseSyncService extends IntentService {
    private static final int MAX_PLACES_PER_REQUEST = 2500;

    public static void startIfNeverSynced(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.contains(PreferenceKeys.DATABASE_SYNC_DATE)) {
            start(context);
        }
    }

    public static void start(Context context) {
        context.startService(new Intent(context, DatabaseSyncService.class));
    }

    public DatabaseSyncService() {
        super(DatabaseSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("New intent");

        if (intent == null) {
            return;
        }

        Timber.d("Posting sync started event");
        EventBus.getInstance().post(new DatabaseSyncStartedEvent());

        try {
            long placesBeforeSync = Place.getCount();
            Timber.d("Places before sync: %s", placesBeforeSync);
            long time = System.currentTimeMillis();
            sync();
            Timber.d("Sync time: %s", System.currentTimeMillis() - time);

            PlacesCache cache = Injector.INSTANCE.getAppComponent().getPlacesCache();
            cache.invalidate();

            EventBus.getInstance().post(new DatabaseSyncedEvent());
        } catch (Exception e) {
            Timber.e(e, "Couldn't sync database");
            EventBus.getInstance().post(new DatabaseSyncFailedEvent());
        }

        GcmNetworkManager.getInstance(this).schedule(new PeriodicTask.Builder()
                .setService(DatabaseGcmSyncService.class)
                .setTag(DatabaseGcmSyncService.TAG)
                .setPeriod(TimeUnit.DAYS.toSeconds(1))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPersisted(true)
                .build());
    }

    private void sync() throws Exception {
        long time = System.currentTimeMillis();

        if (Currency.getCount() == 0) {
            Timber.d("Requesting currencies");
            CoinsApi api = Injector.INSTANCE.getAppComponent().provideApi();
            List<Currency> currencies = api.getCurrencies().execute().body();
            Timber.d("%s currencies loaded. Time: %s", currencies.size(), System.currentTimeMillis() - time);

            Timber.d("Inserting currencies");
            time = System.currentTimeMillis();
            Currency.insert(currencies);
            Timber.d("Inserted. Time: %s", System.currentTimeMillis() - time);
        }

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        UserNotificationController notificationManager = new UserNotificationController(getApplicationContext());
        boolean initialSync = Place.getCount() == 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        while (true) {
            Date lastUpdate = getLatestPlaceUpdateDate(db);
            CoinsApi api = Injector.INSTANCE.getAppComponent().provideApi();
            List<Place> places = api.getPlaces(dateFormat.format(lastUpdate), MAX_PLACES_PER_REQUEST).execute().body();

            for (Place place : places) {
                if (!initialSync && notificationManager.shouldNotifyUser(place)) {
                    notificationManager.notifyUser(place.getId(), place.getName());
                }
            }

            Timber.d("Inserting %s places", places.size());
            time = System.currentTimeMillis();
            Place.insert(places);
            Timber.d("Inserted. Time: %s", System.currentTimeMillis() - time);

            if (places.size() < MAX_PLACES_PER_REQUEST) {
                break;
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(PreferenceKeys.DATABASE_SYNC_DATE, System.currentTimeMillis())
                .apply();
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