@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.btcmap.bundle

import org.btcmap.util.useSource
import org.btcmap.platform.ioDispatcher
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.Source
import okio.buffer
import org.btcmap.api.toArea
import org.btcmap.api.toGetAreasDeltaItem
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation

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
                        for (record in btcmapJson.decodeBufferedSourceToSequence<JsonObject>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toGetAreasDeltaItem().toArea())
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

