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

package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.SharedPreferences
import android.content.res.Resources
import com.bubelov.coins.R
import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.model.Currency
import com.bubelov.coins.repository.currency.CurrenciesRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.DistanceUnitsLiveData
import com.bubelov.coins.util.PlaceNotificationManager
import com.bubelov.coins.util.SelectedCurrencyLiveData
import kotlinx.coroutines.experimental.launch
import java.util.*
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    selectedCurrencyLiveData: SelectedCurrencyLiveData,
    private val placesRepository: PlacesRepository,
    private val currenciesRepository: CurrenciesRepository,
    distanceUnitsLiveData: DistanceUnitsLiveData,
    private val databaseSync: DatabaseSync,
    private val syncLogsRepository: SyncLogsRepository,
    private val placeNotificationsManager: PlaceNotificationManager,
    private val resources: Resources,
    private val preferences: SharedPreferences
) : ViewModel() {
    val selectedCurrency = selectedCurrencyLiveData

    val currencySelectorItems = MutableLiveData<List<CurrencySelectorItem>>()

    val distanceUnits = distanceUnitsLiveData

    val syncLogs = MutableLiveData<List<String>>()

    fun showCurrencySelector() = launch {
        val allCurrencies = currenciesRepository.getAllCurrencies()
        val currenciesToPlaces = mutableListOf<Pair<Currency, Int>>()

        allCurrencies.forEach { currency ->
            currenciesToPlaces.add(Pair(currency, placesRepository.countByCurrency(currency)))
        }

        val items = currenciesToPlaces.map {
            CurrencySelectorItem(
                currency = it.first,
                places = it.second,
                title = "${it.first.code} (${it.second} ${resources.getQuantityString(
                    R.plurals.places,
                    it.second
                )})"
            )
        }

        currencySelectorItems.postValue(items.sortedByDescending { it.places })
    }

    fun selectCurrency(currency: Currency) {
        preferences
            .edit()
            .putString(resources.getString(R.string.pref_currency_key), currency.code)
            .apply()
    }

    fun syncDatabase() = launch {
        databaseSync.sync()
    }

    fun showSyncLogs() = launch {
        val logs = syncLogsRepository.all()
            .reversed()
            .map { "Date: ${Date(it.time)}, Affected places: ${it.affectedPlaces}" }

        syncLogs.postValue(logs)
    }

    fun testNotification() = launch {
        val randomPlace = placesRepository.findRandom()

        if (randomPlace != null) {
            placeNotificationsManager.issueNotification(randomPlace)
        }
    }

    data class CurrencySelectorItem(
        val currency: Currency,
        val places: Int,
        val title: String
    )
}