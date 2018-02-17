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

import com.bubelov.coins.BaseRobolectricTest
import com.bubelov.coins.repository.place.PlacesDb
import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.repository.place.PlacesRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

class DatabaseSyncTest : BaseRobolectricTest() {
    @Inject lateinit var placesRepository: PlacesRepository

    @Inject lateinit var databaseSync: DatabaseSync

    @Inject lateinit var placesDb: PlacesDb

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun syncing() {
        runBlocking {
            placesRepository.getPlaces("").blockingObserve()
            val placesBeforeSync = placesDb.count()
            Assert.assertTrue(placesBeforeSync > 0)
            databaseSync.start().join()
            Assert.assertNotEquals(placesBeforeSync, placesDb.count())
        }
    }
}