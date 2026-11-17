package org.btcmap.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.ui.AppTheme

/** The map's marker-kind buttons toggle each kind independently. */
@OptIn(ExperimentalTestApi::class)
class MarkerFilterButtonsTest {

    @Test
    fun togglesEachKindIndependently() {
        var visible by mutableStateOf(setOf(MarkerKind.Merchants))
        runComposeUiTest {
            setContent {
                AppTheme {
                    MarkerFilterButtons(
                        visible = visible,
                        onToggle = { visible = visible.toggled(it) },
                        palette = AreaChipPalette(
                            buttonBackground = Color(0xFF1F2937),
                            buttonIcon = Color.White,
                            buttonAccent = Color(0xFFF7931A),
                            badgeBackground = Color(0xFFE53935),
                            badgeText = Color.White,
                        ),
                    )
                }
            }
            onNodeWithTag(MARKER_FILTER_MERCHANTS_TAG).assertIsOn()
            onNodeWithTag(MARKER_FILTER_EVENTS_TAG).assertIsOff()
            onNodeWithTag(MARKER_FILTER_EXCHANGES_TAG).assertIsOff()
            onNodeWithTag(MARKER_FILTER_NOTES_TAG).assertIsOff()

            onNodeWithTag(MARKER_FILTER_EVENTS_TAG).performClick()
            assertEquals(setOf(MarkerKind.Merchants, MarkerKind.Events), visible)
            onNodeWithTag(MARKER_FILTER_MERCHANTS_TAG).assertIsOn()
            onNodeWithTag(MARKER_FILTER_EVENTS_TAG).assertIsOn()

            onNodeWithTag(MARKER_FILTER_MERCHANTS_TAG).performClick()
            assertEquals(setOf(MarkerKind.Events), visible)
            onNodeWithTag(MARKER_FILTER_MERCHANTS_TAG).assertIsOff()
            onNodeWithTag(MARKER_FILTER_EVENTS_TAG).assertIsOn()
        }
    }
}
