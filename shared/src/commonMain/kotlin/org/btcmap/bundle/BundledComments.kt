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
import org.btcmap.api.toComment
import org.btcmap.api.toGetCommentsItem
import org.btcmap.db.Database
import org.btcmap.db.table.comment.Comment
import org.btcmap.json.btcmapJson
import org.btcmap.sync.reportSyncFailure
import org.btcmap.util.rethrowIfCancellation

/**
 * Seeds the comment table from the bundled snapshot produced by the bundler.
 *
 * Comments accrue slowly, so the snapshot carries each comment's real
 * `updated_at` and the first sync only fetches the few that changed since the
 * snapshot was generated. A place's comments are then readable offline, or
 * while the server is unreachable, instead of showing nothing.
 *
 * The snapshot is decoded record by record from an [okio.Source], so it is
 * never buffered whole in memory.
 */
object BundledComments {
    const val FILE_NAME = "bundled-comments.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val commentsImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds [db] from the snapshot produced by [openSource], unless it already
     * holds comments. A null [openSource] result means the optional asset is
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
        // deleted comments is not re-seeded into resurrecting them.
        // Re-importing a newer asset later is deliberately avoided so a stale
        // bundle can never overwrite rows that sync has since refreshed.
        var commentsImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            val commentsInDb = withContext(ioDispatcher) {
                db.comment.selectCount(includeDeleted = true)
            }
            if (commentsInDb > 0) {
                return ImportResult(commentsImported = 0, duration = startedAt.elapsedNow())
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
                        var batch = mutableListOf<Comment>()
                        for (record in btcmapJson.decodeBufferedSourceToSequence<JsonObject>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toGetCommentsItem().toComment())
                            if (batch.size >= BATCH_SIZE) {
                                db.comment.insert(batch)
                                commentsImported += batch.size
                                batch = mutableListOf()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            db.comment.insert(batch)
                            commentsImported += batch.size
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync. The failure is still
            // reported, so a broken database is not indistinguishable from a
            // missing or empty snapshot.
            e.rethrowIfCancellation()
            reportSyncFailure(e)
            return ImportResult(commentsImported = 0, duration = startedAt.elapsedNow())
        }

        return ImportResult(commentsImported = commentsImported, duration = startedAt.elapsedNow())
    }
}

