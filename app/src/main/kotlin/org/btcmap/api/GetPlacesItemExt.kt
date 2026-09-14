package org.btcmap.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.db.table.place.FullProjection
import org.btcmap.util.toZonedDateTime

fun GetPlacesItem.toPlace(): FullProjection {
    return FullProjection(
        id = id,
        bundled = false,
        updatedAt = updatedAt.toZonedDateTime(),
        lat = lat,
        lon = lon,
        icon = icon,
        name = name,
        localizedName = localizedName,
        verifiedAt = verifiedAt?.let { (it + "T00:00:00Z").toZonedDateTime() },
        address = address,
        openingHours = openingHours,
        localizedOpeningHours = localizedOpeningHours,
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
    )
}
