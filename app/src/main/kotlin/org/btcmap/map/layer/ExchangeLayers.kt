package org.btcmap.map.layer

import android.graphics.Color
import org.btcmap.map.MapFragment.Companion.ICON_OFFSET_Y
import org.btcmap.map.matcher
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

const val EXCHANGE_MARKER_LAYER_ID = "exchange_marker"

fun createExchangeLayers(
    markerBackgroundColor: Int,
    markerBadgeBackgroundColor: Int,
    markerBadgeTextColor: Int,
    usingOpenFreeMap: Boolean,
): Pair<GeoJsonSource, List<Layer>> {
    val source = GeoJsonSource(
        id = "exchange",
        geoJson = """{"type":"FeatureCollection","features":[]}""",
        options = GeoJsonOptions().withCluster(true).withClusterMaxZoom(14).withClusterRadius(50),
    )

    val clusterBackground = CircleLayer("exchange_cluster_background", source.id).apply {
        setProperties(
            PropertyFactory.circleColor(markerBackgroundColor),
            PropertyFactory.circleRadius(23f),
        )
        val pointCount = Expression.toNumber(Expression.get("point_count"))
        setFilter(
            Expression.all(
                Expression.has("point_count"), Expression.gte(
                    pointCount, Expression.literal(1)
                )
            )
        )
    }

    val clusterText =
        SymbolLayer("exchange_cluster_text", source.id).apply {
            if (usingOpenFreeMap) {
                setProperties(PropertyFactory.textFont(arrayOf("Noto Sans Regular")))
            }
            setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(16f),
                PropertyFactory.textColor(Color.WHITE),
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
                    Expression.match(
                        Expression.get("iconId"),
                        *matcher().toTypedArray()
                    )
                ),
                PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
                PropertyFactory.iconOffset(
                    arrayOf(
                        0f,
                        ICON_OFFSET_Y
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
        SymbolLayer("exchange_comment_count_text", source.id).apply {
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