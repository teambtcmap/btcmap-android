package org.btcmap.map.layer

import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.EXCHANGE_MARKER_ICON_PREFIX
import org.btcmap.map.ICON_OFFSET_Y
import org.btcmap.map.OUTDATED_ICON_ALPHA
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

const val EXCHANGE_MARKER_LAYER_ID = "exchange_marker"
const val EXCHANGE_CLUSTER_TEXT_LAYER_ID = "exchange_cluster_text"
const val EXCHANGE_COMMENT_COUNT_TEXT_LAYER_ID = "exchange_comment_count_text"

fun createExchangeLayers(
    markerBackgroundColor: Int,
    markerIconColor: Int,
    markerBadgeBackgroundColor: Int,
    markerBadgeTextColor: Int,
    usingOpenFreeMap: Boolean,
): Pair<GeoJsonSource, List<Layer>> {
    val source = GeoJsonSource(
        id = "exchange",
        geoJson = EMPTY_GEOJSON,
        options = GeoJsonOptions().withCluster(true).withClusterMaxZoom(14).withClusterRadius(50),
    )

    val clusterBackground = CircleLayer("exchange_cluster_background", source.id).apply {
        setProperties(
            PropertyFactory.circleColor(markerBackgroundColor),
            PropertyFactory.circleRadius(23f),
        )
        setFilter(Expression.has("point_count"))
    }

    val clusterText =
        SymbolLayer(EXCHANGE_CLUSTER_TEXT_LAYER_ID, source.id).apply {
            if (usingOpenFreeMap) {
                setProperties(PropertyFactory.textFont(arrayOf("Noto Sans Bold")))
            }
            setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(18f),
                PropertyFactory.textColor(markerIconColor),
            )
        }

    val marker =
        SymbolLayer(EXCHANGE_MARKER_LAYER_ID, source.id).apply {
            setProperties(
                PropertyFactory.iconImage("btcmap-marker"),
                PropertyFactory.iconAnchor(Expression.literal("bottom")),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
            setFilter(
                Expression.neq(Expression.get("cluster"), true)
            )
        }

    val markerIcon =
        SymbolLayer("exchange_marker_icon", source.id).apply {
            setProperties(
                PropertyFactory.iconImage(
                    Expression.concat(
                        Expression.literal(EXCHANGE_MARKER_ICON_PREFIX),
                        Expression.get("iconId"),
                    )
                ),
                PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
                PropertyFactory.iconOffset(
                    arrayOf(
                        0f,
                        ICON_OFFSET_Y
                    )
                ),
                PropertyFactory.iconOpacity(
                    Expression.switchCase(
                        Expression.get("outdated"),
                        Expression.literal(OUTDATED_ICON_ALPHA),
                        Expression.literal(1f)
                    )
                ),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
            setFilter(
                Expression.neq(Expression.get("cluster"), true)
            )
        }

    val commentCountBackground =
        CircleLayer("exchange_comment_count_background", source.id).apply {
            setProperties(
                PropertyFactory.circleColor(markerBadgeBackgroundColor),
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleOpacity(1f),
                PropertyFactory.circleTranslate(arrayOf(13f, -43f)),
                PropertyFactory.circleTranslateAnchor("viewport")
            )
            setFilter(
                Expression.all(
                    Expression.neq(Expression.get("cluster"), true),
                    Expression.gt(Expression.get("comments"), 0)
                )
            )
        }

    val commentCountText =
        SymbolLayer(EXCHANGE_COMMENT_COUNT_TEXT_LAYER_ID, source.id).apply {
            if (usingOpenFreeMap) {
                setProperties(PropertyFactory.textFont(arrayOf("Noto Sans Bold")))
            }
            setProperties(
                PropertyFactory.textField(
                    Expression.switchCase(
                        Expression.gte(Expression.get("comments"), Expression.literal(10)),
                        Expression.literal("9+"),
                        Expression.toString(Expression.get("comments"))
                    )
                ),
                PropertyFactory.textSize(11f),
                PropertyFactory.textColor(markerBadgeTextColor),
                PropertyFactory.textTranslate(arrayOf(13f, -43f)),
                PropertyFactory.textTranslateAnchor("viewport"),
                PropertyFactory.textAllowOverlap(true)
            )
            setFilter(
                Expression.all(
                    Expression.neq(Expression.get("cluster"), true),
                    Expression.gt(Expression.get("comments"), 0)
                )
            )
        }

    return Pair(
        source,
        listOf(
            clusterBackground,
            clusterText,
            marker,
            markerIcon,
            commentCountBackground,
            commentCountText
        )
    )
}