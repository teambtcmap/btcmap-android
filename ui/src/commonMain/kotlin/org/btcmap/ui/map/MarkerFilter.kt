package org.btcmap.ui.map

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.btcmap.ui.MaterialSymbol

/** Which marker kind the map shows; one at a time, as the map's filter did. */
enum class MarkerKind {
    Merchants,
    Events,
    Exchanges,
}

/** Test tags for the filter buttons. */
const val MARKER_FILTER_MERCHANTS_TAG = "marker-filter-merchants"
const val MARKER_FILTER_EVENTS_TAG = "marker-filter-events"
const val MARKER_FILTER_EXCHANGES_TAG = "marker-filter-exchanges"

/**
 * The map's marker-kind filter: one button per kind, the active one ringed.
 * Ported from the Views button group the map swap dropped, whose buttons shared
 * the app's button background and marked the selected one with a border.
 */
@Composable
fun MarkerFilterButtons(
    selected: MarkerKind,
    onSelect: (MarkerKind) -> Unit,
    palette: AreaChipPalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MarkerFilterButton(
            kind = MarkerKind.Merchants,
            glyph = "storefront",
            tag = MARKER_FILTER_MERCHANTS_TAG,
            selected = selected,
            onSelect = onSelect,
            palette = palette,
        )
        MarkerFilterButton(
            kind = MarkerKind.Events,
            glyph = "event",
            tag = MARKER_FILTER_EVENTS_TAG,
            selected = selected,
            onSelect = onSelect,
            palette = palette,
        )
        MarkerFilterButton(
            kind = MarkerKind.Exchanges,
            glyph = "currency_exchange",
            tag = MARKER_FILTER_EXCHANGES_TAG,
            selected = selected,
            onSelect = onSelect,
            palette = palette,
        )
    }
}

@Composable
private fun MarkerFilterButton(
    kind: MarkerKind,
    glyph: String,
    tag: String,
    selected: MarkerKind,
    onSelect: (MarkerKind) -> Unit,
    palette: AreaChipPalette,
) {
    FilledTonalIconButton(
        onClick = { onSelect(kind) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = palette.buttonBackground,
            contentColor = palette.buttonIcon,
        ),
        modifier = Modifier.testTag(tag),
    ) {
        // The ring is drawn on the button's visual circle, not its larger touch
        // target, so it traces the background the way the Views border did.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(FILTER_BUTTON_SIZE)
                .then(
                    if (kind == selected) {
                        Modifier.border(FILTER_BORDER_WIDTH, palette.buttonBorder, CircleShape)
                    } else {
                        Modifier
                    },
                ),
        ) {
            MaterialSymbol(glyph = glyph, contentDescription = null)
        }
    }
}

/** Material's icon button visual container, which its touch target pads to 48. */
private val FILTER_BUTTON_SIZE = 40.dp

private val FILTER_BORDER_WIDTH = 2.dp
