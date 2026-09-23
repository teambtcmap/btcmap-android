package org.btcmap.bundle

import android.content.Context
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.comment.Comment
import org.btcmap.reportSyncFailure
import org.btcmap.util.rethrowIfCancellation
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Seeds the comment table from the bundled snapshot produced by the bundler.
 *
 * Comments accrue slowly, so the snapshot carries each comment's real
 * `updated_at` and the first sync only fetches the few that changed since the
 * snapshot was generated. A place's comments are then readable offline, or
 * while the server is unreachable, instead of showing nothing.
 */
object BundledComments {
    internal const val FILE_NAME = "bundled-comments.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val commentsImported: Long,
        val duration: Duration,
    )

    suspend fun import(ctx: Context, db: Database): ImportResult =
        importFrom(db) { ctx.assets.open(FILE_NAME) }

    /**
     * Seeds [db] from the snapshot produced by [openStream], unless it already
     * holds comments.
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
            val commentsInDb = withContext(Dispatchers.IO) {
                db.comment.selectCount(includeDeleted = true)
            }
            if (commentsInDb > 0) {
                return ImportResult(commentsImported = 0, duration = elapsedSince(startedAt))
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
                            var batch = mutableListOf<Comment>()
                            while (jsonReader.hasNext()) {
                                batch.add(jsonReader.readBundledComment())
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
                            jsonReader.endArray()
                        }
                    }
                }
            }
        } catch (_: FileNotFoundException) {
            // The snapshot asset is optional; a missing file is not an error.
            return ImportResult(commentsImported = 0, duration = elapsedSince(startedAt))
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync. The failure is still
            // reported, so a broken database is not indistinguishable from a
            // missing or empty snapshot.
            e.rethrowIfCancellation()
            reportSyncFailure(e)
            return ImportResult(commentsImported = 0, duration = elapsedSince(startedAt))
        }

        return ImportResult(commentsImported = commentsImported, duration = elapsedSince(startedAt))
    }

    private fun elapsedSince(startedAtNanos: Long): Duration =
        Duration.ofNanos(System.nanoTime() - startedAtNanos)
}

internal fun JsonReader.readBundledComment(): Comment {
    var id: Long? = null
    var placeId: Long? = null
    var text: String? = null
    var createdAt: ZonedDateTime? = null
    var updatedAt: ZonedDateTime? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "id" -> id = nextLong()
            "place_id" -> placeId = nextLong()
            "text" -> text = nextStringOrNull()
            "created_at" -> createdAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            "updated_at" -> updatedAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            else -> skipValue()
        }
    }
    endObject()
    // Required fields must be present: defaulting them would silently seed a
    // bogus comment if the snapshot format ever changes, instead of failing
    // loudly and rolling the import back. The id is resolved first so the
    // messages for the remaining fields can name the comment.
    val commentId = requireNotNull(id) { "bundled comment is missing 'id'" }
    val commentPlaceId = requireNotNull(placeId) { "bundled comment $commentId is missing 'place_id'" }
    val commentText = requireNotNull(text) { "bundled comment $commentId is missing 'text'" }
    val commentCreatedAt = requireNotNull(createdAt) {
        "bundled comment $commentId is missing a parseable 'created_at'"
    }
    // `updated_at` drives the delta sync cursor, so a malformed one must fail
    // the seed rather than silently reset the cursor to an arbitrary value.
    val commentUpdatedAt = requireNotNull(updatedAt) {
        "bundled comment $commentId is missing a parseable 'updated_at'"
    }
    require(commentText.isNotEmpty()) { "bundled comment $commentId has an empty 'text'" }
    return Comment(
        id = commentId,
        placeId = commentPlaceId,
        comment = commentText,
        createdAt = commentCreatedAt,
        updatedAt = commentUpdatedAt,
        deletedAt = null,
    )
}
