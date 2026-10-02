package org.btcmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
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
 * The map's marker-kind filter: one button per kind, the active one filled. Ported
 * from the Views button group the map swap dropped.
 */
@Composable
fun MarkerFilterButtons(
    selected: MarkerKind,
    onSelect: (MarkerKind) -> Unit,
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
        )
        MarkerFilterButton(
            kind = MarkerKind.Events,
            glyph = "event",
            tag = MARKER_FILTER_EVENTS_TAG,
            selected = selected,
            onSelect = onSelect,
        )
        MarkerFilterButton(
            kind = MarkerKind.Exchanges,
            glyph = "currency_exchange",
            tag = MARKER_FILTER_EXCHANGES_TAG,
            selected = selected,
            onSelect = onSelect,
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
) {
    val onClick = { onSelect(kind) }
    if (kind == selected) {
        FilledIconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
            MaterialSymbol(glyph = glyph, contentDescription = null)
        }
    } else {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
            MaterialSymbol(glyph = glyph, contentDescription = null)
        }
    }
}
