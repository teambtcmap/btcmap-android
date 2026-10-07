package org.btcmap.area

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.ui.AREA_DESCRIPTION_TAG
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AreaLoadErrorHandlingTest : AreaScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun loadSuccess_showsContent() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(listOf(area()))

        withArea { _, _ ->
            awaitArea()

            composeTestRule.onNodeWithText("Grand Paris").assertExists()
            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG).assertExists()
        }
    }

    @Test
    fun missingCachedArea_showsErrorDialogAndClosesScreenOnDismiss() = runBlocking<Unit> {
        withArea(addToBackStack = true) { scenario, _ ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            val ok = Strings.current()["ok"]
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText(ok).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(ok).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(AREA_TAG) == null
            }
        }
    }

    @Test
    fun cachedAreaWithoutGeometry_showsContent() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher()
        databaseRule.db.area.insert(
            listOf(
                area(
                    bboxWest = null,
                    bboxSouth = null,
                    bboxEast = null,
                    bboxNorth = null,
                    geoJson = null,
                ),
            ),
        )

        withArea { _, _ ->
            awaitArea()

            composeTestRule.onNodeWithText("Grand Paris").assertExists()
            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG).assertExists()
        }
    }

    @Test
    fun issuesFailure_keepsContentVisibleWithoutErrorDialog() = runBlocking<Unit> {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = """{"message":"boom"}""",
            issuesCode = 500,
        )
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.event.insert(
            listOf(event(id = 1, name = "Meetup", startsAt = "2999-01-01T10:00:00Z")),
        )

        withArea { scenario, _ ->
            awaitArea()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText("Meetup").fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag(AREA_DESCRIPTION_TAG).assertExists()

            // The issues failure must not interrupt the cached screen with an
            // error dialog; a shown dialog would take the window focus.
            scenario.onActivity { activity ->
                Assert.assertTrue(
                    "issues failure must not show an error dialog",
                    activity.window.decorView.hasWindowFocus(),
                )
            }
        }
    }

    /** Waits for the area body to render. */
    private fun awaitArea() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(AREA_DESCRIPTION_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
