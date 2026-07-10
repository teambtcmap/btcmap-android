package org.btcmap.map

import org.btcmap.db.table.event.Event

fun Iterable<Event>.toEventGeoJson(): String {
    val sb = StringBuilder()
    sb.append(
        """
        {
            "type": "FeatureCollection",
            "features": [
        """.trimIndent()
    )

    this.forEachIndexed { index, event ->
        if (index > 0) {
            sb.append(",")
        }
        sb.append(
            """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${event.lon}, ${event.lat}]
                },
                "properties": {
                    "id": ${event.id}
                }
            }
        """.trimIndent()
        )
    }

    sb.append(
        """
            ]
        }
        """.trimIndent()
    )

    return sb.toString()
}
