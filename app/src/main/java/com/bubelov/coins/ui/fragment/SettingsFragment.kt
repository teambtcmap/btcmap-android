package com.bubelov.coins.ui.fragment

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen

import com.bubelov.coins.R
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.database.sync.DatabaseSyncService

import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SettingsFragment : PreferenceFragment() {
    @Inject
    lateinit var placesRepository: PlacesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.appComponent.inject(this)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        if (preference.key == "pref_test_notification") {
            showRandomPlaceNotification()
        }

        if (preference.key == "pref_update_places") {
            DatabaseSyncService.start(activity)
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    private fun showRandomPlaceNotification() {
        val randomPlace = placesRepository.getRandomPlace()

        if (randomPlace != null) {
            Injector.appComponent.notificationManager().notifyUser(randomPlace)
        }
    }
}