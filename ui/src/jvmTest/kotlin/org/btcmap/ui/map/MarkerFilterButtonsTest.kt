package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.ui.AppTheme

/** The map's marker-kind filter buttons report the kind they select. */
@OptIn(ExperimentalTestApi::class)
class MarkerFilterButtonsTest {

    @Test
    fun rendersEachKindAndReportsTheSelectedOne() {
        var selected: MarkerKind? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    MarkerFilterButtons(
                        selected = MarkerKind.Merchants,
                        onSelect = { selected = it },
                        palette = AreaChipPalette(
                            buttonBackground = Color(0xFF1F2937),
                            buttonIcon = Color.White,
                            buttonBorder = Color.White,
                            badgeBackground = Color(0xFFE53935),
                            badgeText = Color.White,
                        ),
                    )
                }
            }
            onNodeWithTag(MARKER_FILTER_MERCHANTS_TAG).assertExists()
            onNodeWithTag(MARKER_FILTER_EXCHANGES_TAG).assertExists()

            onNodeWithTag(MARKER_FILTER_EVENTS_TAG).performClick()
        }
        assertEquals(MarkerKind.Events, selected)
    }
}
