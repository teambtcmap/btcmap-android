package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToBoolean
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.neq
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LayerDefaults
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource

const val MERCHANT_CLUSTER_BACKGROUND_LAYER_ID = "merchant_cluster_background"
const val MERCHANT_CLUSTER_COUNT_LAYER_ID = "merchant_cluster_count"
const val MERCHANT_MARKER_LAYER_ID = "merchant_marker"

/**
 * The merchant layer pipeline, ported from the Android SDK builder in
 * `org.btcmap.map.layer.createMerchantLayers` to the MapLibre Compose style DSL.
 *
 * The markers are drawn as circles for now; the Android marker bitmaps
 * (badges, boosted and outdated variants) are ported separately.
 */
@Composable
fun MerchantLayers(
    geoJson: String,
    markerColor: Color,
    markerIconColor: Color,
    boostedMarkerColor: Color,
    usingOpenFreeMap: Boolean,
) {
    val source = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(geoJson),
        options = GeoJsonOptions(cluster = true, clusterMaxZoom = 14, clusterRadius = 50),
    )

    CircleLayer(
        id = MERCHANT_CLUSTER_BACKGROUND_LAYER_ID,
        source = source,
        filter = Feature.has("point_count"),
        color = const(markerColor),
        radius = const(23.dp),
    )

    SymbolLayer(
        id = MERCHANT_CLUSTER_COUNT_LAYER_ID,
        source = source,
        textField = Feature.get("point_count").convertToString().cast(),
        textSize = const(18.sp),
        textColor = const(markerIconColor),
        textFont = if (usingOpenFreeMap) {
            const(listOf("Noto Sans Bold"))
        } else {
            LayerDefaults.FontNames
        },
    )

    CircleLayer(
        id = MERCHANT_MARKER_LAYER_ID,
        source = source,
        filter = Feature.get("cluster").neq(const(true)),
        sortKey = Feature.get("sortKey").convertToNumber().cast(),
        color = switch(
            condition(Feature.get("boosted").convertToBoolean(), const(boostedMarkerColor)),
            fallback = const(markerColor),
        ),
        radius = const(12.dp),
        strokeColor = const(markerIconColor),
        strokeWidth = const(2.dp),
    )
}
