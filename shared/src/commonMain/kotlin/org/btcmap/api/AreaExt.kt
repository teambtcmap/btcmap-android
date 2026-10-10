package org.btcmap.api

import kotlinx.datetime.LocalDate
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.areaOf
import org.btcmap.util.toInstant

/**
 * The area row both the delta sync and the bundled seed store, so the two paths
 * map the API's fields the same way.
 */
internal fun GetAreasDeltaItem.toArea(): Area = areaOf(
    id = id,
    name = name,
    type = type,
    urlAlias = urlAlias,
    icon = icon,
    iconWide = iconWide,
    websiteUrl = websiteUrl,
    description = description,
    bboxWest = bboxWest,
    bboxSouth = bboxSouth,
    bboxEast = bboxEast,
    bboxNorth = bboxNorth,
    geoJson = geoJson,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
    localizedName = localizedName,
    localizedDescription = localizedDescription,
    verifiedAt = verifiedAt?.toIsoDateOrNull(),
)

/** The canonical ISO-8601 date for a `verified_at`, or null if malformed. */
internal fun String.toIsoDateOrNull(): String? =
    runCatching { LocalDate.parse(this).toString() }.getOrNull()
