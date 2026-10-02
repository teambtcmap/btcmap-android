package org.btcmap.ui.map

import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.interaction.ClickEvent
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry

/**
 * Called with the features of a marker layer that a tap hit. Since 0.19 the
 * layer click handler is a `ClickEvent` extension, so the receiver carries where
 * the tap landed; the handler here does not need it.
 */
typealias MarkerClickHandler = ClickEvent.(List<Feature<Geometry, JsonObject?>>) -> ClickResult
