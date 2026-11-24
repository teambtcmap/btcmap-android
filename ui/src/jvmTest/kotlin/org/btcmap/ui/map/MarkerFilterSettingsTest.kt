package org.btcmap.ui.map

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.ui.testDatabase
import org.btcmap.ui.testSettings

/** The marker-kind toggles' stored state. */
class MarkerFilterSettingsTest {

    @Test
    fun defaultsToEveryKindExceptExchanges() {
        val settings = testSettings(testDatabase())

        assertEquals(
            MarkerKind.entries.toSet() - MarkerKind.Exchanges,
            settings.visibleMarkerKinds,
        )
    }

    @Test
    fun persistsTheVisibleKindsAcrossSettingsInstances() = runBlocking {
        val db = testDatabase()
        val settings = testSettings(db)

        settings.visibleMarkerKinds = setOf(MarkerKind.Events, MarkerKind.Notes)

        // The write is asynchronous, so wait for it to reach the database before
        // loading a second Settings over the same file.
        withTimeout(2_000) {
            while (db.preference.select(KEY_VISIBLE_MARKER_KINDS) == null) delay(10)
        }
        val reloaded = testSettings(db)

        assertEquals(setOf(MarkerKind.Events, MarkerKind.Notes), reloaded.visibleMarkerKinds)
    }

    @Test
    fun anEmptySelectionIsDistinctFromNoStoredValue() = runBlocking {
        val db = testDatabase()
        val settings = testSettings(db)

        settings.visibleMarkerKinds = emptySet()

        withTimeout(2_000) {
            while (db.preference.select(KEY_VISIBLE_MARKER_KINDS) == null) delay(10)
        }
        val reloaded = testSettings(db)

        assertEquals(emptySet(), reloaded.visibleMarkerKinds)
    }
}
