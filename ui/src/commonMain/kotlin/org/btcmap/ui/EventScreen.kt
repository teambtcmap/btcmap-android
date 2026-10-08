package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.EventPreviewMap
import org.btcmap.ui.map.MarkerPalette
import org.maplibre.compose.map.MapState

/** Test tags for the event body's map, rows and controls. */
const val EVENT_MAP_TAG = "event-map"
const val EVENT_ZOOM_IN_TAG = "event-zoom-in"
const val EVENT_ZOOM_OUT_TAG = "event-zoom-out"
const val EVENT_DATE_TAG = "event-date"
const val EVENT_WEBSITE_TAG = "event-website"

/** The event screen's strings, so the screen stays resource-free. */
data class EventScreenLabels(
    /** Formats a same-day event's date and its start and end times. */
    val dateRange: (date: String, start: String, end: String) -> String,
    /** The content description of the map's zoom-in control. */
    val zoomIn: String,
    /** The content description of the map's zoom-out control. */
    val zoomOut: String,
    /** The content description of the event sheet's directions action. */
    val directions: String,
    /** The delete action and the confirm button's label. */
    val delete: String,
    /** The delete confirmation's title and message. */
    val deleteConfirmTitle: String,
    val deleteConfirmMessage: String,
    /** Shown in the body when the delete call fails. */
    val deleteFailed: String,
    /** The confirmation's dismiss button. */
    val cancel: String,
)

/**
 * An event's details: its own map, its dates and its website. The host supplies
 * its own top bar (with the event's name, the directions action and a back
 * affordance), so the body carries no title of its own.
 *
 * [map] renders the event's map; the host supplies it so a test can render the
 * screen without a GPU. [onOpenWebsite] opens the event's website; null leaves
 * it as plain text.
 *
 * The body is a [ContentColumn], so a wide desktop or tablet window caps and
 * centres it rather than stretching the map and rows edge to edge.
 */
@Composable
fun EventScreen(
    event: Event,
    geoJson: String,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    iconFont: FontFamily?,
    usingOpenFreeMap: Boolean,
    labels: EventScreenLabels,
    onOpenWebsite: (() -> Unit)? = null,
    /**
     * A flat horizontal inset for the body, or null for the standalone screen's
     * capped and centred margin. The map's event sheet sets it so the body lines
     * up with its header rather than being inset further on a wide window.
     */
    contentPadding: Dp? = null,
    /**
     * Whether the body draws its own preview map. The map's event sheet says no
     * because the map is already behind it; the standalone screen says yes.
     */
    showMap: Boolean = true,
    modifier: Modifier = Modifier,
    map: @Composable (modifier: Modifier, onState: (MapState) -> Unit) -> Unit =
        { mapModifier, onState ->
            EventPreviewMap(
                lat = event.lat,
                lon = event.lon,
                geoJson = geoJson,
                styleUrl = styleUrl,
                styleJson = styleJson,
                palette = palette,
                usingOpenFreeMap = usingOpenFreeMap,
                iconFont = iconFont,
                onState = onState,
                modifier = mapModifier,
            )
        },
) {
    var mapState by remember { mutableStateOf<MapState?>(null) }

    // The body is shared by the standalone screen (capped and centred with the
    // responsive margin) and the map's event sheet (a flat inset, so it lines up
    // with its header).
    val body: @Composable ColumnScope.() -> Unit = {
        if (showMap) {
            // ContentColumn caps and centres the body but leaves the screen's
            // vertical 16dp to it.
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                // Only the map is clipped; the zoom controls float above it so
                // their own corners are not cut by the map's rounded shape.
                map(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag(EVENT_MAP_TAG),
                    { mapState = it },
                )
                // The zoom controls sit on a tonal container so they stay
                // legible over the map imagery, the way the main map's controls
                // do.
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = { mapState?.zoomBy(1.0) },
                        modifier = Modifier.testTag(EVENT_ZOOM_IN_TAG),
                    ) {
                        MaterialSymbol(glyph = "add", contentDescription = labels.zoomIn)
                    }
                    FilledTonalIconButton(
                        onClick = { mapState?.zoomBy(-1.0) },
                        modifier = Modifier.testTag(EVENT_ZOOM_OUT_TAG),
                    ) {
                        MaterialSymbol(glyph = "remove", contentDescription = labels.zoomOut)
                    }
                }
            }
        }

        // A multi-day event spans two lines; everything else fits on one.
        val time = eventTimeText(event.startsAt, event.endsAt, labels.dateRange)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag(EVENT_DATE_TAG),
        ) {
            MaterialSymbol(
                glyph = "schedule",
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = time.start, style = MaterialTheme.typography.bodyLarge)
                time.end?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        event.website?.let { url ->
            WebsiteRow(
                text = url.toString(),
                onClick = onOpenWebsite,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag(EVENT_WEBSITE_TAG),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (contentPadding == null) {
        ContentColumn(
            modifier = modifier,
            scroll = true,
            margin = true,
            content = body,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPadding),
            content = body,
        )
    }
}

/**
 * The event's website: an earth glyph and, when [onClick] is set, a tappable
 * link in the secondary colour, matching the area screen's website row.
 */
@Composable
private fun WebsiteRow(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
    ) {
        MaterialSymbol(
            glyph = "public",
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.secondary
            } else {
                LocalContentColor.current
            },
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun MapState.zoomBy(delta: Double) {
    val camera = cameraPosition
    setCameraPosition(camera.copy(zoom = camera.zoom + delta))
}
