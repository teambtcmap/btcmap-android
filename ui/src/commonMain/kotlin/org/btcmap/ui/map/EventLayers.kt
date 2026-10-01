package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.ICON_OFFSET_Y
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.neq
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LayerDefaults
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource

const val EVENT_CLUSTER_BACKGROUND_LAYER_ID = "event_cluster_background"
const val EVENT_CLUSTER_COUNT_LAYER_ID = "event_cluster_count"
const val EVENT_MARKER_LAYER_ID = "event_marker"
const val EVENT_ICON_LAYER_ID = "event_icon"

/**
 * The event layer pipeline, ported from `org.btcmap.map.layer.createEventLayers`.
 * Its markers share the plain pin and draw the event glyph from
 * [EVENT_MARKER_ICON_NAME] on a second layer.
 */
@Composable
fun EventLayers(
    geoJson: String,
    clusterBackgroundColor: Color,
    clusterTextColor: Color,
    usingOpenFreeMap: Boolean,
    showMarkers: Boolean,
    onMarkerClick: MarkerClickHandler,
) {
    val source = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(geoJson),
        options = GeoJsonOptions(cluster = true, clusterMaxZoom = 14, clusterRadius = 30),
    )

    CircleLayer(
        id = EVENT_CLUSTER_BACKGROUND_LAYER_ID,
        source = source,
        filter = Feature.has("point_count"),
        color = const(clusterBackgroundColor),
        radius = const(23.dp),
    )

    SymbolLayer(
        id = EVENT_CLUSTER_COUNT_LAYER_ID,
        source = source,
        textField = Feature.get("point_count").convertToString().cast(),
        textSize = const(18.sp),
        textColor = const(clusterTextColor),
        textFont = if (usingOpenFreeMap) {
            const(listOf("Noto Sans Bold"))
        } else {
            LayerDefaults.FontNames
        },
    )

    if (showMarkers) {
        SymbolLayer(
            id = EVENT_MARKER_LAYER_ID,
            source = source,
            filter = Feature.get("cluster").neq(const(true)),
            iconImage = image(MARKER_PIN_IMAGE_ID),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
            onClick = onMarkerClick,
        )

        SymbolLayer(
            id = EVENT_ICON_LAYER_ID,
            source = source,
            filter = Feature.get("cluster").neq(const(true)),
            iconImage = image(EVENT_MARKER_ICON_NAME),
            iconAnchor = const(SymbolAnchor.Center),
            iconOffset = const(DpOffset(0.dp, ICON_OFFSET_Y.dp)),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
        )
    }
}
