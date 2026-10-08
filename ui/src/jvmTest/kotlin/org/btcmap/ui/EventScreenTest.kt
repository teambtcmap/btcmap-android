package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.util.toUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The event body: its map, date and website render, the zoom controls carry
 * their accessible names, and the website opens through its callback. The map is
 * stubbed out (the test has no GPU).
 */
class EventScreenTest {

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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersMapDateWebsiteAndZoomControls() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventScreen(
                        event = event,
                        geoJson = "",
                        styleUrl = "",
                        styleJson = null,
                        palette = palette,
                        iconFont = null,
                        usingOpenFreeMap = true,
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onOpenWebsite = {},
                        map = { modifier, _ -> Box(modifier) },
                    )
                }
            }
            onNodeWithTag(EVENT_MAP_TAG).assertIsDisplayed()
            onNodeWithTag(EVENT_DATE_TAG).assertIsDisplayed()
            onNodeWithTag(EVENT_WEBSITE_TAG).assertIsDisplayed()
            onNodeWithContentDescription("Zoom in").assertIsDisplayed()
            onNodeWithContentDescription("Zoom out").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun website_opensThroughTheCallback() {
        var opened = 0
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventScreen(
                        event = event,
                        geoJson = "",
                        styleUrl = "",
                        styleJson = null,
                        palette = palette,
                        iconFont = null,
                        usingOpenFreeMap = true,
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onOpenWebsite = { opened++ },
                        map = { modifier, _ -> Box(modifier) },
                    )
                }
            }
            onNodeWithTag(EVENT_WEBSITE_TAG).performClick()
        }
        assertEquals(1, opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withoutMap_thePreviewAndZoomControlsAreHidden() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventScreen(
                        event = event,
                        geoJson = "",
                        styleUrl = "",
                        styleJson = null,
                        palette = palette,
                        iconFont = null,
                        usingOpenFreeMap = true,
                        labels = TEST_EVENT_SCREEN_LABELS,
                        showMap = false,
                        map = { modifier, _ -> Box(modifier) },
                    )
                }
            }
            onNodeWithTag(EVENT_MAP_TAG).assertDoesNotExist()
            onNodeWithContentDescription("Zoom in").assertDoesNotExist()
            onNodeWithTag(EVENT_DATE_TAG).assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withoutCallback_theWebsiteIsStillShownAsText() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventScreen(
                        event = event,
                        geoJson = "",
                        styleUrl = "",
                        styleJson = null,
                        palette = palette,
                        iconFont = null,
                        usingOpenFreeMap = true,
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onOpenWebsite = null,
                        map = { modifier, _ -> Box(modifier) },
                    )
                }
            }
            onNodeWithTag(EVENT_WEBSITE_TAG).assertIsDisplayed()
            onNodeWithText("https://btcmap.org").assertIsDisplayed()
        }
    }
}
