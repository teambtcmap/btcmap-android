package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import org.btcmap.map.MAP_OBJECT_KIND_NOTE
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.neq
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.rememberGeoJsonSource

const val NOTE_MARKER_LAYER_ID = "note_marker"

/**
 * The signed-in user's personal notes, drawn as their own marker layer.
 *
 * The source is a plain GeoJSON source when used alone. The main map draws them
 * from a shared clustered source instead, so a note can join a cluster with
 * other map object kinds.
 */
@Composable
fun NoteLayers(
    geoJson: String,
    showMarkers: Boolean,
    onClick: MarkerClickHandler,
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson)) {
        cluster = false
    }

    if (showMarkers) {
        NoteMarkerLayers(source = source, onClick = onClick)
    }
}

/** The note markers alone, from a source that may carry other map object kinds. */
@Composable
internal fun NoteMarkerLayers(
    source: GeoJsonSource,
    onClick: MarkerClickHandler,
) {
    SymbolLayer(
        id = NOTE_MARKER_LAYER_ID,
        source = source,
        filter = all(
            Feature.get("cluster").neq(const(true)),
            Feature.get("kind").convertToString().eq(const(MAP_OBJECT_KIND_NOTE)),
        ),
        iconImage = image(
            const(NOTE_MARKER_IMAGE_PREFIX) + Feature.get("icon").convertToString(),
        ),
        iconAnchor = const(SymbolAnchor.Bottom),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        onClick = onClick,
    )
}
