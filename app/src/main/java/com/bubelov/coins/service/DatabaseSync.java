package com.bubelov.coins.service;

import android.content.Context;
import android.preference.PreferenceManager;

import com.bubelov.coins.Constants;
import com.bubelov.coins.DataStorage;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
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
    NotificationsController notificationsController;

    @Inject
    Context context;

    @Inject
    PlacesCache placesCache;

    @Inject
    DataStorage dataStorage;

    public DatabaseSync() {
        Injector.INSTANCE.mainComponent().inject(this);
    }

    @Override
    public void run() {
        try {
            List<Currency> currencies = api.getCurrencies().execute().body();

            for (Currency currency : currencies) {
                dataStorage.insertCurrency(currency);
            }

            boolean initialSync = dataStorage.getLatestPlace() == null;

            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            while (true) {
                Place latestPlace = dataStorage.getLatestPlace();
                Date lastUpdate = latestPlace == null ? new Date(0) : latestPlace.updatedAt();
                List<Place> places = api.getPlaces(dateFormat.format(lastUpdate), MAX_PLACES_PER_REQUEST).execute().body();

                dataStorage.doInTransaction(() -> {
                    dataStorage.insertPlaces(places);

                    for (Place place : places) {
                        dataStorage.insertCurrencyForPlaces(place, place.currencies);
                    }
                });

                for (Place place : places) {
                    if (!initialSync && notificationsController.shouldNotifyUser(place)) {
                        notificationsController.notifyUser(place.id(), place.name());
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