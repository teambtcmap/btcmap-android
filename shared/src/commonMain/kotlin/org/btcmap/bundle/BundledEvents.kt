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
import org.btcmap.api.toEvent
import org.btcmap.api.toGetEventsDeltaItem
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation

/**
 * Seeds the event table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries each event's real `updated_at`, so the first sync only
 * fetches the few that changed since the snapshot was generated, and events are
 * searchable offline. It contains only events that had not started when it was
 * generated, matching what the screens display; the delta keeps the rest fresh.
 *
 * The snapshot is decoded record by record from an [okio.Source], so it is
 * never buffered whole in memory.
 */
object BundledEvents {
    const val FILE_NAME = "bundled-events.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val eventsImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds [db] from the snapshot produced by [openSource], unless it already
     * holds events. A null [openSource] result means the optional asset is
     * absent, which is not an error.
     */
    suspend fun import(
        db: Database,
        openSource: () -> Source?,
    ): ImportResult {
        val startedAt = TimeSource.Monotonic.markNow()

        // Seeding is intentionally a one-shot, fresh-install operation: any row
        // already stored means the snapshot was imported or live data was
        // synced. The count includes tombstones, so a table that holds only
        // deleted events is not re-seeded into resurrecting them. Re-importing
        // a newer asset later is deliberately avoided so a stale bundle can
        // never overwrite rows that sync has since refreshed.
        var eventsImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            val eventsInDb = withContext(ioDispatcher) {
                db.event.selectCount(includeDeleted = true)
            }
            if (eventsInDb > 0) {
                return ImportResult(eventsImported = 0, duration = startedAt.elapsedNow())
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
                        var batch = mutableListOf<Event>()
                        for (record in btcmapJson.decodeBufferedSourceToSequence<JsonObject>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toGetEventsDeltaItem().toEvent())
                            if (batch.size >= BATCH_SIZE) {
                                db.event.insert(batch)
                                eventsImported += batch.size
                                batch = mutableListOf()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            db.event.insert(batch)
                            eventsImported += batch.size
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
            return ImportResult(eventsImported = 0, duration = startedAt.elapsedNow())
        }

        return ImportResult(eventsImported = eventsImported, duration = startedAt.elapsedNow())
    }
}

