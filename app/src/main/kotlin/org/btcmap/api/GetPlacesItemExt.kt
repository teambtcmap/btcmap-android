package org.btcmap.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.db.table.place.FullProjection
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
        website = website?.toHttpUrlOrNull(),
        email = email,
        twitter = twitter?.toHttpUrlOrNull(),
        facebook = facebook?.toHttpUrlOrNull(),
        instagram = instagram?.toHttpUrlOrNull(),
        line = line?.toHttpUrlOrNull(),
        requiredAppUrl = requiredAppUrl?.toHttpUrlOrNull(),
        boostedUntil = boostedUntil?.toZonedDateTime(),
        comments = comments,
        telegram = telegram?.toHttpUrlOrNull(),
        osmId = osmId,
        deletedAt = deletedAt?.toZonedDateTime(),
    )
}

internal fun String.toVerifiedAt(): ZonedDateTime {
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
