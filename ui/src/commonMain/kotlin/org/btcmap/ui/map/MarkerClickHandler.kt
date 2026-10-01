package org.btcmap.ui.map

import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry

/** Called with the features of a marker layer that a tap hit. */
typealias MarkerClickHandler = (List<Feature<Geometry, JsonObject?>>) -> ClickResult
