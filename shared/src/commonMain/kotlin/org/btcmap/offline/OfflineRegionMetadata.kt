package org.btcmap.offline

import kotlinx.serialization.Serializable
import org.btcmap.json.btcmapJson

/**
 * The bytes stored alongside an offline region so the app can tell which area
 * and map style a pack belongs to.
 *
 * MapLibre keeps regions in its own database and only gives back opaque
 * metadata, so this is the only link between a pack and an [org.btcmap.db.table.area.Area].
 * Storing it here avoids a database migration.
 */
@Serializable
data class OfflineRegionMetadata(
    val areaId: Long,
    val areaName: String,
    val styleUrl: String,
    val maxZoom: Int,
)

fun OfflineRegionMetadata.toBytes(): ByteArray =
    btcmapJson.encodeToString(this).toByteArray(Charsets.UTF_8)

/**
 * Parses [bytes] written by [toBytes]. Returns null for metadata this app did
 * not write, or that was written by a version whose shape no longer matches, so
 * an unknown pack is ignored rather than crashing.
 */
fun parseOfflineRegionMetadata(bytes: ByteArray?): OfflineRegionMetadata? {
    val json = bytes?.toString(Charsets.UTF_8) ?: return null
    val parsed =
        runCatching { btcmapJson.decodeFromString<OfflineRegionMetadataJson>(json) }.getOrNull()
            ?: return null
    val areaId = parsed.areaId ?: return null
    val areaName = parsed.areaName ?: return null
    val styleUrl = parsed.styleUrl ?: return null
    val maxZoom = parsed.maxZoom ?: return null
    if (areaId <= 0L) return null
    // The download dialog only ever writes a selectable maximum zoom, so a value
    // outside that range means foreign or corrupt metadata.
    if (maxZoom !in 1..OfflineRegionEstimates.MAX_SELECTABLE_MAX_ZOOM) return null
    return OfflineRegionMetadata(
        areaId = areaId,
        areaName = areaName,
        styleUrl = styleUrl,
        maxZoom = maxZoom,
    )
}

// Nullable so a partially written or foreign payload is rejected rather than
// silently producing a metadata object with null fields.
@Serializable
private data class OfflineRegionMetadataJson(
    val areaId: Long? = null,
    val areaName: String? = null,
    val styleUrl: String? = null,
    val maxZoom: Int? = null,
)
