package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource

const val NOTE_MARKER_LAYER_ID = "note_marker"

/**
 * The signed-in user's personal notes, drawn as their own marker layer.
 *
 * The source is a plain GeoJSON source, never clustered, and the layer is
 * declared whatever marker kind the map is showing, so a note is always visible
 * and is never merged into a cluster circle. Each note's pin comes from
 * [MarkerBitmapFactory.notePin] for the icon its feature carries, registered
 * under [noteMarkerImageName]; the map registers one image per icon in use.
 */
@Composable
fun NoteLayers(
    geoJson: String,
    showMarkers: Boolean,
    onClick: MarkerClickHandler,
) {
    val source = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(geoJson),
        options = GeoJsonOptions(cluster = false),
    )

    if (showMarkers) {
        SymbolLayer(
            id = NOTE_MARKER_LAYER_ID,
            source = source,
            iconImage = image(
                const(NOTE_MARKER_IMAGE_PREFIX) + Feature.get("icon").convertToString(),
            ),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
            onClick = onClick,
        )
    }
}
