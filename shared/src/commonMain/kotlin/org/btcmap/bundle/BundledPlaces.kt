@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.btcmap.bundle

import org.btcmap.util.useSource
import org.btcmap.platform.ioDispatcher
import androidx.sqlite.execSQL
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.Source
import okio.buffer
import org.btcmap.api.toGetPlacesItem
import org.btcmap.api.toPlace
import org.btcmap.db.Database
import org.btcmap.db.table.place.CREATE_INDEXES
import org.btcmap.db.table.place.INDEX_NAMES
import org.btcmap.db.table.place.Place
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation

/**
 * Seeds the places table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries the full field set the app syncs, including each place's
 * real `updated_at`, so a seeded row is a complete record rather than a
 * placeholder. The first sync therefore only fetches the changes made since the
 * snapshot was generated: `syncPlaces` pages from the newest stored
 * `updated_at`, which the seed already provides, and asks the server for the
 * delta. The map stays fully usable offline, or while the server is
 * unreachable and the delta cannot be fetched, because every field a place
 * screen reads is already present.
 *
 * The snapshot is decoded record by record from an [okio.Source], so it is
 * never buffered whole in memory.
 */
object BundledPlaces {
    const val FILE_NAME = "bundled-places.json"

    /**
     * Rows inserted per commit. Small enough that the map can query and draw the
     * places committed so far while the rest of the snapshot is still parsing.
     */
    internal const val BATCH_SIZE = 5_000

    /**
     * Where the seed records its progress in the preference table. A marker is
     * needed because the import commits in batches: a table with some rows is no
     * longer proof that the snapshot was imported in full.
     */
    internal const val SEED_STATE_KEY = "place_seed_state"

    internal const val SEED_IN_PROGRESS = "in_progress"
    internal const val SEED_COMPLETE = "complete"

    data class ImportResult(
        val placesImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds the places table from the snapshot produced by [openSource], unless
     * it has already been seeded, reporting the running number of imported
     * places through [onBatch] after each committed batch.
     *
     * [openSource] returns null when the snapshot asset is absent, which is not
     * an error: the snapshot is an optional offline fallback.
     *
     * The import commits in batches so the caller can surface the rows that have
     * landed instead of waiting for the whole snapshot, and it records progress
     * in the preference table so an import interrupted by a crash is retried
     * rather than mistaken for a finished one.
     */
    suspend fun import(
        db: Database,
        onBatch: (Long) -> Unit = {},
        openSource: () -> Source?,
    ): ImportResult {
        val startedAt = TimeSource.Monotonic.markNow()

        // Seeding is intentionally a one-shot, fresh-install operation: once it
        // has run, a stale bundle must never overwrite rows that sync has since
        // refreshed. Re-importing a newer asset later is therefore avoided.
        var placesImported = 0L

        // Whether this call got as far as opening the snapshot. A failure before
        // that must not discard rows an earlier run left behind; only a seed
        // this call started (and so marked in progress) is safe to remove.
        var seedingStarted = false
        try {
            if (withContext(ioDispatcher) { alreadySeeded(db) }) {
                return ImportResult(placesImported = 0, duration = startedAt.elapsedNow())
            }

            withContext(ioDispatcher) {
                seedingStarted = true
                db.preference.upsert(SEED_STATE_KEY, SEED_IN_PROGRESS)

                val source = openSource()
                if (source == null) {
                    discardPartialSeed(db)
                    return@withContext
                }

                source.buffer().useSource { buffered ->
                    // The indexes are rebuilt once on the finished table:
                    // maintaining the updated_at expression index and the
                    // bounds index for every seeded row costs more than one
                    // pass over the table. Until they exist the map's viewport
                    // read scans the partially filled table, which is cheap at
                    // this size and lets it draw each batch.
                    dropPlaceIndexes(db)
                    try {
                        var batch = mutableListOf<Place>()
                        for (record in btcmapJson.decodeBufferedSourceToSequence<JsonObject>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toGetPlacesItem().toPlace())
                            if (batch.size >= BATCH_SIZE) {
                                placesImported += insertBatch(db, batch)
                                onBatch(placesImported)
                                batch = mutableListOf()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            placesImported += insertBatch(db, batch)
                            onBatch(placesImported)
                        }
                    } finally {
                        createPlaceIndexes(db)
                    }
                }

                if (placesImported > 0) {
                    db.preference.upsert(SEED_STATE_KEY, SEED_COMPLETE)
                } else {
                    // An empty snapshot seeds nothing; leave no marker so a
                    // later snapshot can still seed.
                    db.preference.delete(SEED_STATE_KEY)
                }
            }
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync. The batches already
            // committed are discarded, so the next launch retries from an empty
            // table instead of treating a partial seed as done.
            e.rethrowIfCancellation()
            if (seedingStarted) discardPartialSeed(db)
            return ImportResult(placesImported = 0, duration = startedAt.elapsedNow())
        }

        return ImportResult(placesImported = placesImported, duration = startedAt.elapsedNow())
    }

    /**
     * Whether the snapshot has already run.
     *
     * A database that predates the progress marker still counts as seeded when
     * it holds any place — tombstone included — so upgrading does not re-import
     * over rows that sync has since refreshed. Counting tombstones also stops a
     * table holding only deleted places from resurrecting them.
     */
    private suspend fun alreadySeeded(db: Database): Boolean {
        when (db.preference.select(SEED_STATE_KEY)) {
            SEED_COMPLETE -> return true
            SEED_IN_PROGRESS -> {
                // A previous import was interrupted before it finished; its rows
                // are a prefix of the snapshot, not a complete seed.
                db.place.deleteAll()
                return false
            }
        }

        if (db.place.selectCount(includeDeleted = true) > 0) {
            db.preference.upsert(SEED_STATE_KEY, SEED_COMPLETE)
            return true
        }
        return false
    }

    private suspend fun insertBatch(db: Database, batch: List<Place>): Long {
        db.transaction { db.place.insert(batch) }
        return batch.size.toLong()
    }

    private suspend fun dropPlaceIndexes(db: Database) {
        db.transaction { INDEX_NAMES.forEach { db.conn.execSQL("DROP INDEX IF EXISTS $it;") } }
    }

    private suspend fun createPlaceIndexes(db: Database) {
        db.transaction { CREATE_INDEXES.forEach { db.conn.execSQL(it) } }
    }

    /** Best-effort removal of a seed that did not finish, so it is retried. */
    private suspend fun discardPartialSeed(db: Database) {
        try {
            db.place.deleteAll()
        } catch (t: Exception) {
            // The seed is retried on the next launch regardless; a cancellation
            // still propagates.
            t.rethrowIfCancellation()
        }
        try {
            db.preference.delete(SEED_STATE_KEY)
        } catch (t: Exception) {
            // The marker only makes the retry cheaper.
            t.rethrowIfCancellation()
        }
    }
}

