package com.bubelov.coins.service;

import android.content.Context;
import android.preference.PreferenceManager;

import com.bubelov.coins.Constants;
import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.bubelov.coins.util.PlacesCache;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.api.coins.model.Currency;
import com.bubelov.coins.data.api.coins.model.Place;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.text.SimpleDateFormat;
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
    private static final int MAX_PLACES_PER_REQUEST = 500;

    @Inject
    PlaceNotificationManager placeNotificationManager;

    @Inject
    Context context;

    @Inject
    PlacesCache placesCache;

    @Inject
    DataManager dataManager;

    public DatabaseSync() {
        Injector.INSTANCE.mainComponent().inject(this);
    }

    @Override
    public void run() {
        try {
            List<Currency> currencies = dataManager.coinsApi().getCurrencies().execute().body();
            Timber.d("Inserting %s currencies", currencies.size());
            long time = System.currentTimeMillis();
            dataManager.database().insertCurrencies(currencies);
            Timber.d("Inserted in %s ms", System.currentTimeMillis() - time);

            boolean initialSync = dataManager.database().getLatestPlace() == null;

            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            while (true) {
                Place latestPlace = dataManager.database().getLatestPlace();
                Date lastUpdate = latestPlace == null ? new Date(0) : latestPlace.updatedAt();
                List<Place> places = dataManager.coinsApi().getPlaces(dateFormat.format(lastUpdate), MAX_PLACES_PER_REQUEST).execute().body();
                Timber.d("Inserting %s places", places.size());
                time = System.currentTimeMillis();
                dataManager.database().insertPlaces(places);
                Timber.d("Inserted in %s ms", System.currentTimeMillis() - time);

                for (Place place : places) {
                    if (!initialSync && placeNotificationManager.shouldNotifyUser(place)) {
                        placeNotificationManager.notifyUser(place.id(), place.name());
                    }
                }

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
        }

        GcmNetworkManager.getInstance(context).schedule(new PeriodicTask.Builder()
                .setService(DatabaseGcmSyncService.class)
                .setTag(DatabaseGcmSyncService.TAG)
                .setPeriod(TimeUnit.DAYS.toSeconds(1))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPersisted(true)
                .build());
    }
}