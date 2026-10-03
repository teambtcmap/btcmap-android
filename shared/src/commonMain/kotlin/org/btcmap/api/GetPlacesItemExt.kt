package org.btcmap.api

import org.btcmap.db.table.place.FullProjection
import org.btcmap.util.toUrlOrNull
import org.btcmap.util.toZonedDateTime
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun GetPlacesItem.toPlace(): FullProjection {
    return FullProjection(
        id = id,
        updatedAt = updatedAt.toZonedDateTime(),
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
        boostedUntil = boostedUntil?.toZonedDateTime(),
        comments = comments,
        telegram = telegram?.toUrlOrNull(),
        osmId = osmId,
        deletedAt = deletedAt?.toZonedDateTime(),
    )
}

fun String.toVerifiedAt(): ZonedDateTime {
    // A date-only value gets midnight UTC; anything else is a full timestamp.
    // Choosing on the date/time separator instead of trying LocalDate first
    // keeps a full timestamp from paying for a thrown and caught
    // DateTimeParseException, which costs far more than the parse itself.
    return if (contains('T')) {
        ZonedDateTime.parse(this)
    } else {
        LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC)
    }
}
