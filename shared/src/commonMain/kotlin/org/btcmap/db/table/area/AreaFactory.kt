package org.btcmap.db.table.area

import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject

/**
 * The one place an [Area] is built from an API delta or a bundled snapshot.
 * Dates are passed already parsed; the bbox corners are passed already split
 * from the source's `bbox`, and the two paths normalize `verified_at` and
 * `geo_json` themselves.
 */
internal fun areaOf(
    id: Long,
    name: String,
    type: String,
    urlAlias: String,
    icon: String?,
    iconWide: String?,
    websiteUrl: String,
    description: String?,
    bboxWest: Double?,
    bboxSouth: Double?,
    bboxEast: Double?,
    bboxNorth: Double?,
    geoJson: String?,
    updatedAt: Instant = Instant.fromEpochSeconds(0),
    deletedAt: Instant? = null,
    localizedName: JsonObject? = null,
    localizedDescription: JsonObject? = null,
    verifiedAt: String? = null,
): Area = Area(
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
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    localizedName = localizedName,
    localizedDescription = localizedDescription,
    verifiedAt = verifiedAt,
)
