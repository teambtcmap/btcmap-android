package com.bubelov.coins.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.WorkerThread;

import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.repository.currency.CurrenciesRepository;
import com.bubelov.coins.repository.place.PlacesRepository;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class DatabaseSync {
    private final Context context;

    private final PlacesRepository placesRepository;

    private final PlaceCategoriesRepository placeCategoriesRepository;

    private final CurrenciesRepository currenciesRepository;

    private final PlaceNotificationManager placeNotificationManager;

    private final SharedPreferences preferences;

    private volatile boolean syncing;

    private final Collection<Callback> callbacks = new ArrayList<>();

    @Inject
    DatabaseSync(Context context, PlacesRepository placesRepository, PlaceCategoriesRepository placeCategoriesRepository, CurrenciesRepository currenciesRepository, PlaceNotificationManager placeNotificationManager, SharedPreferences preferences) {
        this.context = context;
        this.placesRepository = placesRepository;
        this.placeCategoriesRepository = placeCategoriesRepository;
        this.currenciesRepository = currenciesRepository;
        this.placeNotificationManager = placeNotificationManager;
        this.preferences = preferences;
    }

    @WorkerThread
    void sync() {
        syncing = true;

        try {
            Collection<Place> newPlaces = placesRepository.fetchNewPlaces();

            for (Place place : newPlaces) {
                placeNotificationManager.notifyUserIfNecessary(place);
            }

            if (!placeCategoriesRepository.reloadFromNetwork()) {
                throw new IllegalStateException("Couldn't sync place categories");
            }

            if (!currenciesRepository.reloadFromNetwork()) {
                throw new IllegalStateException("Couldn't sync place categories");
            }

            preferences.edit().putLong(PreferenceKeys.LAST_SYNC_DATE, System.currentTimeMillis()).apply();

            for (Callback callback : callbacks) {
                callback.onDatabaseSyncFinished();
            }
        } catch (Exception e) {
            Timber.e(e, "Couldn't sync database");

            for (Callback callback : callbacks) {
                callback.onDatabaseSyncError();
            }
        } finally {
            syncing = false;
            scheduleNextSync();
        }
    }

    public boolean isSyncing() {
        return syncing;
    }

    public long getLastSyncDate() {
        if (preferences.contains(PreferenceKeys.LAST_SYNC_DATE)) {
            return preferences.getLong(PreferenceKeys.LAST_SYNC_DATE, 0);
        } else {
            return 0;
        }
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
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

    public interface Callback {
        void onDatabaseSyncFinished();

        void onDatabaseSyncError();
    }
}