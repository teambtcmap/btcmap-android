package org.btcmap.area

import android.view.View
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.offline.OfflineAreaState
import org.btcmap.settings.MapStyle
import org.btcmap.settings.mapStyle
import org.btcmap.ui.AREA_OFFLINE_DELETE_TAG
import org.btcmap.ui.AREA_OFFLINE_DOWNLOAD_TAG
import org.btcmap.ui.AREA_OFFLINE_STATUS_TAG
import org.btcmap.ui.AREA_OFFLINE_TAG
import org.btcmap.util.waitUntilOnMain
import org.hamcrest.Matchers.containsString
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AreaOfflineMapTest : AreaScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun areaWithBoundingBox_showsToolbarDownloadAndHidesPanel() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).check(matches(isDisplayed()))
            composeTestRule.onNodeWithTag(AREA_OFFLINE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun areaWithoutBoundingBox_hidesToolbarDownload() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(bboxWest = null, bboxSouth = null, bboxEast = null, bboxNorth = null)),
        )

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).check(doesNotExist())
            composeTestRule.onNodeWithTag(AREA_OFFLINE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun toolbarDownload_opensDialogWithZoomSelectionAndEstimate() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            onView(withId(R.id.download)).perform(click())

            onView(withId(R.id.zoom)).inRoot(isDialog())
                .check(matches(withText(containsString("Maximum zoom"))))
            onView(withId(R.id.zoom_slider)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withId(R.id.estimate)).inRoot(isDialog())
                .check(matches(withText(containsString("Estimated size"))))
        }
    }

    @Test
    fun failedDownload_offersRetryAndDelete() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { scenario, fragment ->
            waitUntilOnMain { !fragment.requireView().findViewById<View>(R.id.loading).isVisible }

            injectState(scenario, OfflineAreaState.Failed("boom"))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onNodeWithTag(AREA_OFFLINE_DOWNLOAD_TAG).assertExists()
            composeTestRule.onNodeWithTag(AREA_OFFLINE_STATUS_TAG)
                .assertTextContains("boom", substring = true)
        }
    }

    @Test
    fun completedDownloadForAnotherStyle_showsStyleMismatch() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        preferencesRule.prefs.mapStyle = MapStyle.Dark

        withArea { scenario, fragment ->
            waitUntilOnMain { !fragment.requireView().findViewById<View>(R.id.loading).isVisible }

            injectState(scenario, completeState(styleUrl = LIBERTY_STYLE_URL))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onAllNodesWithText("different map style", substring = true)
                .assertCountEquals(1)
        }
    }

    @Test
    fun completedAutoDownload_isNotFlaggedAfterTheThemeSwitches() {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        // Dark resolves to the dark hosted style; a pack downloaded for Auto's
        // light style must still read as the same family rather than a mismatch.
        preferencesRule.prefs.mapStyle = MapStyle.Dark

        withArea { scenario, fragment ->
            waitUntilOnMain { !fragment.requireView().findViewById<View>(R.id.loading).isVisible }

            injectState(scenario, completeState(styleUrl = AUTO_LIGHT_STYLE_URL))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onAllNodesWithText("different map style", substring = true)
                .assertCountEquals(0)
        }
    }

    private fun hasTag(tag: String): Boolean =
        composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Publishes a pack state without going near MapLibre or the network: the
     * app's OfflineMaps only exposes states from a real download otherwise.
     */
    private fun injectState(
        scenario: ActivityScenario<Activity>,
        state: OfflineAreaState,
    ) {
        scenario.onActivity { app.offlineMaps.setStatesForTesting(mapOf(AREA_ID to state)) }
    }

    private fun completeState(styleUrl: String) = OfflineAreaState.Complete(
        bytes = 1234L,
        maxZoom = 12,
        styleUrl = styleUrl,
    )

    private companion object {
        const val AREA_ID = 1L
        const val AUTO_LIGHT_STYLE_URL = "https://static.btcmap.org/map-styles/light.json"
        const val LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    }
}
