package org.btcmap.map

import org.btcmap.db.table.note.Note

/**
 * The signed-in user's notes as a GeoJSON FeatureCollection for the map's note
 * layer. Each feature is a point at the note's coordinate carrying its `id` and
 * `icon`, so a tap can be resolved back to the cached note and the layer can
 * pick the matching pin.
 */
fun Iterable<Note>.toNoteGeoJson(): String {
    val sizeHint = if (this is Collection<*>) size else 0
    val sb = StringBuilder(sizeHint * 96 + 64)

    sb.append("{\"type\":\"FeatureCollection\",\"features\":[")

    forEachIndexed { index, note ->
        if (index > 0) sb.append(',')

        sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
        sb.append(note.lon)
        sb.append(',')
        sb.append(note.lat)
        sb.append("]},\"properties\":{\"id\":")
        sb.append(note.id)
        sb.append(",\"icon\":")
        sb.appendJsonString(note.icon)
        sb.append("}}")
    }

    sb.append("]}")

    return sb.toString()
}
