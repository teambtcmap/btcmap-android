package org.btcmap.imagestats

import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.settings.SettingsFragment
import org.btcmap.ui.STATS_LIST_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageStatsFragmentTest : AppTestCase() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        ImageLoadStats.reset()
    }

    private fun showsText(text: String): Boolean =
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun showsCacheSectionsAndLoadCounters() {
        launchFragment { _, _ ->
            composeTestRule.waitUntil(5_000) { showsText("Memory cache") }
            composeTestRule.onNodeWithText("Memory cache").assertIsDisplayed()

            composeTestRule.onNodeWithTag(STATS_LIST_TAG).performScrollToNode(hasText("Loads"))
            composeTestRule.onNodeWithText("Loads").assertIsDisplayed()

            composeTestRule.onNodeWithTag(STATS_LIST_TAG).performScrollToNode(hasText("Requests"))
            composeTestRule.onNodeWithText("Requests").assertIsDisplayed()
        }
    }

    @Test
    fun refreshButtonReReadsTheCounters() {
        launchFragment { scenario, fragment ->
            composeTestRule.waitUntil(5_000) { showsText("Memory cache") }

            // A load that happens after the screen opened only shows up once
            // the refresh action re-reads the counters.
            scenario.onActivity { ImageLoadStats.recordStart() }
            scenario.onActivity {
                val toolbar = fragment.requireView().findViewById<MaterialToolbar>(R.id.topAppBar)
                toolbar.menu.performIdentifierAction(R.id.refresh, 0)
            }

            composeTestRule.onNodeWithTag(STATS_LIST_TAG).performScrollToNode(hasText("Requests"))
            composeTestRule.onNodeWithText("Requests").assertIsDisplayed()
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

            composeTestRule
                .onNodeWithText(context.getString(R.string.image_stats))
                .performScrollTo()
                .performClick()

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
