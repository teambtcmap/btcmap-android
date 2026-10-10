package org.btcmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    Notes,
}

/** Test tags for the filter buttons. */
const val MARKER_FILTER_MERCHANTS_TAG = "marker-filter-merchants"
const val MARKER_FILTER_EVENTS_TAG = "marker-filter-events"
const val MARKER_FILTER_EXCHANGES_TAG = "marker-filter-exchanges"
const val MARKER_FILTER_NOTES_TAG = "marker-filter-notes"

/**
 * The map's marker-kind filter: one button per kind, the active one's icon
 * tinted with the button accent. Ported from the Views button group the map swap
 * dropped, whose buttons shared the app's button background and marked the
 * selected one with a border; that ring is gone, so the tinted icon now marks
 * the selection.
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
        // The icon button's 48dp touch target pads its 40dp circle by 4dp on
        // each side, so 8dp here reads as MAP_CONTROLS_GAP on screen.
        verticalArrangement = Arrangement.spacedBy(MAP_CONTROLS_GAP - 8.dp),
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
        MarkerFilterButton(
            kind = MarkerKind.Notes,
            glyph = "notes",
            tag = MARKER_FILTER_NOTES_TAG,
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
    val isSelected = kind == selected
    FilledTonalIconButton(
        onClick = { onSelect(kind) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            // The active kind is only tinted, so all four keep the same button
            // background and the accent colour alone marks the selection.
            containerColor = palette.buttonBackground,
            contentColor = if (isSelected) palette.buttonAccent else palette.buttonIcon,
        ),
        modifier = Modifier.testTag(tag),
    ) {
        MaterialSymbol(glyph = glyph, contentDescription = null)
    }
}

/** Half the padding the 48dp touch target adds around the button's 40dp circle. */
internal val FILTER_BUTTON_INSET = 4.dp
