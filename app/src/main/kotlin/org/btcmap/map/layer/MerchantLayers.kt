package org.btcmap.map.layer

import android.graphics.Color
import org.btcmap.map.MAX_COMMENT_BADGE
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

const val MERCHANT_MARKER_LAYER_ID = "merchant_marker"

fun createMerchantLayers(
    markerBackgroundColor: Int,
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

    val markerImageExpression = Expression.concat(
        Expression.literal("merchant-marker-"),
        Expression.get("iconId"),
        Expression.switchCase(
            Expression.get("outdated"),
            Expression.literal("-outdated"),
            Expression.get("boosted"),
            Expression.literal("-boosted"),
            Expression.literal("")
        ),
        Expression.switchCase(
            Expression.gt(Expression.get("comments"), Expression.literal(0)),
            Expression.concat(
                Expression.literal("-b"),
                Expression.switchCase(
                    Expression.gt(
                        Expression.get("comments"),
                        Expression.literal(MAX_COMMENT_BADGE)
                    ),
                    Expression.literal("9p"),
                    Expression.toString(Expression.get("comments"))
                )
            ),
            Expression.literal("")
        )
    )

    val markerLayer =
        SymbolLayer(MERCHANT_MARKER_LAYER_ID, merchantsSource.id).apply {
            setProperties(
                PropertyFactory.iconImage(markerImageExpression),
                PropertyFactory.iconAnchor(Expression.literal("bottom")),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.symbolSortKey(Expression.get("sortKey")),
                PropertyFactory.iconOpacity(
                    Expression.switchCase(
                        Expression.get("outdated"),
                        Expression.literal(0.85f),
                        Expression.literal(1f)
                    )
                )
            )
            setFilter(Expression.neq(Expression.get("cluster"), true))
        }

    return Pair(
        merchantsSource, listOf(
            clusterBackgroundLayer,
            clusterCountLayer,
            markerLayer,
        )
    )
}