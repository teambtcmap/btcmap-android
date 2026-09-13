package org.btcmap.map

import org.btcmap.db.table.place.Marker
import java.time.ZonedDateTime

private const val OUTDATED_AFTER_YEARS = 1L
const val MAX_COMMENT_BADGE = 9L

fun Marker.isOutdated(now: ZonedDateTime = ZonedDateTime.now()): Boolean {
    return !bundled && (verifiedAt == null || verifiedAt.isBefore(now.minusYears(OUTDATED_AFTER_YEARS)))
}

fun Marker.markerImageName(): String {
    return merchantMarkerImageName(
        iconId = icon,
        boosted = boostedUntil != null,
        outdated = isOutdated(),
        comments = comments,
    )
}

fun merchantMarkerImageName(
    iconId: String,
    boosted: Boolean,
    outdated: Boolean,
    comments: Long,
): String {
    val variant = when {
        outdated -> "-outdated"
        boosted -> "-boosted"
        else -> ""
    }
    val badge = when {
        comments <= 0 -> ""
        comments > MAX_COMMENT_BADGE -> "-b9p"
        else -> "-b$comments"
    }
    return "merchant-marker-$iconId$variant$badge"
}

fun Iterable<Marker>.toMarkerGeoJson(): String {
    val now = ZonedDateTime.now()
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
                    "boosted": ${place.boostedUntil != null},
                    "outdated": ${place.isOutdated(now)},
                    "sortKey": ${-place.lat}
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
