package org.btcmap.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.map.MaplibreMap

/**
 * Phase 3 spike: a shared MapLibre Compose map. Renders the default demo style
 * until the app's own style and layers are ported.
 */
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    AppTheme {
        MaplibreMap(modifier = modifier.fillMaxSize())
    }
}
