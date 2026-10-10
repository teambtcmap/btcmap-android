package org.btcmap.dbstats

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.nav.AppRootFragment
import org.btcmap.sync.SyncState
import org.btcmap.ui.AppRoute
import org.btcmap.ui.STATS_LIST_TAG
import org.btcmap.ui.statsDetailTag
import org.btcmap.ui.statsEntryTag
import org.btcmap.util.AppTestCase
import org.btcmap.util.TestSyncController
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class DbStatsFragmentTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun summarisesEveryTableInOneCard() = runBlocking<Unit> {
        databaseRule.db.place.insert(listOf(place(1L), place(2L)))

        launchFragment { _, _ ->
            waitForSection("Tables")
            entry("tables", "place").assertTextContains("2/2/0")
            entry("tables", "event").assertExists()

            waitForSection("Database")
            entry("database", "Version").assertTextContains(Database.VERSION.toString())

            waitForSection("Sync")
            entry("sync", "State").assertTextContains("Idle")
        }
    }

    @Test
    fun expandsATableRowToShowTheBreakdown() = runBlocking<Unit> {
        databaseRule.db.place.insert(listOf(place(1L)))

        launchFragment { _, _ ->
            waitForSection("Tables")
            entry("tables", "place").performClick()

            detail("tables", "place", "Rows").assertTextContains("1")
            detail("tables", "place", "Visible").assertTextContains("1")
            detail("tables", "place", "Deleted").assertTextContains("0")
            detail("tables", "place", "Max updated at").assertExists()
        }
    }

    @Test
    fun syncButton_startsTheSync() = runBlocking<Unit> {
        val controller = app.syncControllerForTesting as TestSyncController

        launchFragment { _, _ ->
            val before = controller.startedCount
            composeTestRule.onNodeWithContentDescription("Sync").performClick()
            waitUntil { controller.startedCount > before }
        }
    }

    @Test
    fun showsBundleStatsForEachSnapshot() = runBlocking<Unit> {
        launchFragment { _, _ ->
            waitForSection("Bundles")
            entry("bundles", "place").performClick()

            detail("bundles", "place", "Location")
                .assertTextContains("assets/bundled-places.json")
            detail("bundles", "place", "Visible").assertExists()
            detail("bundles", "place", "Deleted").assertExists()
        }
    }

    @Test
    fun refreshesRowCountsWhenTheSyncFinishes() = runBlocking<Unit> {
        val controller = app.syncControllerForTesting as TestSyncController
        databaseRule.db.place.insert(listOf(place(1L)))

        launchFragment { scenario, _ ->
            waitForSection("Tables")
            entry("tables", "place").assertTextContains("1/1/0")

            // The sync writes a new row, reports that it is running, and only
            // then finishes; the counts must be re-read when it does.
            runBlocking { databaseRule.db.place.insert(listOf(place(2L))) }
            scenario.onActivity { controller.setState(SyncState.SyncingPlaces) }
            waitForSection("Sync")
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText("Syncing places").fetchSemanticsNodes()
                    .isNotEmpty()
            }

            scenario.onActivity { controller.setState(SyncState.Idle) }
            composeTestRule.waitUntil(5_000) {
                runCatching {
                    scrollToSection("Tables")
                    entry("tables", "place").assertTextContains("2/2/0")
                }.isSuccess
            }
        }
    }

    @Test
    fun settingsButtonOpensTheScreen() = runBlocking<Unit> {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var activity: Activity
            scenario.onActivity {
                activity = it
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainerView, AppRootFragment.create(AppRoute.Settings))
                }
                activity.supportFragmentManager.executePendingTransactions()
            }

            composeTestRule
                .onNodeWithText(Strings.current()["database_stats"])
                .performScrollTo()
                .performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AppRootFragment
            }
        }
    }

    /** Scrolls to the card titled [title], waiting for the async load to land. */
    private fun waitForSection(title: String) {
        composeTestRule.waitUntil(5_000) {
            runCatching { scrollToSection(title) }.isSuccess
        }
    }

    private fun scrollToSection(title: String) {
        composeTestRule.onNodeWithTag(STATS_LIST_TAG).performScrollToNode(hasText(title))
    }

    private fun entry(sectionKey: String, label: String) =
        composeTestRule.onNodeWithTag(statsEntryTag(sectionKey, label))

    private fun detail(sectionKey: String, entryLabel: String, detailLabel: String) =
        composeTestRule.onNodeWithTag(statsDetailTag(sectionKey, entryLabel, detailLabel))

    private fun launchFragment(block: (ActivityScenario<Activity>, AppRootFragment) -> Unit) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: AppRootFragment
            scenario.onActivity { activity ->
                fragment = AppRootFragment.create(AppRoute.DbStats)
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainerView, fragment, DB_STATS_TAG)
                }
                activity.supportFragmentManager.executePendingTransactions()
            }
            block(scenario, fragment)
        }
    }

    private fun place(id: Long): Place {
        return Place(
            id = id,
            updatedAt = Instant.parse("2024-01-01T10:00:00Z"),
            lat = 40.7128,
            lon = -74.0060,
            icon = "coffee",
            name = "Place $id",
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
    }

    private companion object {
        const val DB_STATS_TAG = "db-stats"
    }
}
