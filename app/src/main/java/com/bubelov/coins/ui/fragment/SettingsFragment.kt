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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
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
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert
import javax.inject.Inject

class SettingsFragment : PreferenceFragment() {
    @Inject lateinit var modelFactory: ViewModelProvider.Factory

    private val model by lazy {
        ViewModelProviders.of(settingsActivity, modelFactory)[SettingsViewModel::class.java]
    }

    private val settingsActivity by lazy { activity as SettingsActivity }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        val currencyPreference = findPreference(getString(R.string.pref_currency_key)) as Preference

        model.selectedCurrencyLiveData.observe(settingsActivity, Observer {
            currencyPreference.summary = it
        })

        val distanceUnitsPreference =
            findPreference(getString(R.string.pref_distance_units_key)) as ListPreference

        model.distanceUnitsLiveData.observe(settingsActivity, Observer {
            distanceUnitsPreference.summary = distanceUnitsPreference.entry
        })
    }

    override fun onPreferenceTreeClick(
        preferenceScreen: PreferenceScreen,
        preference: Preference
    ): Boolean {
        when (preference.key) {
            getString(R.string.pref_currency_key) -> showCurrencySelector()
            getString(R.string.pref_sync_database_key) -> model.syncDatabase()
            getString(R.string.pref_show_sync_log_key) -> showSyncLog()
            getString(R.string.pref_test_notification_key) -> launch(UI) { model.testNotification() }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    private fun showCurrencySelector() {
        val rowsLiveData = model.getCurrencySelectorRows()

        rowsLiveData.observe(
            settingsActivity,
            object : Observer<List<SettingsViewModel.CurrencySelectorRow>> {
                override fun onChanged(rows: List<SettingsViewModel.CurrencySelectorRow>?) {
                    if (rows != null && !rows.isEmpty()) {
                        showCurrencySelector(rows)
                    }

                    rowsLiveData.removeObserver(this)
                }
            })
    }

    private fun showCurrencySelector(rows: List<SettingsViewModel.CurrencySelectorRow>) {
        alert {
            items(rows.map {
                "${it.currency} (${it.places} ${resources.getQuantityString(
                    R.plurals.places,
                    it.places
                )})"
            }, onItemSelected = { _, _, index ->
                preferenceManager.sharedPreferences
                    .edit()
                    .putString(getString(R.string.pref_currency_key), rows[index].currency)
                    .apply()
            })
        }.apply {
            titleResource = R.string.currency
            show()
        }
    }

    private fun showSyncLog() {
        model.getSyncLogs().observe(settingsActivity, object : Observer<List<String>> {
            override fun onChanged(logs: List<String>?) {
                if (logs != null && !logs.isEmpty()) {
                    alert { items(logs, onItemSelected = { _, _, _ -> }) }.show()
                }

                model.getSyncLogs().removeObserver(this)
            }
        })
    }
}