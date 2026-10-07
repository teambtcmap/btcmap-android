package org.btcmap.map

const val EMPTY_GEOJSON = """{"type":"FeatureCollection","features":[]}"""
const val ICON_OFFSET_Y = -29f

/**
 * The default map view before anything else sets one: Willemstad, Curaçao — the
 * island the app's original default viewport framed — at the zoom that viewport
 * worked out to. Android falls back to this when no camera has been saved, and
 * the desktop opens here.
 */
const val DEFAULT_MAP_CENTER_LAT = 12.116667
const val DEFAULT_MAP_CENTER_LON = -68.90333
const val DEFAULT_MAP_ZOOM = 12.0

/** North: the rotation the map resets to when it is set to stop rotating. */
const val DEFAULT_MAP_BEARING = 0.0

/** Straight down: the tilt the map resets to when it is set to stop tilting. */
const val DEFAULT_MAP_TILT = 0.0
