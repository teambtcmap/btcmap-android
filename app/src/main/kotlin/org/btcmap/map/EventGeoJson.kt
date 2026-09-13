package org.btcmap.map

import org.btcmap.db.table.event.Event

fun Iterable<Event>.toEventGeoJson(): String {
    val sizeHint = if (this is Collection<*>) size else 0
    val sb = StringBuilder(sizeHint * 128 + 64)

    sb.append("{\"type\":\"FeatureCollection\",\"features\":[")

    forEachIndexed { index, event ->
        if (index > 0) sb.append(',')

        sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
        sb.append(event.lon)
        sb.append(',')
        sb.append(event.lat)
        sb.append("]},\"properties\":{\"id\":")
        sb.append(event.id)
        sb.append("}}")
    }

    sb.append("]}")

    return sb.toString()
}
