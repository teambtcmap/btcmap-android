package org.btcmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.btcmap.ui.MaterialSymbol

/** Which marker kinds the map can draw; any combination may be shown at once. */
enum class MarkerKind {
    Merchants,
    Events,
    Exchanges,
    Notes,
}

/**
 * The kinds drawn before the user has chosen otherwise. Exchanges (ATMs) are
 * numerous enough to clutter the map, so they start hidden; a saved choice
 * replaces this as soon as the user toggles any kind.
 */
val DEFAULT_VISIBLE_MARKER_KINDS: Set<MarkerKind> = MarkerKind.entries.toSet() - MarkerKind.Exchanges

/** The visible kinds with [kind] added when absent and removed when present. */
internal fun Set<MarkerKind>.toggled(kind: MarkerKind): Set<MarkerKind> =
    if (kind in this) this - kind else this + kind

/** Saves the visible kinds across a configuration change as their names. */
internal val VISIBLE_MARKER_KINDS_SAVER: Saver<Set<MarkerKind>, List<String>> = Saver(
    save = { kinds -> MarkerKind.entries.filter { it in kinds }.map { it.name } },
    restore = { names ->
        val byName = MarkerKind.entries.associateBy { it.name }
        names.mapNotNull { byName[it] }.toSet()
    },
)

/** Test tags for the filter buttons. */
const val MARKER_FILTER_MERCHANTS_TAG = "marker-filter-merchants"
const val MARKER_FILTER_EVENTS_TAG = "marker-filter-events"
const val MARKER_FILTER_EXCHANGES_TAG = "marker-filter-exchanges"
const val MARKER_FILTER_NOTES_TAG = "marker-filter-notes"

/**
 * The map's marker-kind toggles: one button per kind, each showing whether that
 * kind is drawn and flipping it on tap. Any combination can be visible; every
 * kind starts on except exchanges (ATMs), which start hidden. A visible kind
 * carries a ring in the same colour as its icon; a hidden one fades to 60% and
 * has no ring.
 *
 * Ported from the Views button group the map swap dropped, whose buttons shared
 * the app's button background and marked the selected one with a border.
 */
@Composable
fun MarkerFilterButtons(
    visible: Set<MarkerKind>,
    onToggle: (MarkerKind) -> Unit,
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
            visible = MarkerKind.Merchants in visible,
            onToggle = onToggle,
            palette = palette,
        )
        MarkerFilterButton(
            kind = MarkerKind.Events,
            glyph = "event",
            tag = MARKER_FILTER_EVENTS_TAG,
            visible = MarkerKind.Events in visible,
            onToggle = onToggle,
            palette = palette,
        )
        MarkerFilterButton(
            kind = MarkerKind.Exchanges,
            glyph = "currency_exchange",
            tag = MARKER_FILTER_EXCHANGES_TAG,
            visible = MarkerKind.Exchanges in visible,
            onToggle = onToggle,
            palette = palette,
        )
        MarkerFilterButton(
            kind = MarkerKind.Notes,
            glyph = "notes",
            tag = MARKER_FILTER_NOTES_TAG,
            visible = MarkerKind.Notes in visible,
            onToggle = onToggle,
            palette = palette,
        )
    }
}

@Composable
private fun MarkerFilterButton(
    kind: MarkerKind,
    glyph: String,
    tag: String,
    visible: Boolean,
    onToggle: (MarkerKind) -> Unit,
    palette: AreaChipPalette,
) {
    FilledTonalIconToggleButton(
        checked = visible,
        onCheckedChange = { onToggle(kind) },
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            // The active kind keeps the full button colour; an inactive one
            // fades to 60% and is not ringed.
            containerColor = palette.buttonBackground,
            contentColor = palette.buttonIcon.copy(alpha = 0.6f),
            checkedContainerColor = palette.buttonBackground,
            checkedContentColor = palette.buttonIcon,
        ),
        shape = CircleShape,
        modifier = Modifier
            .testTag(tag)
            .then(
                if (visible) {
                    // The button's 48dp touch target is larger than its 40dp
                    // circle, so draw the ring on the circle rather than around
                    // the touch target.
                    Modifier.drawWithContent {
                        drawContent()
                        drawCircle(
                            color = palette.buttonIcon,
                            radius = size.minDimension / 2f - FILTER_BUTTON_INSET.toPx(),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        MaterialSymbol(glyph = glyph, contentDescription = null)
    }
}

/** Half the padding the 48dp touch target adds around the button's 40dp circle. */
internal val FILTER_BUTTON_INSET = 4.dp
