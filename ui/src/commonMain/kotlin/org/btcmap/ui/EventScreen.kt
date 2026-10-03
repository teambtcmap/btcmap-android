package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.EventPreviewMap
import org.btcmap.ui.map.MarkerPalette
import org.maplibre.compose.map.MapState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The event screen's strings, so the screen stays resource-free. */
data class EventScreenLabels(
    /** Formats a same-day event's date and its start and end times. */
    val dateRange: (date: String, start: String, end: String) -> String,
)

/**
 * An event's details: its own map, its name, its dates and its website. The host
 * supplies its own top bar (with the directions action) and back affordance.
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
    modifier: Modifier = Modifier,
) {
    var mapState by remember { mutableStateOf<MapState?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            EventPreviewMap(
                lat = event.lat,
                lon = event.lon,
                geoJson = geoJson,
                styleUrl = styleUrl,
                styleJson = styleJson,
                palette = palette,
                usingOpenFreeMap = usingOpenFreeMap,
                iconFont = iconFont,
                onState = { mapState = it },
                modifier = Modifier.fillMaxSize(),
            )
            Column(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = { mapState?.zoomBy(1.0) }) {
                    MaterialSymbol(glyph = "add", contentDescription = null)
                }
                IconButton(onClick = { mapState?.zoomBy(-1.0) }) {
                    MaterialSymbol(glyph = "remove", contentDescription = null)
                }
            }
        }

        Text(
            text = event.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        val start = event.startsAt
        val end = event.endsAt
        when {
            end == null -> Text(
                text = start.format(dateTimeFormatter),
                modifier = Modifier.padding(top = 8.dp),
            )

            start.toLocalDate() == end.toLocalDate() -> Text(
                text = labels.dateRange(
                    start.format(dateFormatter),
                    start.format(timeFormatter),
                    end.format(timeFormatter),
                ),
                modifier = Modifier.padding(top = 8.dp),
            )

            else -> {
                Text(
                    text = start.format(dateTimeFormatter),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(text = end.format(dateTimeFormatter))
            }
        }

        event.website?.let {
            Text(
                text = it.toString(),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun MapState.zoomBy(delta: Double) {
    val camera = cameraPosition
    setCameraPosition(camera.copy(zoom = camera.zoom + delta))
}
