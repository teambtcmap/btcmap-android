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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.content.SharedPreferences
import android.content.res.Resources
import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.model.Currency
import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.currency.CurrenciesRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.DistanceUnitsLiveData
import com.bubelov.coins.util.PlaceNotificationManager
import com.bubelov.coins.util.SelectedCurrencyLiveData
import com.bubelov.coins.util.emptyPlace
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class SettingsViewModelTest {
    @JvmField @Rule val instantExecutor = InstantTaskExecutorRule()

    @Mock private lateinit var selectedCurrencyLiveData: SelectedCurrencyLiveData
    @Mock private lateinit var distanceUnitsLiveData: DistanceUnitsLiveData
    @Mock private lateinit var databaseSync: DatabaseSync
    @Mock private lateinit var syncLogsRepository: SyncLogsRepository
    @Mock private lateinit var placesRepository: PlacesRepository
    @Mock private lateinit var currenciesRepository: CurrenciesRepository
    @Mock private lateinit var notificationManager: PlaceNotificationManager
    @Mock private lateinit var resources: Resources
    @Mock private lateinit var preferences: SharedPreferences
    private lateinit var model: SettingsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        model = SettingsViewModel(
            selectedCurrencyLiveData,
            placesRepository,
            currenciesRepository,
            distanceUnitsLiveData,
            databaseSync,
            syncLogsRepository,
            notificationManager,
            resources,
            preferences
        )
    }

    @Test
    fun returnsCurrencies() {
        val currencies = listOf(
            Currency("ABC"),
            Currency("BTC"),
            Currency("LTC")
        )

        `when`(currenciesRepository.getAllCurrencies()).thenReturn(currencies)

        `when`(placesRepository.countByCurrency(currencies[0])).thenReturn(1)
        `when`(placesRepository.countByCurrency(currencies[1])).thenReturn(2)
        `when`(placesRepository.countByCurrency(currencies[2])).thenReturn(3)

        runBlocking {
            model.showCurrencySelector().join()
            val rows = model.currencySelectorItems.value!!

            Assert.assertEquals(rows.size, 3)
            Assert.assertTrue(rows[0].places == 3)
            Assert.assertTrue(rows[1].places == 2)
            Assert.assertTrue(rows[2].places == 1)

            verify(currenciesRepository).getAllCurrencies()
            verifyNoMoreInteractions(currenciesRepository)

            currencies.forEach {
                verify(placesRepository).countByCurrency(it)
            }

            verifyNoMoreInteractions(placesRepository)
        }
    }

    @Test
    fun callsSync() = runBlocking {
        model.syncDatabase().join()
        verify(databaseSync).sync()
    }

    @Test
    fun returnsSyncLogs() = runBlocking {
        `when`(syncLogsRepository.all()).thenReturn(listOf(SyncLogEntry(0, 10)))

        model.showSyncLogs().join()
        val logs = model.syncLogs.value!!

        Assert.assertEquals(1, logs.size)
        verify(syncLogsRepository).all()
        verifyNoMoreInteractions(syncLogsRepository)
    }

    @Test
    fun sendsRandomPlaceNotification() = runBlocking {
        `when`(placesRepository.findRandom()).thenReturn(
            emptyPlace().copy(
                id = 1,
                name = "Random Place"
            )
        )

        model.testNotification().join()

        verify(placesRepository).findRandom()
        verifyNoMoreInteractions(placesRepository)
    }
}