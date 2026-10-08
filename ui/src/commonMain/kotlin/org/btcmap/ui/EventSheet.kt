package org.btcmap.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.MarkerPalette

/**
 * The event details shown when an event marker is selected, the map's event
 * sheet. It reuses the standalone [EventScreen] body and adds the event's name
 * and actions as a header, so opening an event from the map keeps the user on
 * the map rather than pushing a screen (as the map's place sheet does).
 *
 * [onDirections] and [onDelete] are null when the host does not offer them; a
 * null delete (the signed-in user may not remove the event) draws no action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSheet(
    event: Event,
    geoJson: String,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    iconFont: FontFamily?,
    usingOpenFreeMap: Boolean,
    labels: EventScreenLabels,
    onDismiss: () -> Unit,
    onOpenWebsite: (() -> Unit)? = null,
    onDirections: (() -> Unit)? = null,
    onDelete: (suspend () -> Unit)? = null,
    onDeleted: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
) {
    // Open at the half-expanded height the place sheet uses, so the map stays
    // visible behind it; the user can drag it up to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The place sheet keeps the map fully visible; match it.
        scrimColor = Color.Transparent,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
        ) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (onDirections != null) {
                IconButton(onClick = onDirections) {
                    MaterialSymbol(glyph = "directions", contentDescription = labels.directions)
                }
            }
            EventDeleteAction(
                labels = labels,
                onDelete = onDelete,
                onDeleted = onDeleted,
            )
        }

        EventScreen(
            event = event,
            geoJson = geoJson,
            styleUrl = styleUrl,
            styleJson = styleJson,
            palette = palette,
            iconFont = iconFont,
            usingOpenFreeMap = usingOpenFreeMap,
            labels = labels,
            onOpenWebsite = onOpenWebsite,
            // A flat 16dp inset lines the body up with the header above it,
            // instead of the standalone screen's wider capped/centred margin.
            contentPadding = 16.dp,
            // The map is already behind the sheet, so the body does not draw
            // its own preview.
            showMap = false,
        )
    }
}
