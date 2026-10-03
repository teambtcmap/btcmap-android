package org.btcmap.dbstats

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.settings.SettingsFragment
import org.btcmap.sync.SyncState
import org.btcmap.ui.STATS_LIST_TAG
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
    fun showsRowCountsForEachTable() = runBlocking<Unit> {
        databaseRule.db.place.insert(listOf(place(1L), place(2L)))

        launchFragment { _, _ ->
            waitForSection("place table")
            entry("table:place", "Rows").assertTextContains("2")
            entry("table:place", "Visible").assertTextContains("2")
            entry("table:place", "Deleted").assertTextContains("0")

            waitForSection("event table")
            entry("table:event", "Future").assertExists()

            waitForSection("Database")
            entry("database", "Version").assertTextContains(Database.VERSION.toString())

            waitForSection("Sync")
            entry("sync", "State").assertTextContains("Idle")
        }
    }

    @Test
    fun syncButton_startsTheSync() = runBlocking<Unit> {
        val controller = app.syncControllerForTesting as TestSyncController

        launchFragment { _, _ ->
            val before = controller.startedCount
            onView(withId(R.id.sync)).perform(click())
            waitUntil { controller.startedCount > before }
        }
    }

    @Test
    fun showsBundleStatsForEachSnapshot() = runBlocking<Unit> {
        launchFragment { _, _ ->
            waitForSection("place bundle")

            entry("bundle:place", "Location")
                .assertTextContains("assets/bundled-places.json")
            entry("bundle:place", "Visible").assertExists()
            entry("bundle:place", "Deleted").assertExists()
        }
    }

    @Test
    fun refreshesRowCountsWhenTheSyncFinishes() = runBlocking<Unit> {
        val controller = app.syncControllerForTesting as TestSyncController
        databaseRule.db.place.insert(listOf(place(1L)))

        launchFragment { scenario, _ ->
            waitForSection("place table")
            entry("table:place", "Rows").assertTextContains("1")

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
                    scrollToSection("place table")
                    entry("table:place", "Rows").assertTextContains("2")
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
                    replace<SettingsFragment>(R.id.fragmentContainerView, null)
                }
                activity.supportFragmentManager.executePendingTransactions()
            }

            composeTestRule
                .onNodeWithText(app.getString(R.string.database_stats))
                .performScrollTo()
                .performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is DbStatsFragment
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

    private fun launchFragment(block: (ActivityScenario<Activity>, DbStatsFragment) -> Unit) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: DbStatsFragment
            scenario.onActivity { activity ->
                fragment = DbStatsFragment()
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
