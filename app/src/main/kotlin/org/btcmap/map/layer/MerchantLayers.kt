package org.btcmap.map.layer

import android.graphics.Color
import org.btcmap.map.ICON_OFFSET_Y
import org.btcmap.map.matcher
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

const val MERCHANT_MARKER_LAYER_ID = "merchant_marker"

fun createMerchantLayers(
    markerBackgroundColor: Int,
    markerBadgeBackgroundColor: Int,
    markerBadgeTextColor: Int,
    usingOpenFreeMap: Boolean,
): Pair<GeoJsonSource, List<Layer>> {
    val merchantsSource = GeoJsonSource(
        "merchant",
        """{"type":"FeatureCollection","features":[]}""",
        GeoJsonOptions()
            .withCluster(true)
            .withClusterMaxZoom(14)
            .withClusterRadius(50)
    )

    val clusterBackgroundLayer =
        CircleLayer("merchant_cluster_background", merchantsSource.id).apply {
            setProperties(
                PropertyFactory.circleColor(markerBackgroundColor),
                PropertyFactory.circleRadius(23f),
            )
            val pointCount = Expression.toNumber(Expression.get("point_count"))
            setFilter(
                Expression.all(
                    Expression.has("point_count"),
                    Expression.gte(
                        pointCount,
                        Expression.literal(1)
                    )
                )
            )
        }

    val clusterCountLayer =
        SymbolLayer("merchant_cluster_count", merchantsSource.id).apply {
            if (usingOpenFreeMap) {
                setProperties(PropertyFactory.textFont(arrayOf("Noto Sans Regular")))
            }

            setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(16f),
                PropertyFactory.textColor(Color.WHITE),
            )
        }

    val markerLayer =
        SymbolLayer(MERCHANT_MARKER_LAYER_ID, merchantsSource.id).apply {
            setProperties(
                PropertyFactory.iconImage(
                    Expression.switchCase(
                        Expression.get("boosted"),
                        Expression.literal("btcmap-marker-boosted"),
                        Expression.literal("btcmap-marker")
                    )
                ),
                PropertyFactory.iconAnchor(Expression.literal("bottom")),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
            setFilter(
                Expression.neq(Expression.get("cluster"), true)
            )
        }

    val markerIconsLayer =
        SymbolLayer("merchant_marker_icon", merchantsSource.id).apply {
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
            setFilter(Expression.neq(Expression.get("cluster"), true))
        }

    val markerCommentCountBackground =
        CircleLayer("merchant_marker_comment_count_background", merchantsSource.id).apply {
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

    val markerCommentsCount =
        SymbolLayer("merchant_marker_comment_count", merchantsSource.id).apply {
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
        merchantsSource, listOf(
            clusterBackgroundLayer,
            clusterCountLayer,
            markerLayer,
            markerIconsLayer,
            markerCommentCountBackground,
            markerCommentsCount,
        )
    )
}