package org.btcmap.settings

import org.btcmap.map.DEFAULT_MAP_BEARING
import org.btcmap.map.DEFAULT_MAP_CENTER_LAT
import org.btcmap.map.DEFAULT_MAP_CENTER_LON
import org.btcmap.map.DEFAULT_MAP_TILT
import org.btcmap.map.DEFAULT_MAP_ZOOM

/**
 * Where the map was last left, the full camera state. The map screen saves these
 * as the camera comes to rest and reopens there, so the app returns to what the
 * user was looking at, at the same angle and zoom.
 */
var Settings.mapCenterLat: Double
    get() = getFloat("mapCenterLat", DEFAULT_MAP_CENTER_LAT.toFloat()).toDouble()
    set(value) = putFloat("mapCenterLat", value.toFloat())

var Settings.mapCenterLon: Double
    get() = getFloat("mapCenterLon", DEFAULT_MAP_CENTER_LON.toFloat()).toDouble()
    set(value) = putFloat("mapCenterLon", value.toFloat())

var Settings.mapZoom: Double
    get() = getFloat("mapZoom", DEFAULT_MAP_ZOOM.toFloat()).toDouble()
    set(value) = putFloat("mapZoom", value.toFloat())

/** The camera's rotation, in degrees clockwise from north. */
var Settings.mapBearing: Double
    get() = getFloat("mapBearing", DEFAULT_MAP_BEARING.toFloat()).toDouble()
    set(value) = putFloat("mapBearing", value.toFloat())

/** The camera's pitch, in degrees from straight down. */
var Settings.mapTilt: Double
    get() = getFloat("mapTilt", DEFAULT_MAP_TILT.toFloat()).toDouble()
    set(value) = putFloat("mapTilt", value.toFloat())
