package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.btcmap.db.table.area.Area
import org.btcmap.i18n.Strings
import org.btcmap.settings.MapStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

/** The area admin page: verify, name edit and description edit wire to the host. */
class AreaAdminPageTest {

    private fun area() = Area(
        id = 42,
        name = "Grand Paris",
        type = "community",
        urlAlias = "grand-paris",
        icon = null,
        iconWide = null,
        websiteUrl = "https://btcmap.org/community/grand-paris",
        description = null,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = null,
    )

    private fun labels(): AppLabels = appLabels(
        strings = Strings.current(),
        currentStyle = MapStyle.Auto,
        formatNumber = { value, _ -> value.toString() },
        formatBytes = { "$it B" },
        formatFeedDate = { it },
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun verifyAction_stampsTodayThroughTheHost() {
        var verified: Pair<Long, String>? = null
        val labels = labels()
        runComposeUiTest {
            setContent {
                AreaAdminPage(
                    areaId = 42,
                    labels = labels,
                    load = { area() },
                    verify = { id, date -> verified = id to date },
                    onBack = {},
                    map = {},
                )
            }
            waitForIdle()

            onNodeWithTag(AREA_ADMIN_VERIFY_BUTTON_TAG).performClick()
            waitUntil(timeoutMillis = 5_000) { verified != null }
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        assertEquals(42L, verified?.first)
        assertEquals(today, verified?.second)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun verifyInlineAction_bumpsThroughTheHost() {
        var verified: Pair<Long, String>? = null
        val labels = labels()
        runComposeUiTest {
            setContent {
                AreaAdminPage(
                    areaId = 42,
                    labels = labels,
                    load = { area() },
                    verify = { id, date -> verified = id to date },
                    onBack = {},
                    map = {},
                )
            }
            waitForIdle()

            onNode(hasScrollAction())
                .performScrollToNode(hasTestTag(areaAdminActionTag("verified_at")))
            onNodeWithTag(areaAdminActionTag("verified_at")).performClick()
            waitUntil(timeoutMillis = 5_000) { verified != null }
        }

        assertEquals(42L, verified?.first)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editName_renamesThroughTheHost() {
        var renamed: String? = null
        val labels = labels()
        runComposeUiTest {
            setContent {
                AreaAdminPage(
                    areaId = 42,
                    labels = labels,
                    load = { area() },
                    verify = { _, _ -> },
                    rename = { renamed = it },
                    onBack = {},
                    map = {},
                )
            }
            waitForIdle()

            onNodeWithTag(areaAdminActionTag("name")).performClick()
            waitForIdle()

            onNodeWithTag(AREA_ADMIN_EDIT_FIELD_TAG).performTextReplacement("New Name")
            onNodeWithText(labels.save).performClick()
            waitUntil(timeoutMillis = 5_000) { renamed != null }
        }

        assertEquals("New Name", renamed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editDescription_updatesThroughTheHost() {
        var updated: String? = null
        var called = false
        val labels = labels()
        runComposeUiTest {
            setContent {
                AreaAdminPage(
                    areaId = 42,
                    labels = labels,
                    load = { area() },
                    verify = { _, _ -> },
                    updateDescription = {
                        updated = it
                        called = true
                    },
                    onBack = {},
                    map = {},
                )
            }
            waitForIdle()

            onNode(hasScrollAction())
                .performScrollToNode(hasTestTag(areaAdminActionTag("description")))
            onNodeWithTag(areaAdminActionTag("description")).performClick()
            waitForIdle()

            onNodeWithTag(AREA_ADMIN_EDIT_FIELD_TAG).performTextReplacement("New description")
            onNodeWithText(labels.save).performClick()
            waitUntil(timeoutMillis = 5_000) { called }
        }

        assertEquals("New description", updated)
    }
}
