package org.btcmap.offline

import com.google.gson.Gson

/**
 * The bytes stored alongside an offline region so the app can tell which area
 * and map style a pack belongs to.
 *
 * MapLibre keeps regions in its own database and only gives back opaque
 * metadata, so this is the only link between a pack and an [org.btcmap.db.table.area.Area].
 * Storing it here avoids a database migration.
 */
internal data class OfflineRegionMetadata(
    val areaId: Long,
    val areaName: String,
    val styleUrl: String,
    val maxZoom: Int,
)

private val gson = Gson()

internal fun OfflineRegionMetadata.toBytes(): ByteArray =
    gson.toJson(this).toByteArray(Charsets.UTF_8)

/**
 * Parses [bytes] written by [toBytes]. Returns null for metadata this app did
 * not write, or that was written by a version whose shape no longer matches, so
 * an unknown pack is ignored rather than crashing.
 */
internal fun parseOfflineRegionMetadata(bytes: ByteArray?): OfflineRegionMetadata? {
    val json = bytes?.toString(Charsets.UTF_8) ?: return null
    val parsed =
        runCatching { gson.fromJson(json, OfflineRegionMetadataJson::class.java) }.getOrNull()
            ?: return null
    val areaName = parsed.areaName ?: return null
    val styleUrl = parsed.styleUrl ?: return null
    if (parsed.areaId <= 0L || parsed.maxZoom <= 0) return null
    return OfflineRegionMetadata(
        areaId = parsed.areaId,
        areaName = areaName,
        styleUrl = styleUrl,
        maxZoom = parsed.maxZoom,
    )
}

// Nullable so a partially written or foreign payload is rejected rather than
// silently producing a metadata object with null fields.
private data class OfflineRegionMetadataJson(
    val areaId: Long = 0L,
    val areaName: String? = null,
    val styleUrl: String? = null,
    val maxZoom: Int = 0,
)
