package org.btcmap.map

import org.btcmap.db.table.place.Marker
import java.time.ZonedDateTime

fun Iterable<Marker>.toMarkerGeoJson(): String {
    val outdatedThreshold = ZonedDateTime.now().minusYears(1)
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
        val outdated = place.verifiedAt == null || place.verifiedAt.isBefore(outdatedThreshold)
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
                    "boosted": ${place.boostedUntil != null},
                    "outdated": $outdated
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
