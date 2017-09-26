package com.bubelov.coins.ui.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v7.app.AppCompatActivity
import com.bubelov.coins.R
import com.bubelov.coins.database.sync.DatabaseSyncService
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.PlaceNotificationManager
import dagger.android.AndroidInjection
import org.jetbrains.anko.alert
import java.util.*
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject internal lateinit var placesRepository: PlacesRepository

    @Inject internal lateinit var syncLogsRepository: SyncLogsRepository

    @Inject internal lateinit var placeNotificationsManager: PlaceNotificationManager

    override fun onAttach(context: Context) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        updateDistanceUnitsSummary()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.pref_sync_database_key) -> DatabaseSyncService.start(activity)
            getString(R.string.pref_show_sync_log_key) -> showSyncLog()
            getString(R.string.pref_test_notification_key) -> testNotification()
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        updateDistanceUnitsSummary()
    }

    private fun showSyncLog() {
        val logs = syncLogsRepository.syncLogs.reversed().map { "Date: ${Date(it.time)}, Affected places: ${it.affectedPlaces}" }

        if (logs.isEmpty()) {
            alert(message = "Logs are empty").show()
        } else {
            alert { items(logs, onItemSelected = { _, _, _ -> }) }.show()
        }
    }

    private fun updateDistanceUnitsSummary() {
        val distanceUnits = findPreference(getString(R.string.pref_distance_units_key)) as ListPreference
        distanceUnits.summary = distanceUnits.entry
    }

    private fun testNotification() {
        placesRepository.getRandomPlace().observe(activity as AppCompatActivity, android.arch.lifecycle.Observer {
            if (it != null) {
                placeNotificationsManager.notifyUser(it)
            }
        })
    }
}