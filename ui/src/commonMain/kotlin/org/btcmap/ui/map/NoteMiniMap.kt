package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A quiet, non-interactive map thumbnail centred on the note, with the stock
 * note pin over it. See [MiniMap] for what it drops and why.
 */
@Composable
fun NoteMiniMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    modifier: Modifier = Modifier,
) {
    MiniMap(
        lat = lat,
        lon = lon,
        styleUrl = styleUrl,
        styleJson = styleJson,
        palette = palette,
        pin = { it.notePin() },
        modifier = modifier,
    )
}
