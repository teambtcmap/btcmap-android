package com.bubelov.coins.ui.fragment;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.place.PlacesRepository;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.service.DatabaseSyncService;

import javax.inject.Inject;

/**
 * @author Igor Bubelov
 */

public class SettingsFragment extends PreferenceFragment {
    @Inject
    PlacesRepository placesRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.INSTANCE.mainComponent().inject(this);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("pref_test_notification")) {
            showRandomPlaceNotification();
        }

        if (preference.getKey().equals("pref_update_places")) {
            DatabaseSyncService.start(getActivity());
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showRandomPlaceNotification() {
        Place randomPlace = placesRepository.getRandomPlace();

        if (randomPlace != null) {
            Injector.INSTANCE.mainComponent().notificationManager().notifyUser(randomPlace);
        }
    }
}