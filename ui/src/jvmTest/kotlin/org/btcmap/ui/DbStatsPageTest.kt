package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import org.btcmap.dbstats.DbStatsLabels
import org.btcmap.sync.SyncRunStats
import org.btcmap.sync.SyncState
import org.btcmap.sync.SyncStepTiming

/**
 * The database stats page: the sync card is the host's own, and it carries the
 * last run's timings as an expandable row.
 */
class DbStatsPageTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    private val labels = DbStatsPageLabels(
        dbStats = DbStatsLabels(
            database = "Database",
            file = "File",
            version = "Version",
            size = "Size",
            tables = "Tables",
            bundles = "Bundles",
            location = "Location",
            visibleRows = "Visible",
            deletedRows = "Deleted",
            futureRows = "Future",
            rows = "Rows",
            newestUpdate = "Max updated at",
        ),
        sync = "Sync",
        source = "Source",
        state = "State",
        lastSync = "Last sync",
        syncNow = "Sync now",
        syncStateLabel = { it.toString() },
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun syncCardShowsTheLastRunAndExpandsToItsSteps() {
        val stats = SyncRunStats(
            steps = listOf(
                SyncStepTiming(SyncState.UnbundlingPlaces, 120.milliseconds),
                SyncStepTiming(SyncState.SyncingPlaces, 1500.milliseconds),
            ),
            total = 1620.milliseconds,
        )

        runComposeUiTest {
            setContent {
                DbStatsPage(
                    db = db,
                    settings = settings,
                    syncState = SyncState.Idle,
                    labels = labels,
                    onSync = {},
                    lastSyncStats = stats,
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Last sync").fetchSemanticsNodes().isNotEmpty()
            }

            val row = onNodeWithTag(statsEntryTag("sync", "Last sync"))
            row.assertTextContains("1.6 s")

            row.performClick()
            onNodeWithTag(statsDetailTag("sync", "Last sync", "UnbundlingPlaces"))
                .assertTextContains("120 ms")
            onNodeWithTag(statsDetailTag("sync", "Last sync", "SyncingPlaces"))
                .assertTextContains("1.5 s")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun syncCardHasNoLastRunRowBeforeTheFirstSync() {
        runComposeUiTest {
            setContent {
                DbStatsPage(
                    db = db,
                    settings = settings,
                    syncState = SyncState.Idle,
                    labels = labels,
                    onSync = {},
                )
            }

            // The sync card is there (it always lists the source and state)…
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("State").fetchSemanticsNodes().isNotEmpty()
            }
            // …but the last-run row only appears once a sync has run.
            onNodeWithText("Last sync").assertDoesNotExist()
        }
    }
}
