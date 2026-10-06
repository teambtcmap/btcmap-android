package org.btcmap.ui.map

import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.renderMode

internal actual fun platformMapUiOptions(): MapUiOptions =
    MapUiOptions { renderMode = AndroidRenderMode.Texture }
