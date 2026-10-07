package org.btcmap.area

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.offline.OfflineAreaState
import org.btcmap.settings.MapStyle
import org.btcmap.settings.mapStyle
import org.btcmap.ui.AREA_DESCRIPTION_TAG
import org.btcmap.ui.AREA_OFFLINE_DELETE_TAG
import org.btcmap.ui.AREA_OFFLINE_DOWNLOAD_TAG
import org.btcmap.ui.AREA_OFFLINE_STATUS_TAG
import org.btcmap.ui.AREA_OFFLINE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AreaOfflineMapTest : AreaScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun areaWithBoundingBox_showsToolbarDownloadAndHidesPanel() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, _ ->
            awaitArea()

            composeTestRule.onNodeWithContentDescription(offlineAction()).assertExists()
            composeTestRule.onNodeWithTag(AREA_OFFLINE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun areaWithoutBoundingBox_hidesToolbarDownload() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(area(bboxWest = null, bboxSouth = null, bboxEast = null, bboxNorth = null)),
        )

        withArea { _, _ ->
            awaitArea()

            composeTestRule.onNodeWithContentDescription(offlineAction()).assertDoesNotExist()
            composeTestRule.onNodeWithTag(AREA_OFFLINE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun toolbarDownload_opensDialogWithZoomSelectionAndEstimate() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, _ ->
            awaitArea()

            composeTestRule.onNodeWithContentDescription(offlineAction()).performClick()

            composeTestRule.waitUntil(5_000) {
                composeTestRule
                    .onAllNodesWithText("Maximum zoom", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule
                .onNodeWithText("Maximum zoom", substring = true)
                .assertExists()
            composeTestRule
                .onNodeWithText("Estimated size", substring = true)
                .assertExists()
        }
    }

    @Test
    fun failedDownload_offersRetryAndDelete() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { scenario, _ ->
            awaitArea()

            injectState(scenario, OfflineAreaState.Failed("boom"))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onNodeWithTag(AREA_OFFLINE_DOWNLOAD_TAG).assertExists()
            composeTestRule.onNodeWithTag(AREA_OFFLINE_STATUS_TAG)
                .assertTextContains("boom", substring = true)
        }
    }

    @Test
    fun completedDownloadForAnotherStyle_showsStyleMismatch() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        preferencesRule.prefs.mapStyle = MapStyle.Dark

        withArea { scenario, _ ->
            awaitArea()

            injectState(scenario, completeState(styleUrl = LIBERTY_STYLE_URL))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onAllNodesWithText("different map style", substring = true)
                .assertCountEquals(1)
        }
    }

    @Test
    fun completedAutoDownload_isNotFlaggedAfterTheThemeSwitches() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))
        // Dark resolves to the dark hosted style; a pack downloaded for Auto's
        // light style must still read as the same family rather than a mismatch.
        preferencesRule.prefs.mapStyle = MapStyle.Dark

        withArea { scenario, _ ->
            awaitArea()

            injectState(scenario, completeState(styleUrl = AUTO_LIGHT_STYLE_URL))

            composeTestRule.waitUntil { hasTag(AREA_OFFLINE_DELETE_TAG) }
            composeTestRule.onAllNodesWithText("different map style", substring = true)
                .assertCountEquals(0)
        }
    }

    /** Waits for the area body to render before driving its actions. */
    private fun awaitArea() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(AREA_DESCRIPTION_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun offlineAction(): String = Strings.current()["offline_map"]

    private fun hasTag(tag: String): Boolean =
        composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Publishes a pack state without going near MapLibre or the network: the
     * app's OfflinePacks only exposes states from a real download otherwise.
     */
    private fun injectState(
        scenario: ActivityScenario<Activity>,
        state: OfflineAreaState,
    ) {
        scenario.onActivity { app.offlinePacks.setStatesForTesting(mapOf(AREA_ID to state)) }
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
