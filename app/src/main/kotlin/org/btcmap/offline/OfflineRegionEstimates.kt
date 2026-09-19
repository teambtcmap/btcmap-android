package org.btcmap.offline

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Network-free estimates of how much disk an offline tile pyramid for an area
 * would need.
 *
 * MapLibre can only report the real size after a download has started, so the
 * user picks a maximum zoom against an estimate. Tile counts are exact (Web
 * Mercator), tile sizes are not: they are conservative per-zoom averages in the
 * range typical OpenFreeMap vector tiles occupy, so a dense city is unlikely to
 * exceed the shown figure by much while rural areas are overestimated on
 * purpose. The UI always presents the result as approximate.
 */
internal object OfflineRegionEstimates {

    /** Every pack includes the low zooms so the user can zoom out. */
    const val MIN_ZOOM = 0

    /** Lowest maximum zoom offered; below this even a country stays tiny. */
    const val MIN_SELECTABLE_MAX_ZOOM = 8

    /** Highest maximum zoom offered; beyond this the tile count explodes. */
    const val MAX_SELECTABLE_MAX_ZOOM = 16

    /** The slider's upper bound never estimates more than this. */
    private const val MAX_ESTIMATED_BYTES = 1L shl 30 // 1 GiB

    /** The default selection aims to stay under this. */
    private const val DEFAULT_ESTIMATED_BYTES = 250L shl 20 // 250 MiB

    private const val MAX_MERCATOR_LATITUDE = 85.05112878

    fun tileCount(bounds: OfflineBounds, zoom: Int): Long {
        val tilesPerAxis = 1L shl zoom
        val minX = longitudeToTile(bounds.west, zoom).coerceIn(0L, tilesPerAxis - 1)
        val maxX = longitudeToTile(bounds.east, zoom).coerceIn(0L, tilesPerAxis - 1)
        // North is the smaller y in Web Mercator, south the larger one.
        val minY = latitudeToTile(bounds.north, zoom).coerceIn(0L, tilesPerAxis - 1)
        val maxY = latitudeToTile(bounds.south, zoom).coerceIn(0L, tilesPerAxis - 1)
        val width = (maxX - minX + 1).coerceAtLeast(1L)
        val height = (maxY - minY + 1).coerceAtLeast(1L)
        return width * height
    }

    fun estimatedBytes(bounds: OfflineBounds, minZoom: Int, maxZoom: Int): Long {
        var bytes = 0L
        for (zoom in minZoom..maxZoom) {
            bytes += tileCount(bounds, zoom) * averageTileBytes(zoom)
        }
        return bytes
    }

    /**
     * The highest maximum zoom the slider offers: the largest zoom whose
     * estimate still fits [MAX_ESTIMATED_BYTES], never below
     * [MIN_SELECTABLE_MAX_ZOOM].
     */
    fun maxSelectableZoom(bounds: OfflineBounds): Int {
        for (zoom in MAX_SELECTABLE_MAX_ZOOM downTo MIN_SELECTABLE_MAX_ZOOM) {
            if (estimatedBytes(bounds, MIN_ZOOM, zoom) <= MAX_ESTIMATED_BYTES) return zoom
        }
        return MIN_SELECTABLE_MAX_ZOOM
    }

    /** The preselected maximum zoom: the largest one under [DEFAULT_ESTIMATED_BYTES]. */
    fun defaultMaxZoom(bounds: OfflineBounds): Int {
        val maxSelectable = maxSelectableZoom(bounds)
        for (zoom in maxSelectable downTo MIN_SELECTABLE_MAX_ZOOM) {
            if (estimatedBytes(bounds, MIN_ZOOM, zoom) <= DEFAULT_ESTIMATED_BYTES) return zoom
        }
        return MIN_SELECTABLE_MAX_ZOOM
    }

    /**
     * False when even the lowest selectable zoom exceeds [MAX_ESTIMATED_BYTES].
     * Continental areas fall in this bucket and must not be offered for download.
     */
    fun isWithinLimit(bounds: OfflineBounds): Boolean {
        return estimatedBytes(bounds, MIN_ZOOM, maxSelectableZoom(bounds)) <= MAX_ESTIMATED_BYTES
    }

    /**
     * A conservative average decoded vector-tile size in bytes for [zoom]. The
     * values grow with zoom up to the point where tiles cover a small enough
     * patch that their feature count drops, roughly matching observed
     * OpenFreeMap tiles.
     */
    fun averageTileBytes(zoom: Int): Long {
        val kibibytes = when {
            zoom <= 6 -> 500L
            zoom == 7 -> 400L
            zoom == 8 -> 300L
            zoom == 9 -> 250L
            zoom == 10 -> 180L
            zoom == 11 -> 140L
            zoom == 12 -> 120L
            zoom == 13 -> 130L
            zoom == 14 -> 160L
            zoom == 15 -> 190L
            else -> 220L
        }
        return kibibytes * 1024L
    }

    private fun longitudeToTile(lon: Double, zoom: Int): Long {
        val tiles = 1L shl zoom
        return floor((lon + 180.0) / 360.0 * tiles).toLong()
    }

    private fun latitudeToTile(lat: Double, zoom: Int): Long {
        val clamped = lat.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val radians = Math.toRadians(clamped)
        val tiles = 1L shl zoom
        val mercator = 1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI
        return floor(mercator / 2.0 * tiles).toLong()
    }
}
