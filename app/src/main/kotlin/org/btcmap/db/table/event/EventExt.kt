package org.btcmap.db.table.event

import org.btcmap.db.table.area.AreaGeometry

/**
 * Whether the event's location falls inside the area geometry.
 *
 * The server associates an event with an area by pre-filtering on the area's
 * bbox and then testing the point against its GeoJSON; the area screen and the
 * map chips repeat the same rule over the local cache, so the point test lives
 * here instead of in each caller. The bbox pre-filter stays with the caller:
 * the area screen queries a single area's bbox while the map unions the boxes
 * of every candidate area.
 *
 * [geometry] is passed in rather than parsed from an area so a caller that
 * tests many events against the same area parses the GeoJSON once.
 */
internal fun Event.isWithin(geometry: AreaGeometry): Boolean = geometry.contains(lat, lon)
