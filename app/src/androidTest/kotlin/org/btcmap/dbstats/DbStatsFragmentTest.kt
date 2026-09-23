package org.btcmap.dbstats

import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.settings.SettingsFragment
import org.btcmap.stats.StatsAdapter
import org.btcmap.util.AppTestCase
import org.btcmap.util.TestSyncController
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class DbStatsFragmentTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun showsRowCountsForEachTable() {
        databaseRule.db.place.insert(listOf(place(1L), place(2L)))

        launchFragment { scenario, fragment ->
            lateinit var list: RecyclerView
            scenario.onActivity {
                list = fragment.requireView().findViewById(R.id.statsList)
            }

            waitUntilOnMain {
                val adapter = list.adapter as? StatsAdapter ?: return@waitUntilOnMain false
                adapter.currentList.any { it.title == "place table" }
            }

            scenario.onActivity {
                val adapter = list.adapter as StatsAdapter
                val sections = adapter.currentList.associateBy { it.title }

                val place = sections.getValue("place table").entries.associate { it.label to it.value }
                Assert.assertEquals("2", place["Rows"])
                Assert.assertEquals("2", place["Visible"])
                Assert.assertEquals("0", place["Deleted"])

                val event = sections.getValue("event table")
                Assert.assertTrue(
                    "event section should report a Future count, was ${event.entries}",
                    event.entries.any { it.label == "Future" },
                )

                Assert.assertTrue(
                    "expected a Database section, sections were ${adapter.currentList.map { it.title }}",
                    "Database" in sections,
                )
                val version = sections.getValue("Database").entries.first { it.label == "Version" }
                Assert.assertEquals(Database.VERSION.toString(), version.value)

                Assert.assertTrue(
                    "expected a Sync section, sections were ${adapter.currentList.map { it.title }}",
                    "Sync" in sections,
                )
                val syncState = sections.getValue("Sync").entries.first { it.label == "State" }
                // The app-scoped sync is disabled for tests, so it reports Idle.
                Assert.assertEquals("Idle", syncState.value)
            }
        }
    }

    @Test
    fun syncButton_startsTheSync() {
        val controller = app.syncControllerForTesting as TestSyncController

        launchFragment { _, _ ->
            val before = controller.startedCount
            onView(withId(R.id.sync)).perform(click())
            waitUntil { controller.startedCount > before }
        }
    }

    @Test
    fun settingsButtonOpensTheScreen() {
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

            onView(withId(R.id.dbStatsButton)).perform(scrollTo(), click())

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is DbStatsFragment
            }
        }
    }

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
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            lat = 40.7128,
            lon = -74.0060,
            icon = "coffee",
            name = "Place $id",
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            localizedOpeningHours = null,
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
