package org.btcmap.map

import org.btcmap.db.table.place.Marker

fun Iterable<Marker>.toMarkerGeoJson(): String {
    val sb = StringBuilder()
    sb.append(
        """
        {
            "type": "FeatureCollection",
            "features": [
        """.trimIndent()
    )

    this.forEachIndexed { index, place ->
        if (index > 0) {
            sb.append(",")
        }
        sb.append(
            """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${place.lon}, ${place.lat}]
                },
                "properties": {
                    "id": ${place.id},
                    "count": 1,
                    "iconId": "${place.icon}",
                    "requiresCompanionApp": ${place.requiredAppUrl != null},
                    "comments": ${place.comments},
                    "boosted": ${place.boostedUntil != null}
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
