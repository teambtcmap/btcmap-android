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

package com.bubelov.coins

import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.db.sync.SyncScheduler
import com.bubelov.coins.repository.Result
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.PlaceNotificationManager
import com.bubelov.coins.util.emptyPlace
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class DatabaseSyncTest {
    @Mock private lateinit var placesRepository: PlacesRepository
    @Mock private lateinit var placeNotificationManager: PlaceNotificationManager
    @Mock private lateinit var syncLogsRepository: SyncLogsRepository
    @Mock private lateinit var databaseSyncScheduler: SyncScheduler

    private val databaseSync: DatabaseSync

    init {
        MockitoAnnotations.initMocks(this)

        databaseSync = DatabaseSync(
            placesRepository,
            placeNotificationManager,
            syncLogsRepository,
            databaseSyncScheduler
        )
    }

    @Test
    fun handleSuccessfulSync() {
        val fetchedPlaces = listOf(
            emptyPlace().copy(id = 1),
            emptyPlace().copy(id = 2),
            emptyPlace().copy(id = 3)
        )

        `when`(placesRepository.fetchNewPlaces())
            .thenReturn(Result.Success(fetchedPlaces))

        runBlocking { databaseSync.sync() }

        verify(placeNotificationManager).issueNotificationsIfNecessary(fetchedPlaces)
        verify(syncLogsRepository).insert(any())
        verify(databaseSyncScheduler).scheduleNextSync()
    }
}