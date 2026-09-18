package org.btcmap.dbstats

import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.place.Place
import org.btcmap.settings.SettingsFragment
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class DbStatsFragmentTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    @Test
    fun showsRowCountsForEachTable() {
        databaseRule.db.place.insert(listOf(place(1L), place(2L)))

        launchFragment { scenario, fragment ->
            lateinit var list: RecyclerView
            scenario.onActivity {
                list = fragment.requireView().findViewById(R.id.statsList)
            }

            waitUntilOnMain {
                val adapter = list.adapter as? DbStatsAdapter ?: return@waitUntilOnMain false
                adapter.currentList.any { it is DbStatsItem.Entry && it.title == "place" }
            }

            scenario.onActivity {
                val adapter = list.adapter as DbStatsAdapter
                val entries = adapter.currentList.filterIsInstance<DbStatsItem.Entry>()
                val place = entries.first { it.title == "place" }
                Assert.assertTrue(
                    "place detail should report two rows, was '${place.detail}'",
                    place.detail.contains("rows: 2"),
                )
                Assert.assertTrue(
                    "place detail should report two visible rows, was '${place.detail}'",
                    place.detail.contains("visible: 2"),
                )
                Assert.assertTrue(
                    "place detail should report no deleted rows, was '${place.detail}'",
                    place.detail.contains("deleted: 0"),
                )
                val event = entries.first { it.title == "event" }
                Assert.assertTrue(
                    "event detail should report a future count, was '${event.detail}'",
                    event.detail.contains("future:"),
                )
                Assert.assertTrue(
                    "expected a Databases section, entries were ${entries.map { it.title }}",
                    adapter.currentList.any {
                        it is DbStatsItem.Header && it.title == "Databases"
                    },
                )
            }
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
            bundled = false,
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
