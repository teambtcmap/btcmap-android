package org.btcmap.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.db.table.area.Area
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The area admin screen: every cached field of one area, read-only. */
class AreaAdminScreenTest {

    private fun area() = Area(
        id = 42,
        name = "Grand Paris",
        type = "community",
        urlAlias = "grand-paris",
        icon = "https://static.example/icon.png",
        iconWide = null,
        websiteUrl = "https://btcmap.org/community/grand-paris",
        description = "A community",
        bboxWest = 2.22,
        bboxSouth = 48.81,
        bboxEast = 2.47,
        bboxNorth = 48.91,
        geoJson = """{"type":"Polygon"}""",
        updatedAt = Instant.parse("2026-04-21T08:46:03Z"),
        verifiedAt = "2026-01-06",
    )

    private fun fields(area: Area): Map<String, String> =
        areaAdminFields(area).associate { it.label to it.value }

    @Test
    fun areaAdminFields_listEveryCachedField() {
        val fields = fields(area())

        assertEquals("42", fields["id"])
        assertEquals("Grand Paris", fields["name"])
        assertEquals("community", fields["type"])
        assertEquals("grand-paris", fields["url_alias"])
        assertEquals("https://btcmap.org/community/grand-paris", fields["website_url"])
        assertEquals("A community", fields["description"])
        assertEquals("https://static.example/icon.png", fields["icon"])
        assertEquals("2.22, 48.81, 2.47, 48.91", fields["bbox"])
        assertEquals("""{"type":"Polygon"}""", fields["geo_json"])
        assertEquals("2026-01-06", fields["verified_at"])
        assertTrue(fields["updated_at"]!!.startsWith("2026-04-21T08:46:03"))

        // The full field set is present, keyed by the schema names.
        assertEquals(
            listOf(
                "id", "name", "type", "url_alias", "website_url", "description", "icon",
                "icon_wide", "bbox", "geo_json", "verified_at", "updated_at", "deleted_at",
                "localized_name", "localized_description",
            ),
            areaAdminFields(area()).map { it.label },
        )
    }

    @Test
    fun areaAdminFields_placeholdersForAbsentValues() {
        val fields = fields(
            area().copy(
                iconWide = null,
                deletedAt = null,
                localizedName = null,
                localizedDescription = null,
            ),
        )

        assertEquals("—", fields["icon_wide"])
        assertEquals("—", fields["deleted_at"])
        assertEquals("—", fields["localized_name"])
        assertEquals("—", fields["localized_description"])
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun screen_listsTheArea() {
        runComposeUiTest {
            setContent { AreaAdminScreen(area()) }
            waitForIdle()

            assertTrue(onAllNodesWithText("Grand Paris").fetchSemanticsNodes().isNotEmpty())
            assertTrue(onAllNodesWithText("id").fetchSemanticsNodes().isNotEmpty())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun map_scrollsAwayWithTheFields() {
        runComposeUiTest {
            setContent { AreaAdminScreen(area(), map = { Text("MAP") }) }
            waitForIdle()

            val before = onNodeWithText("MAP").fetchSemanticsNode().boundsInRoot.top

            onNode(hasScrollAction()).performScrollToNode(hasText("localized_description"))

            // Scrolled off (or disposed): it must no longer sit where it did.
            val after = onAllNodesWithText("MAP").fetchSemanticsNodes()
            assertTrue(after.isEmpty() || after.first().boundsInRoot.top < before)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun urlValue_opensTheLinkOnTap() {
        var opened: String? = null
        runComposeUiTest {
            setContent { AreaAdminScreen(area(), onOpenUrl = { opened = it }) }
            waitForIdle()

            onNodeWithText("https://btcmap.org/community/grand-paris").performClick()
        }
        assertEquals("https://btcmap.org/community/grand-paris", opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nonUrlValue_isNotALink() {
        var opened: String? = null
        runComposeUiTest {
            setContent { AreaAdminScreen(area(), onOpenUrl = { opened = it }) }
            waitForIdle()

            // The url_alias is not a URL, so tapping it must not open anything.
            onNodeWithText("grand-paris").performClick()
        }
        assertEquals(null, opened)
    }
}
