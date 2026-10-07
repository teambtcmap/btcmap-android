@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.btcmap.bundle

import kotlinx.datetime.LocalDate
import org.btcmap.util.useSource
import org.btcmap.platform.ioDispatcher
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.Source
import okio.buffer
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toInstantOrNull

/**
 * Seeds the areas table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries the full field set the app syncs, including the full
 * `geo_json` polygon and each area's real `updated_at`, so a seeded row is a
 * complete record rather than a placeholder. The first sync therefore only
 * fetches the changes made since the snapshot was generated, and community and
 * country chips — with the polygons they are matched against — work offline, or
 * while the server is unreachable, without downloading megabytes of geometry
 * first.
 *
 * The snapshot is decoded record by record from an [okio.Source], so it is
 * never buffered whole in memory.
 */
object BundledAreas {
    const val FILE_NAME = "bundled-areas.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val areasImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds [db] from the snapshot produced by [openSource], unless it already
     * holds areas. A null [openSource] result means the optional asset is
     * absent, which is not an error.
     */
    suspend fun import(
        db: Database,
        openSource: () -> Source?,
    ): ImportResult {
        val startedAt = TimeSource.Monotonic.markNow()

        // Seeding is intentionally a one-shot, fresh-install operation: any area
        // already stored — tombstone included — means the snapshot was imported
        // or live data was synced. Re-importing a newer asset later is
        // deliberately avoided so a stale bundle can never overwrite rows that
        // sync has since refreshed, and counting tombstones stops a table that
        // holds only deleted areas from being re-seeded into resurrecting them.
        var areasImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            val areasInDb = withContext(ioDispatcher) {
                db.area.selectCount(includeDeleted = true)
            }
            if (areasInDb > 0) {
                return ImportResult(areasImported = 0, duration = startedAt.elapsedNow())
            }

            // The whole parse runs inside one transaction so a malformed asset
            // rolls back to an empty table and is retried on the next launch,
            // instead of leaving a partial seed that the count check above would
            // then treat as complete.
            withContext(ioDispatcher) {
                val source = openSource()
                if (source == null) {
                    return@withContext
                }

                source.buffer().useSource { buffered ->
                    db.transaction {
                        var batch = mutableListOf<Area>()
                        for (record in btcmapJson.decodeBufferedSourceToSequence<BundledAreaJson>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toArea())
                            if (batch.size >= BATCH_SIZE) {
                                db.area.insert(batch)
                                areasImported += batch.size
                                batch = mutableListOf()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            db.area.insert(batch)
                            areasImported += batch.size
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
            return ImportResult(areasImported = 0, duration = startedAt.elapsedNow())
        }

        return ImportResult(areasImported = areasImported, duration = startedAt.elapsedNow())
    }
}

/** One area record as it appears in the bundled snapshot. */
@Serializable
internal class BundledAreaJson(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
    @SerialName("url_alias") val urlAlias: String? = null,
    val icon: String? = null,
    @SerialName("icon_wide") val iconWide: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    val description: String? = null,
    @SerialName("localized_name") val localizedName: JsonElement? = null,
    @SerialName("localized_description") val localizedDescription: JsonElement? = null,
    val bbox: List<Double>? = null,
    @SerialName("geo_json") val geoJson: JsonElement? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
)

internal fun BundledAreaJson.toArea(): Area {
    // Required fields must be present: defaulting them would silently seed a
    // bogus area if the snapshot format ever changes, instead of failing loudly
    // and rolling the import back. The id is resolved first so the messages for
    // the remaining fields can name the area.
    val areaId = requireNotNull(id) { "bundled area is missing 'id'" }
    val areaName = requireNotNull(name) { "bundled area $areaId is missing 'name'" }
    val areaType = requireNotNull(type) { "bundled area $areaId is missing 'type'" }
    val areaUrlAlias = requireNotNull(urlAlias) { "bundled area $areaId is missing 'url_alias'" }
    val areaWebsiteUrl = requireNotNull(websiteUrl) {
        "bundled area $areaId is missing 'website_url'"
    }
    // `updated_at` drives the delta sync cursor, so a malformed one must fail
    // the seed rather than silently reset the cursor to an arbitrary value.
    val areaUpdatedAt = requireNotNull(updatedAt?.toInstantOrNull()) {
        "bundled area $areaId is missing a parseable 'updated_at'"
    }
    // A bbox that is not exactly four numbers is treated as absent: the app
    // stores the four corners separately, and a partial box would seed a broken
    // point-in-area pre-filter.
    val areaBbox = bbox?.takeIf { it.size == 4 }
    return Area(
        id = areaId,
        name = areaName,
        type = areaType,
        urlAlias = areaUrlAlias,
        icon = icon,
        iconWide = iconWide,
        websiteUrl = areaWebsiteUrl,
        description = description,
        bboxWest = areaBbox?.get(0),
        bboxSouth = areaBbox?.get(1),
        bboxEast = areaBbox?.get(2),
        bboxNorth = areaBbox?.get(3),
        geoJson = (geoJson as? JsonObject)?.toString(),
        updatedAt = areaUpdatedAt,
        deletedAt = null,
        localizedName = localizedName as? JsonObject,
        localizedDescription = localizedDescription as? JsonObject,
        // A malformed date is dropped rather than failing the whole seed:
        // unlike `updated_at` it drives no cursor, so an area simply reads as
        // unverified if its date cannot be parsed.
        verifiedAt = verifiedAt?.toIsoDateOrNull(),
    )
}

/** The canonical ISO-8601 date for a bundled `verified_at`, or null if malformed. */
private fun String.toIsoDateOrNull(): String? =
    runCatching { LocalDate.parse(this).toString() }.getOrNull()
