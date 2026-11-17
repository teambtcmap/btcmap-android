package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.map.MAX_COMMENT_BADGE
import org.btcmap.map.MAP_OBJECT_KIND_MERCHANT
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToBoolean
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.gt
import org.maplibre.compose.expressions.dsl.gte
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.neq
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.NumberValue
import org.maplibre.compose.expressions.value.StringValue
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LayerDefaults
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.rememberGeoJsonSource

const val MERCHANT_CLUSTER_BACKGROUND_LAYER_ID = "merchant_cluster_background"
const val MERCHANT_CLUSTER_COUNT_LAYER_ID = "merchant_cluster_count"
const val MERCHANT_MARKER_LAYER_ID = "merchant_marker"

/**
 * The merchant layer pipeline, ported from the Android SDK builder in
 * `org.btcmap.map.layer.createMerchantLayers` to the MapLibre Compose style DSL.
 *
 * The marker image names it builds match `org.btcmap.map.merchantMarkerImageName`
 * so [MarkerBitmapFactory] can register the matching bitmap (see `MapScreen`).
 */
@Composable
fun MerchantLayers(
    geoJson: String,
    clusterBackgroundColor: Color,
    clusterTextColor: Color,
    usingOpenFreeMap: Boolean,
    showMarkers: Boolean,
    onMarkerClick: MarkerClickHandler,
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson)) {
        cluster = true
        clusterMaxZoom = 14
        clusterRadius = 50
    }

    CircleLayer(
        id = MERCHANT_CLUSTER_BACKGROUND_LAYER_ID,
        source = source,
        filter = Feature.has("point_count"),
        color = const(clusterBackgroundColor),
        radius = const(23.dp),
    )

    SymbolLayer(
        id = MERCHANT_CLUSTER_COUNT_LAYER_ID,
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
        MerchantMarkerLayers(source = source, onMarkerClick = onMarkerClick)
    }
}

/**
 * The merchant markers alone, drawn from a source that may also carry other map
 * object kinds. The `kind` filter keeps a shared clustered source from drawing
 * another kind's features with the merchant image.
 */
@Composable
internal fun MerchantMarkerLayers(
    source: GeoJsonSource,
    onMarkerClick: MarkerClickHandler,
) {
    SymbolLayer(
        id = MERCHANT_MARKER_LAYER_ID,
        source = source,
        filter = all(
            Feature.get("cluster").neq(const(true)),
            Feature.get("kind").convertToString().eq(const(MAP_OBJECT_KIND_MERCHANT)),
        ),
        sortKey = Feature.get("sortKey").convertToNumber().cast(),
        iconImage = image(markerImageName()),
        iconAnchor = const(SymbolAnchor.Bottom),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconOpacity = switch(
            condition(Feature.get("outdated").convertToBoolean(), const(0.85f)),
            fallback = const(1f),
        ),
        onClick = onMarkerClick,
    )
}

/**
 * The merchant marker image name: `merchant-marker-<icon>[-outdated|-boosted][-bN|-b9p]`,
 * the same shape `org.btcmap.map.merchantMarkerImageName` produces.
 */
private fun markerImageName(): Expression<StringValue> {
    val comments = Feature.get("comments").convertToNumber()

    val variant = switch(
        condition(Feature.get("outdated").convertToBoolean(), const("-outdated")),
        condition(Feature.get("boosted").convertToBoolean(), const("-boosted")),
        fallback = const(""),
    )

    val badge = switch(
        condition(
            comments.gt(const(0).cast<NumberValue<Number>>()),
            const("-b") + switch(
                condition(
                    comments.gte(const(MAX_COMMENT_BADGE.toInt() + 1).cast<NumberValue<Number>>()),
                    const("9p"),
                ),
                fallback = Feature.get("comments").convertToString(),
            ),
        ),
        fallback = const(""),
    )

    return const("merchant-marker-") + Feature.get("iconId").convertToString() + variant + badge
}
