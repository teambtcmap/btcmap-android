package org.btcmap.imagestats

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
import org.btcmap.settings.SettingsFragment
import org.btcmap.stats.StatsAdapter
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageStatsFragmentTest {

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    @Before
    fun setUp() {
        ImageLoadStats.reset()
    }

    @Test
    fun showsCacheSectionsAndLoadCounters() {
        launchFragment { scenario, fragment ->
            lateinit var list: RecyclerView
            scenario.onActivity {
                list = fragment.requireView().findViewById(R.id.statsList)
            }

            waitUntilOnMain {
                val adapter = list.adapter as? StatsAdapter ?: return@waitUntilOnMain false
                adapter.currentList.any { it.title == "Memory cache" }
            }

            scenario.onActivity {
                val adapter = list.adapter as StatsAdapter
                val sections = adapter.currentList.associateBy { it.title }
                val titles = adapter.currentList.map { it.title }
                Assert.assertTrue("missing memory section, was $titles", "Memory cache" in sections)
                Assert.assertTrue("missing disk section, was $titles", "Disk cache" in sections)
                Assert.assertTrue("missing loads section, was $titles", "Loads" in sections)

                val loads = sections.getValue("Loads").entries.associate { it.label to it.value }
                Assert.assertEquals("0", loads["Requests"])
                Assert.assertEquals("0", loads["Errors"])
                Assert.assertEquals("0", loads["Cancels"])

                val memorySize = sections.getValue("Memory cache").entries
                    .first { it.label == "Size" }.value
                Assert.assertTrue(
                    "missing memory size, was $memorySize",
                    memorySize.contains(" of "),
                )
                Assert.assertTrue(
                    "missing cache location",
                    sections.getValue("Disk cache").entries.any { it.label == "Location" },
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

            onView(withId(R.id.imageStatsButton)).perform(scrollTo(), click())

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is ImageStatsFragment
            }
        }
    }

    private fun launchFragment(block: (ActivityScenario<Activity>, ImageStatsFragment) -> Unit) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: ImageStatsFragment
            scenario.onActivity { activity ->
                fragment = ImageStatsFragment()
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainerView, fragment, IMAGE_STATS_TAG)
                }
                activity.supportFragmentManager.executePendingTransactions()
            }
            block(scenario, fragment)
        }
    }

    private companion object {
        const val IMAGE_STATS_TAG = "image-stats"
    }
}
