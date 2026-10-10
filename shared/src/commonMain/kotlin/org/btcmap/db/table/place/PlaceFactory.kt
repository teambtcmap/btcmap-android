package org.btcmap.db.table.place

import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import org.btcmap.util.toUrlOrNull

/**
 * Builds a [Place] from the values both the sync delta and the bundled snapshot
 * can supply, so the two mappers cannot drift in how a place's URL fields are
 * normalized. Dates are passed already parsed: the two paths parse them with
 * different strictness and validate required fields themselves.
 */
internal fun placeOf(
    id: Long,
    updatedAt: Instant,
    lat: Double,
    lon: Double,
    icon: String,
    name: String?,
    localizedName: JsonObject?,
    verifiedAt: Instant?,
    address: String?,
    openingHours: String?,
    phone: String?,
    website: String?,
    email: String?,
    twitter: String?,
    facebook: String?,
    instagram: String?,
    line: String?,
    requiredAppUrl: String?,
    boostedUntil: Instant?,
    comments: Long?,
    telegram: String?,
    osmId: String?,
    deletedAt: Instant? = null,
): Place = Place(
    id = id,
    updatedAt = updatedAt,
    lat = lat,
    lon = lon,
    icon = icon,
    name = name,
    localizedName = localizedName,
    verifiedAt = verifiedAt,
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
    boostedUntil = boostedUntil,
    comments = comments,
    telegram = telegram?.toUrlOrNull(),
    osmId = osmId,
    deletedAt = deletedAt,
)
