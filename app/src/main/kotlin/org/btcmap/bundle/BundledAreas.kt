package org.btcmap.bundle

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.util.rethrowIfCancellation
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Duration
import java.time.ZonedDateTime

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
 */
object BundledAreas {
    private const val FILE_NAME = "bundled-areas.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val areasImported: Long,
        val duration: Duration,
    )

    suspend fun import(ctx: Context, db: Database): ImportResult =
        importFrom(db) { ctx.assets.open(FILE_NAME) }

    /**
     * Seeds [db] from the snapshot produced by [openStream], unless it already
     * holds areas.
     *
     * Kept separate from [import] so the seeding logic — the one-shot guard, the
     * transactional import and the failure handling — is testable without an
     * Android [Context] or a real asset.
     */
    internal suspend fun importFrom(
        db: Database,
        openStream: () -> InputStream,
    ): ImportResult {
        val startedAt = System.nanoTime()

        // Seeding is intentionally a one-shot, fresh-install operation: any area
        // already stored means the snapshot was imported or live data was
        // synced. Re-importing a newer asset later is deliberately avoided so a
        // stale bundle can never overwrite rows that sync has since refreshed.
        var areasImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            val areasInDb = withContext(Dispatchers.IO) { db.area.selectCount() }
            if (areasInDb > 0) {
                return ImportResult(areasImported = 0, duration = elapsedSince(startedAt))
            }

            // The whole parse runs inside one transaction so a malformed asset
            // rolls back to an empty table and is retried on the next launch,
            // instead of leaving a partial seed that the count check above would
            // then treat as complete.
            withContext(Dispatchers.IO) {
                openStream().use { stream ->
                    stream.bufferedReader().use { reader ->
                        val jsonReader = JsonReader(reader)
                        db.transaction {
                            jsonReader.beginArray()
                            var batch = mutableListOf<Area>()
                            while (jsonReader.hasNext()) {
                                batch.add(jsonReader.readBundledArea())
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
                            jsonReader.endArray()
                        }
                    }
                }
            }
        } catch (_: FileNotFoundException) {
            // The snapshot asset is optional; a missing file is not an error.
            return ImportResult(areasImported = 0, duration = elapsedSince(startedAt))
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
            return ImportResult(areasImported = 0, duration = elapsedSince(startedAt))
        }

        return ImportResult(areasImported = areasImported, duration = elapsedSince(startedAt))
    }

    private fun elapsedSince(startedAtNanos: Long): Duration =
        Duration.ofNanos(System.nanoTime() - startedAtNanos)
}

internal fun JsonReader.readBundledArea(): Area {
    var id: Long? = null
    var name: String? = null
    var type: String? = null
    var urlAlias: String? = null
    var icon: String? = null
    var iconWide: String? = null
    var websiteUrl: String? = null
    var description: String? = null
    var bbox: List<Double>? = null
    var geoJson: JsonObject? = null
    var updatedAt: ZonedDateTime? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "id" -> id = nextLong()
            "name" -> name = nextStringOrNull()
            "type" -> type = nextStringOrNull()
            "url_alias" -> urlAlias = nextStringOrNull()
            "icon" -> icon = nextStringOrNull()
            "icon_wide" -> iconWide = nextStringOrNull()
            "website_url" -> websiteUrl = nextStringOrNull()
            "description" -> description = nextStringOrNull()
            "bbox" -> bbox = nextDoubleListOrNull()
            "geo_json" -> geoJson = nextJsonObjectOrNull()
            "updated_at" -> updatedAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            else -> skipValue()
        }
    }
    endObject()
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
    val areaUpdatedAt = requireNotNull(updatedAt) {
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
        geoJson = geoJson?.toString(),
        updatedAt = areaUpdatedAt,
        deletedAt = null,
    )
}
