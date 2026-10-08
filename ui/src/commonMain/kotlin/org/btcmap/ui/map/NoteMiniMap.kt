package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.btcmap.ui.DEFAULT_NOTE_ICON

/**
 * A quiet, non-interactive map thumbnail centred on the note, with the note's
 * pin over it. See [MiniMap] for what it drops and why.
 */
@Composable
fun NoteMiniMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    modifier: Modifier = Modifier,
    icon: String = DEFAULT_NOTE_ICON,
) {
    MiniMap(
        lat = lat,
        lon = lon,
        styleUrl = styleUrl,
        styleJson = styleJson,
        palette = palette,
        pin = { it.notePin(icon) },
        modifier = modifier,
    )
}
