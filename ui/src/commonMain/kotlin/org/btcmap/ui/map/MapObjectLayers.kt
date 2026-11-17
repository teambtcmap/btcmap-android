package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LayerDefaults
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource

const val MAP_CLUSTER_BACKGROUND_LAYER_ID = "map_cluster_background"
const val MAP_CLUSTER_COUNT_LAYER_ID = "map_cluster_count"

/**
 * Every visible map object kind drawn from one clustered source. Keeping the
 * kinds in a single source is what lets a cluster count a merchant, an event, an
 * exchange and a note together; separate sources each clustered only their own
 * kind.
 *
 * The source's features carry a `kind` property, and each kind's marker layers
 * filter on it, so the shared source still draws the right pin, glyph and badge
 * for every object. Hidden kinds are left out of the source by the caller, so a
 * cluster never counts an object the user has toggled off.
 */
@Composable
fun MapObjectLayers(
    geoJson: String,
    visibleKinds: Set<MarkerKind>,
    clusterBackgroundColor: Color,
    clusterTextColor: Color,
    badgeBackgroundColor: Color,
    badgeTextColor: Color,
    usingOpenFreeMap: Boolean,
    showMarkers: Boolean,
    onMarkerClick: MarkerClickHandler,
    onNoteClick: MarkerClickHandler,
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson)) {
        cluster = true
        clusterMaxZoom = 14
        clusterRadius = 50
    }

    CircleLayer(
        id = MAP_CLUSTER_BACKGROUND_LAYER_ID,
        source = source,
        filter = Feature.has("point_count"),
        color = const(clusterBackgroundColor),
        radius = const(23.dp),
    )

    SymbolLayer(
        id = MAP_CLUSTER_COUNT_LAYER_ID,
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
        if (MarkerKind.Merchants in visibleKinds) {
            MerchantMarkerLayers(source = source, onMarkerClick = onMarkerClick)
        }
        if (MarkerKind.Events in visibleKinds) {
            EventMarkerLayers(source = source, onMarkerClick = onMarkerClick)
        }
        if (MarkerKind.Exchanges in visibleKinds) {
            ExchangeMarkerLayers(
                source = source,
                badgeBackgroundColor = badgeBackgroundColor,
                badgeTextColor = badgeTextColor,
                onMarkerClick = onMarkerClick,
            )
        }
        if (MarkerKind.Notes in visibleKinds) {
            NoteMarkerLayers(source = source, onClick = onNoteClick)
        }
    }
}
