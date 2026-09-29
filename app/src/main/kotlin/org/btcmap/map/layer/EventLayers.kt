package org.btcmap.map.layer

import android.graphics.Color
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.ICON_OFFSET_Y
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

const val EVENT_MARKER_LAYER_ID = "event_marker"
const val EVENT_CLUSTER_COUNT_LAYER_ID = "event_cluster_count"

fun createEventLayers(
    markerBackgroundColor: Int,
    usingOpenFreeMap: Boolean,
): Pair<GeoJsonSource, List<Layer>> {
    val source = GeoJsonSource(
        id = "event",
        geoJson = EMPTY_GEOJSON,
        options = GeoJsonOptions().withCluster(true).withClusterMaxZoom(14).withClusterRadius(30),
    )

    val clusterBackground =
        CircleLayer("event_cluster_background", source.id).apply {
            setProperties(
                PropertyFactory.circleColor(markerBackgroundColor),
                PropertyFactory.circleRadius(23f),
            )
            setFilter(Expression.has("point_count"))
        }

    val clusterCount =
        SymbolLayer(EVENT_CLUSTER_COUNT_LAYER_ID, source.id).apply {
            if (usingOpenFreeMap) {
                setProperties(PropertyFactory.textFont(arrayOf("Noto Sans Regular")))
            }
            setProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(16f),
                PropertyFactory.textColor(Color.WHITE),
            )
        }

    val eventMarker =
        SymbolLayer(EVENT_MARKER_LAYER_ID, source.id).apply {
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

    val eventIcon =
        SymbolLayer("event_icon", source.id).apply {
            setProperties(
                PropertyFactory.iconImage(EVENT_MARKER_ICON_NAME),
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

    return Pair(source, listOf(clusterBackground, clusterCount, eventMarker, eventIcon))
}