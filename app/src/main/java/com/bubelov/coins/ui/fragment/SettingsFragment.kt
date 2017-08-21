package com.bubelov.coins.ui.fragment

import android.content.Context
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen

import com.bubelov.coins.R
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.database.sync.DatabaseSyncService
import com.bubelov.coins.util.PlaceNotificationManager
import dagger.android.AndroidInjection

import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SettingsFragment : PreferenceFragment() {
    @Inject internal lateinit var placesRepository: PlacesRepository

    @Inject internal lateinit var placeNotificationsManager: PlaceNotificationManager

    override fun onAttach(context: Context) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when (preference.key) {
            "pref_test_notification" -> placeNotificationsManager.notifyUser(placesRepository.getRandomPlace()!!)
            "pref_update_places" -> DatabaseSyncService.start(activity)
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }
}