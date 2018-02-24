/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.ui.fragment

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import com.bubelov.coins.R
import com.bubelov.coins.ui.activity.SettingsActivity
import com.bubelov.coins.ui.viewmodel.SettingsViewModel
import dagger.android.AndroidInjection
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert
import java.util.*
import javax.inject.Inject

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var model: SettingsViewModel

    override fun onAttach(context: Context) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(getSettingsActivity(), modelFactory)[SettingsViewModel::class.java]
        addPreferencesFromResource(R.xml.preferences)
        updateSummaries()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onPreferenceTreeClick(
        preferenceScreen: PreferenceScreen,
        preference: Preference
    ): Boolean {
        when (preference.key) {
            getString(R.string.pref_currency_key) -> showCurrencySelector()
            getString(R.string.pref_sync_database_key) -> model.databaseSync.start()
            getString(R.string.pref_show_sync_log_key) -> showSyncLog()
            getString(R.string.pref_test_notification_key) -> testNotification()
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        updateSummaries()
    }

    private fun showSyncLog() {
        val logs = model.syncLogsRepository.syncLogs
            .reversed()
            .map { "Date: ${Date(it.time)}, Affected places: ${it.affectedPlaces}" }

        if (logs.isEmpty()) {
            alert(message = "Logs are empty").show()
        } else {
            alert { items(logs, onItemSelected = { _, _, _ -> }) }.show()
        }
    }

    private fun updateSummaries() {
        updateCurrencySummary()
        updateDistanceUnitsSummary()
    }

    private fun updateCurrencySummary() {
        val currency = findPreference(getString(R.string.pref_currency_key)) as Preference

        currency.summary = preferenceManager.sharedPreferences.getString(
            getString(R.string.pref_currency_key),
            "BTC"
        )
    }

    private fun updateDistanceUnitsSummary() {
        val distanceUnits = findPreference(getString(R.string.pref_distance_units_key)) as ListPreference
        distanceUnits.summary = distanceUnits.entry
    }

    private fun showCurrencySelector() {
        model.placesRepository.getCurrenciesToPlacesMap()
            .observe(getSettingsActivity(), android.arch.lifecycle.Observer { map ->
                if (map == null) {
                    return@Observer
                }

                val currencies = map.keys.sortedBy { -map[it]!!.size }

                val titles = currencies.map {
                    "$it (${map[it]!!.size} ${resources.getQuantityString(
                        R.plurals.places,
                        map[it]!!.size
                    ).toLowerCase()})"
                }

                alert {
                    items(titles, onItemSelected = { _, _, index ->
                        preferenceManager.sharedPreferences
                            .edit()
                            .putString(getString(R.string.pref_currency_key), currencies[index])
                            .apply()
                    })
                }.apply {
                    titleResource = R.string.currency
                    show()
                }
            })
    }

    private fun testNotification() = launch(UI) {
        val randomPlace = async { model.placesRepository.findRandom() }.await()

        if (randomPlace != null) {
            model.placeNotificationsManager.issueNotification(randomPlace)
        }
    }

    private fun getSettingsActivity() = activity as SettingsActivity
}