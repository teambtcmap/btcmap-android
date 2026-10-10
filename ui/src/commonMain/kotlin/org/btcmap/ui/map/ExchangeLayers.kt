package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.map.EXCHANGE_MARKER_ICON_PREFIX
import org.btcmap.map.ICON_OFFSET_Y
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToBoolean
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.gt
import org.maplibre.compose.expressions.dsl.gte
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.neq
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.NumberValue
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.TranslateAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LayerDefaults
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource

const val EXCHANGE_CLUSTER_BACKGROUND_LAYER_ID = "exchange_cluster_background"
const val EXCHANGE_CLUSTER_TEXT_LAYER_ID = "exchange_cluster_text"
const val EXCHANGE_MARKER_LAYER_ID = "exchange_marker"
const val EXCHANGE_MARKER_ICON_LAYER_ID = "exchange_marker_icon"
const val EXCHANGE_COMMENT_COUNT_BACKGROUND_LAYER_ID = "exchange_comment_count_background"
const val EXCHANGE_COMMENT_COUNT_TEXT_LAYER_ID = "exchange_comment_count_text"

/**
 * The exchange layer pipeline, ported from
 * `org.btcmap.map.layer.createExchangeLayers`. Its markers share the plain pin
 * and draw the service glyph from the name
 * `org.btcmap.map.exchangeMarkerIconImageName` builds.
 */
@Composable
fun ExchangeLayers(
    geoJson: String,
    clusterBackgroundColor: Color,
    clusterTextColor: Color,
    badgeBackgroundColor: Color,
    badgeTextColor: Color,
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
        id = EXCHANGE_CLUSTER_BACKGROUND_LAYER_ID,
        source = source,
        filter = Feature.has("point_count"),
        color = const(clusterBackgroundColor),
        radius = const(23.dp),
    )

    SymbolLayer(
        id = EXCHANGE_CLUSTER_TEXT_LAYER_ID,
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
            id = EXCHANGE_MARKER_LAYER_ID,
            source = source,
            filter = Feature.get("cluster").neq(const(true)),
            iconImage = image(MARKER_PIN_IMAGE_ID),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
            onClick = onMarkerClick,
        )

        SymbolLayer(
            id = EXCHANGE_MARKER_ICON_LAYER_ID,
            source = source,
            filter = Feature.get("cluster").neq(const(true)),
            iconImage = image(const(EXCHANGE_MARKER_ICON_PREFIX) + Feature.get("iconId").convertToString()),
            iconAnchor = const(SymbolAnchor.Center),
            iconOffset = const(DpOffset(0.dp, ICON_OFFSET_Y.dp)),
            iconOpacity = switch(
                condition(Feature.get("outdated").convertToBoolean(), const(OUTDATED_ICON_ALPHA)),
                fallback = const(1f),
            ),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
        )

        val hasComments = Feature.get("comments").convertToNumber()
            .gt(const(0).cast<NumberValue<Number>>())

        CircleLayer(
            id = EXCHANGE_COMMENT_COUNT_BACKGROUND_LAYER_ID,
            source = source,
            filter = all(
                Feature.get("cluster").neq(const(true)),
                hasComments,
            ),
            color = const(badgeBackgroundColor),
            radius = const(9.dp),
            opacity = const(1f),
            translate = const(DpOffset(13.dp, -43.dp)),
            translateAnchor = const(TranslateAnchor.Viewport),
        )

        SymbolLayer(
            id = EXCHANGE_COMMENT_COUNT_TEXT_LAYER_ID,
            source = source,
            filter = all(
                Feature.get("cluster").neq(const(true)),
                hasComments,
            ),
            textField = switch(
                condition(
                    Feature.get("comments").convertToNumber()
                        .gte(const(10).cast<NumberValue<Number>>()),
                    const("9+"),
                ),
                fallback = Feature.get("comments").convertToString(),
            ).cast(),
            textSize = const(11.sp),
            textColor = const(badgeTextColor),
            textTranslate = const(DpOffset(13.dp, -43.dp)),
            textTranslateAnchor = const(TranslateAnchor.Viewport),
            textAllowOverlap = const(true),
        )
    }
}
