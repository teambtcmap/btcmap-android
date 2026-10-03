package org.btcmap.map

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.btcmap.db.table.place.Marker

private const val OUTDATED_AFTER_YEARS = 1
private const val ESTIMATED_BYTES_PER_FEATURE = 320
const val MAX_COMMENT_BADGE = 9L
const val EXCHANGE_MARKER_ICON_PREFIX = "marker-icon-"
const val EVENT_MARKER_ICON_NAME = "marker-icon-event"
const val EVENT_ICON = "event"

fun Marker.isOutdated(now: Instant = Clock.System.now()): Boolean {
    val verifiedAt = verifiedAt
    return verifiedAt == null ||
        verifiedAt < now.minus(DateTimePeriod(years = OUTDATED_AFTER_YEARS), TimeZone.UTC)
}

fun Marker.isBoosted(now: Instant = Clock.System.now()): Boolean {
    return boostedUntil?.let { it > now } == true
}

fun Marker.markerImageName(now: Instant = Clock.System.now()): String {
    return merchantMarkerImageName(
        iconId = icon,
        boosted = isBoosted(now),
        outdated = isOutdated(now),
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

fun exchangeMarkerIconImageName(iconId: String): String {
    return "$EXCHANGE_MARKER_ICON_PREFIX$iconId"
}

fun Iterable<Marker>.toMarkerGeoJson(now: Instant = Clock.System.now()): String {
    val sizeHint = if (this is Collection<*>) size else 0
    val sb = StringBuilder(sizeHint * ESTIMATED_BYTES_PER_FEATURE + 64)

    sb.append("{\"type\":\"FeatureCollection\",\"features\":[")

    forEachIndexed { index, place ->
        if (index > 0) sb.append(',')

        sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
        sb.append(place.lon)
        sb.append(',')
        sb.append(place.lat)
        sb.append("]},\"properties\":{\"id\":")
        sb.append(place.id)
        sb.append(",\"count\":1,\"iconId\":")
        sb.appendJsonString(place.icon)
        sb.append(",\"comments\":")
        sb.append(place.comments)
        sb.append(",\"boosted\":")
        sb.append(place.isBoosted(now))
        sb.append(",\"outdated\":")
        sb.append(place.isOutdated(now))
        sb.append(",\"sortKey\":")
        sb.append(-place.lat)
        sb.append("}}")
    }

    sb.append("]}")

    return sb.toString()
}
