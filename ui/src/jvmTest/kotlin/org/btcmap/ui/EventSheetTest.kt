package org.btcmap.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.util.toUrl
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The map's event sheet: its header carries the event's name and actions, and
 * the event body renders beneath it without its own map.
 */
@OptIn(ExperimentalTestApi::class)
class EventSheetTest {

    private val event = Event(
        id = 1L,
        lat = 7.8,
        lon = 98.3,
        name = "Phuket Bitcoin Meetup",
        website = "https://btcmap.org".toUrl(),
        startsAt = Instant.parse("2026-11-01T18:00:00Z"),
        endsAt = Instant.parse("2026-11-01T20:00:00Z"),
    )

    private val palette = MarkerPalette(
        markerBackground = Color(0xFF00BCD4),
        markerIcon = Color.White,
        boostedMarkerBackground = Color(0xFF00BCD4),
        boostedMarkerIcon = Color.White,
        badgeBackground = Color(0xFFE53935),
        badgeText = Color.White,
    )

    @Test
    fun showsTheNameActionsAndBody() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventSheet(
                        event = event,
                        geoJson = "",
                        styleUrl = "",
                        styleJson = null,
                        palette = palette,
                        iconFont = null,
                        usingOpenFreeMap = true,
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onDismiss = {},
                        onOpenWebsite = {},
                        onDirections = {},
                        onDelete = {},
                    )
                }
            }
            onNodeWithText("Phuket Bitcoin Meetup").assertIsDisplayed()
            onNodeWithContentDescription("Directions").assertIsDisplayed()
            onNodeWithTag(EVENT_DELETE_TAG).assertIsDisplayed()
            onNodeWithTag(EVENT_WEBSITE_TAG).assertExists()
        }
    }
}
