package org.btcmap.ui.map

import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapUiOptions

internal actual fun platformMapUiOptions(): MapUiOptions =
    MapUiOptions { renderMode = AndroidRenderMode.Texture }
