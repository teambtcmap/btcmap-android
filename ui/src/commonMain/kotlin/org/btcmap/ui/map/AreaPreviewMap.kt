package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.btcmap.db.table.area.Area
import org.btcmap.map.toRenderGeoJson
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

private const val AREA_PREVIEW_FILL_LAYER_ID = "area_preview_fill"
private const val AREA_PREVIEW_LINE_LAYER_ID = "area_preview_line"

/** The zoom the preview opens at before it is fitted to the area's bounds. */
private const val AREA_PREVIEW_ZOOM = 9.0

/** The fill's opacity, so the basemap shows through the polygon. */
private const val AREA_PREVIEW_FILL_OPACITY = 0.5f

/** The margin the fitted polygon keeps from the map widget's edges. */
private val AREA_PREVIEW_FIT_PADDING = 16.dp

/**
 * A quiet, non-interactive preview of the area's cached polygon, its camera
 * fitted to the area's own bounds with a [AREA_PREVIEW_FIT_PADDING] margin so the
 * polygon never touches the widget's edges. It drops the MapLibre UI (logo,
 * attribution and controls), like [EventMiniMap], so it reads as a static
 * thumbnail rather than a map to pan; the surrounding screen carries the app's
 * attribution.
 *
 * [borderColor] is the user's marker background colour, used for both the
 * polygon's outline and its half-opaque fill.
 */
@Composable
fun AreaPreviewMap(
    area: Area,
    styleUrl: String,
    styleJson: String?,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val bounds = area.cachedBounds()
    val geoJson = remember(area) { area.toRenderGeoJson() }

    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = bounds?.center() ?: Position(0.0, 0.0),
            zoom = AREA_PREVIEW_ZOOM,
        ),
    ) {
        val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))
        FillLayer(
            id = AREA_PREVIEW_FILL_LAYER_ID,
            source = source,
            color = const(borderColor),
            // The translucency must live on the layer's own `opacity`: a `Color`
            // alpha inside `const(...)` is dropped and the fill renders fully
            // transparent.
            opacity = const(AREA_PREVIEW_FILL_OPACITY),
        )
        LineLayer(
            id = AREA_PREVIEW_LINE_LAYER_ID,
            source = source,
            color = const(borderColor),
            width = const(3.dp),
        )
    }

    LaunchedEffect(state, bounds) {
        bounds?.let {
            state.fitCameraToBounds(
                it,
                fitPadding = DpPadding(
                    left = AREA_PREVIEW_FIT_PADDING,
                    top = AREA_PREVIEW_FIT_PADDING,
                    right = AREA_PREVIEW_FIT_PADDING,
                    bottom = AREA_PREVIEW_FIT_PADDING,
                ),
            )
        }
    }

    MaplibreMap(
        modifier = modifier,
        state = state,
        interactions = MapInteractions.None,
        uiOptions = MapUiOptions.None,
        overlay = {},
    )
}

/** The area's bounding box from its cached corners, or null when any is missing. */
private fun Area.cachedBounds(): BoundingBox? {
    val west = bboxWest ?: return null
    val south = bboxSouth ?: return null
    val east = bboxEast ?: return null
    val north = bboxNorth ?: return null
    return BoundingBox(west, south, east, north)
}

private fun BoundingBox.center(): Position =
    Position((west + east) / 2, (south + north) / 2)
