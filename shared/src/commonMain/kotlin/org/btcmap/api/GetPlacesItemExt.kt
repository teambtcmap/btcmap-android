package org.btcmap.api

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.btcmap.db.table.place.FullProjection
import org.btcmap.util.toInstant
import org.btcmap.util.toUrlOrNull

fun GetPlacesItem.toPlace(): FullProjection {
    return FullProjection(
        id = id,
        updatedAt = updatedAt.toInstant(),
        lat = lat,
        lon = lon,
        icon = icon,
        name = name,
        localizedName = localizedName,
        verifiedAt = verifiedAt?.toVerifiedAt(),
        address = address,
        openingHours = openingHours,
        phone = phone,
        website = website?.toUrlOrNull(),
        email = email,
        twitter = twitter?.toUrlOrNull(),
        facebook = facebook?.toUrlOrNull(),
        instagram = instagram?.toUrlOrNull(),
        line = line?.toUrlOrNull(),
        requiredAppUrl = requiredAppUrl?.toUrlOrNull(),
        boostedUntil = boostedUntil?.toInstant(),
        comments = comments,
        telegram = telegram?.toUrlOrNull(),
        osmId = osmId,
        deletedAt = deletedAt?.toInstant(),
    )
}

fun String.toVerifiedAt(): Instant {
    // A date-only value gets midnight UTC; anything else is a full timestamp.
    // Choosing on the date/time separator instead of trying LocalDate first
    // keeps a full timestamp from paying for a thrown and caught parse
    // exception, which costs far more than the parse itself.
    return if (contains('T')) {
        Instant.parse(this)
    } else {
        LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC)
    }
}
