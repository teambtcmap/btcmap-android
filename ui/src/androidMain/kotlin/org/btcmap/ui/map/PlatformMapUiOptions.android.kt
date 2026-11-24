package org.btcmap.ui.map

import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapUiOptions
import androidx.compose.ui.unit.dp

internal actual fun platformMapUiOptions(): MapUiOptions =
    MapUiOptions { renderMode = AndroidRenderMode.Texture }

internal actual val mapSearchBarMargin = 16.dp
