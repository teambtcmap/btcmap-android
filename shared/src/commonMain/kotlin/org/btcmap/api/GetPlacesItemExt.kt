package org.btcmap.api

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.btcmap.db.table.place.FullProjection
import org.btcmap.db.table.place.placeOf
import org.btcmap.util.toInstant
import org.btcmap.util.toInstantOrNull

fun GetPlacesItem.toPlace(): FullProjection = placeOf(
    id = id,
    updatedAt = updatedAt.toInstant(),
    lat = lat,
    lon = lon,
    icon = icon,
    name = name,
    localizedName = localizedName,
    verifiedAt = verifiedAt?.toVerifiedAtOrNull(),
    address = address,
    openingHours = openingHours,
    phone = phone,
    website = website,
    email = email,
    twitter = twitter,
    facebook = facebook,
    instagram = instagram,
    line = line,
    requiredAppUrl = requiredAppUrl,
    boostedUntil = boostedUntil?.toInstantOrNull(),
    comments = comments,
    telegram = telegram,
    osmId = osmId,
    deletedAt = deletedAt?.toInstant(),
)

fun String.toVerifiedAt(): Instant {
    // A date-only value gets midnight UTC; anything else is a full timestamp.
    // Choosing on the date/time separator instead of trying LocalDate first
    // keeps a full timestamp from paying for a thrown and caught parse
    // exception, which costs far more than the parse itself.
    return if (contains('T')) {
        toInstant()
    } else {
        LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC)
    }
}

/**
 * A display-only verification date, degraded to null when malformed so a bad
 * value never rolls back a whole snapshot or sync page (see [toInstantOrNull]).
 */
fun String.toVerifiedAtOrNull(): Instant? = runCatching { toVerifiedAt() }.getOrNull()
