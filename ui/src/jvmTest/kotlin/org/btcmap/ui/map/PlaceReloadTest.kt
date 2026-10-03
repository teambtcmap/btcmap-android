package org.btcmap.ui.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place

/**
 * The selected place is re-read when a sync bumps the reload key, instead of
 * leaving the copy captured at selection time up. Ported from the app's
 * instrumented `MapPlaceDeepLinkRefreshTest` when the Views sheet went with the
 * map swap.
 */
@OptIn(ExperimentalTestApi::class)
class PlaceReloadTest {

    @Test
    fun reloadsTheSelectedRowWhenTheKeyChanges() {
        val db = database()
        runBlocking { db.place.insert(listOf(place(name = "Old name"))) }

        val selected = mutableStateOf<Place?>(null)
        val key = mutableStateOf(0)
        var shownName: String? = null

        runComposeUiTest {
            setContent {
                shownName = rememberReloadedPlace(selected.value, db, key.value)?.name
            }

            runOnIdle { selected.value = runBlocking { db.place.selectById(PLACE_ID) } }
            waitUntil(timeoutMillis = 5_000) { shownName == "Old name" }

            // The sync rewrites the row in place, then the host bumps the key.
            runBlocking { db.place.insert(listOf(place(name = "New name"))) }
            runOnIdle { key.value = 1 }
            waitUntil(timeoutMillis = 5_000) { shownName == "New name" }
        }

        assertEquals("New name", shownName)
    }

    private fun database(): Database =
        Database(BundledSQLiteDriver(), Files.createTempFile("place-reload", ".db").toString())

    private fun place(name: String) = Place(
        id = PLACE_ID,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        lat = 0.0,
        lon = 0.0,
        icon = "storefront",
        name = name,
        localizedName = null,
        verifiedAt = null,
        address = null,
        openingHours = null,
        phone = null,
        website = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
        requiredAppUrl = null,
        boostedUntil = null,
        comments = null,
        telegram = null,
        osmId = null,
    )

    private companion object {
        const val PLACE_ID = 7L
    }
}
