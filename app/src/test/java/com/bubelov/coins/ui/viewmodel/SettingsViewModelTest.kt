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
import android.arch.lifecycle.MutableLiveData
import com.bubelov.coins.blockingObserve
import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.model.Place
import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.DistanceUnitsLiveData
import com.bubelov.coins.util.PlaceNotificationManager
import com.bubelov.coins.util.SelectedCurrencyLiveData
import com.bubelov.coins.util.emptyPlace
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
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
    @Mock private lateinit var notificationManager: PlaceNotificationManager

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun returnsCurrencies() {
        val model = createModel()

        `when`(placesRepository.all()).thenReturn(MutableLiveData<List<Place>>().apply {
            value = listOf(
                emptyPlace().copy(id = 1, name = "Cafe", currencies = arrayListOf("BTC")),
                emptyPlace().copy(id = 2, name = "Bar", currencies = arrayListOf("BTC", "LTC")),
                emptyPlace().copy(id = 3, name = "Black Market", currencies = arrayListOf("ZEC"))
            )
        })

        val rows = model.getCurrencySelectorRows().blockingObserve()
        Assert.assertEquals(rows.size, 3)
        Assert.assertTrue(rows.any { it.places == 2 })
        verify(placesRepository).all()
        verifyNoMoreInteractions(placesRepository)
    }

    @Test
    fun callsSync() {
        val model = createModel()
        runBlocking { model.syncDatabase() }
        verify(databaseSync).sync()
    }

    @Test
    fun returnsSyncLogs() {
        val model = createModel()
        `when`(syncLogsRepository.all()).thenReturn(listOf(SyncLogEntry(0, 10)))
        val logs = model.getSyncLogs().blockingObserve()
        Assert.assertEquals(1, logs.size)
        verify(syncLogsRepository).all()
        verifyNoMoreInteractions(syncLogsRepository)
    }

    @Test
    fun sendsRandomPlaceNotification() {
        val model = createModel()
        `when`(placesRepository.findRandom()).thenReturn(
            emptyPlace().copy(
                id = 1,
                name = "Random Place"
            )
        )
        runBlocking { model.testNotification() }
        verify(placesRepository).findRandom()
        verifyNoMoreInteractions(placesRepository)
    }

    private fun createModel(): SettingsViewModel {
        return SettingsViewModel(
            selectedCurrencyLiveData,
            placesRepository,
            distanceUnitsLiveData,
            databaseSync,
            syncLogsRepository,
            notificationManager
        )
    }
}