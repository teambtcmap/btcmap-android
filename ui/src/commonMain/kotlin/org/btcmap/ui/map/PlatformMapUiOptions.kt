package org.btcmap.ui.map

import org.maplibre.compose.map.MapUiOptions

/**
 * The map's platform UI options.
 *
 * On Android the default `Surface` render mode composites the map in a
 * `SurfaceView` behind a hole punched in the Compose view. When the map is
 * resized (for example when the keyboard opens and the activity pads the IME
 * inset) that hole and the surface fall out of sync, leaving the map blank
 * until a redraw is forced (maplibre-compose#1121), so Android uses the
 * `Texture` render mode instead. The other platforms keep the default.
 */
internal expect fun platformMapUiOptions(): MapUiOptions
