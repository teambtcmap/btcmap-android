package com.bubelov.coins.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.bubelov.coins.repository.currency.CurrenciesRepository;
import com.bubelov.coins.repository.place.PlacesRepository;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * @author Igor Bubelov
 */

public class DatabaseSync implements Runnable {
    @Inject
    Context context;

    @Inject
    CurrenciesRepository currenciesRepository;

    @Inject
    PlaceCategoriesRepository placeCategoriesRepository;

    @Inject
    PlacesRepository placesRepository;

    @Inject
    PlaceNotificationManager placeNotificationManager;

    @Inject
    SharedPreferences preferences;

    public DatabaseSync() {
        Injector.INSTANCE.mainComponent().inject(this);
    }

    @Override
    public void run() {
        if (!currenciesRepository.reloadFromNetwork()) {
            scheduleNextSync();
            return;
        }

        if (!placeCategoriesRepository.reloadFromNetwork()) {
            scheduleNextSync();
            return;
        }

        Collection<Place> newPlaces = placesRepository.fetchNewPlaces();

        for (Place place : newPlaces) {
            placeNotificationManager.notifyUserIfNecessary(place);
        }

        preferences.edit().putLong(PreferenceKeys.DATABASE_SYNC_DATE, System.currentTimeMillis()).apply();
        scheduleNextSync();
    }

    private void scheduleNextSync() {
        GcmNetworkManager.getInstance(context).schedule(new PeriodicTask.Builder()
                .setService(DatabaseGcmSyncService.class)
                .setTag(DatabaseGcmSyncService.TAG)
                .setPeriod(TimeUnit.DAYS.toSeconds(1))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPersisted(true)
                .build());
    }
}