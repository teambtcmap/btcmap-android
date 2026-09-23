package org.btcmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.GetAreasDeltaItem
import org.btcmap.api.GetCommentsItem
import org.btcmap.api.GetEventsDeltaItem
import org.btcmap.api.getAreas
import org.btcmap.api.getComments
import org.btcmap.api.getEvents
import org.btcmap.api.getPlaces
import org.btcmap.api.toPlace
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.event.Event
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toZonedDateTime
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val PLACES_BATCH_SIZE = 10_000L
private const val DEFAULT_BATCH_SIZE = 1_000L

private val EPOCH: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)

class Sync(val api: Api, val db: Database) {
    /**
     * The outcome of one incremental sync.
     *
     * [rowsAffected] alone cannot tell a failure from a successful sync that
     * changed nothing, so [failed] records whether a page could not be read or
     * applied. The sync is best-effort and offline-first: a failure is never
     * thrown at the caller, only reported.
     */
    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
        val failed: Boolean,
    )

    suspend fun syncPlaces(): Report = syncDelta(
        baseBatchSize = PLACES_BATCH_SIZE,
        cursor = db.place.selectMaxUpdatedAt(),
        fetch = { since, limit -> api.getPlaces(since, limit) },
        updatedAt = { it.updatedAt },
        apply = { rows ->
            db.transaction {
                // Tombstones are kept as well: a deleted place still carries
                // its last-known data, which stays available offline, and its
                // updated_at keeps the cursor moving.
                db.place.insert(rows.map { it.toPlace() })
            }
        },
    )

    suspend fun syncComments(): Report = syncDelta(
        baseBatchSize = DEFAULT_BATCH_SIZE,
        cursor = db.comment.selectMaxUpdatedAt(),
        fetch = { since, limit -> api.getComments(since, limit) },
        updatedAt = { it.updatedAt },
        apply = { rows ->
            db.transaction {
                // Deleted comments are kept as tombstones too, so a hidden
                // comment that is published later replaces the tombstone in
                // place.
                db.comment.insert(rows.map { it.toComment() })
            }
        },
    )

    suspend fun syncEvents(): Report = syncDelta(
        baseBatchSize = DEFAULT_BATCH_SIZE,
        cursor = db.event.selectMaxUpdatedAt() ?: EPOCH,
        fetch = { since, limit -> api.getEvents(requireNotNull(since), limit) },
        updatedAt = { it.updatedAt },
        apply = { rows ->
            db.transaction {
                // Event tombstones are kept like the other tables'.
                db.event.insert(rows.map { it.toEvent() })
            }
        },
    )

    suspend fun syncAreas(): Report = syncDelta(
        baseBatchSize = DEFAULT_BATCH_SIZE,
        cursor = db.area.selectMaxUpdatedAt() ?: EPOCH,
        fetch = { since, limit -> api.getAreas(requireNotNull(since), limit) },
        updatedAt = { it.updatedAt },
        apply = { rows ->
            db.transaction {
                // Area tombstones are kept like the other tables'. Raw tags
                // are never synced; bbox and the full geo_json polygon are the
                // geometry the server exposes.
                db.area.insert(rows.map { it.toArea() })
            }
        },
    )

    /**
     * Runs the incremental-sync state machine shared by all four tables.
     *
     * Reads pages newer than [cursor], advancing it to the newest timestamp
     * that is certainly complete and widening [baseBatchSize] when a whole page
     * shares one timestamp (see [nextUpdatedAtCursor]). A failed page read or
     * apply stops the loop and leaves the cursor where it is, so the next sync
     * retries the same page; it never throws at the caller.
     */
    private suspend fun <T> syncDelta(
        baseBatchSize: Long,
        cursor: ZonedDateTime?,
        fetch: suspend (since: ZonedDateTime?, limit: Long) -> List<T>,
        updatedAt: (T) -> String,
        apply: (List<T>) -> Unit,
    ): Report = withContext(Dispatchers.IO) {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        var rowsAffected = 0L
        var failed = false
        var maxKnownUpdatedAt = cursor
        var batchSize = baseBatchSize

        while (true) {
            val delta = try {
                fetch(maxKnownUpdatedAt, batchSize)
            } catch (t: Throwable) {
                // A failed delta must not crash the caller; leave the cursor
                // where it is so the next sync retries the same page.
                t.rethrowIfCancellation()
                reportSyncFailure(t)
                failed = true
                break
            }

            if (delta.isEmpty()) {
                break
            }

            val nextCursor = nextUpdatedAtCursor(delta.map(updatedAt), batchSize)
            if (nextCursor == null) {
                batchSize *= 2
                continue
            }

            maxKnownUpdatedAt = nextCursor
            val reachedTip = delta.size < batchSize

            try {
                // Guard the whole apply step, not just the request: a malformed
                // row or a database failure here must not escape and take down
                // the lifecycle coroutine that called sync.
                apply(delta)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                reportSyncFailure(t)
                failed = true
                break
            }

            rowsAffected += delta.size

            if (reachedTip) {
                break
            }
            batchSize = baseBatchSize
        }

        Report(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            rowsAffected = rowsAffected,
            failed = failed,
        )
    }
}

private fun GetCommentsItem.toComment(): Comment = Comment(
    id = id,
    placeId = placeId,
    comment = comment,
    createdAt = ZonedDateTime.parse(createdAt),
    updatedAt = ZonedDateTime.parse(updatedAt),
    deletedAt = deletedAt?.toZonedDateTime(),
)

private fun GetEventsDeltaItem.toEvent(): Event = Event(
    id = id,
    areaId = areaId,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
    updatedAt = ZonedDateTime.parse(updatedAt),
    deletedAt = deletedAt?.toZonedDateTime(),
)

private fun GetAreasDeltaItem.toArea(): Area = Area(
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
    updatedAt = ZonedDateTime.parse(updatedAt),
    deletedAt = deletedAt?.toZonedDateTime(),
)

/**
 * Records a non-fatal sync failure. The sync is best-effort and offline-first,
 * so a failure is never shown to the user; it is logged for debugging instead.
 * Centralised so the whole sync has a single place to route these through.
 */
internal fun reportSyncFailure(t: Throwable) {
    t.printStackTrace()
}

/**
 * Returns the new `updated_at` cursor after reading a page, or null when the
 * page is full and every row shares one timestamp.
 *
 * The server filters with `updated_at > updated_since` and orders by
 * `updated_at`, so a full page can be cut off in the middle of the newest
 * timestamp group. Advancing the cursor to that group's timestamp would skip
 * the rest of the group on the next request. Only a timestamp that is
 * certainly complete is safe: on a full page that is the second-newest
 * distinct timestamp, since re-reading the newest group is harmless (the
 * insert replaces the rows). When every row shares one timestamp there is no
 * such timestamp, and the caller must widen the request instead.
 */
internal fun nextUpdatedAtCursor(timestamps: List<String>, pageSize: Long): ZonedDateTime? {
    if (timestamps.size.toLong() < pageSize) {
        return timestamps.maxOf { ZonedDateTime.parse(it) }
    }

    val distinct = timestamps.map { ZonedDateTime.parse(it) }.distinct().sorted()
    return distinct.getOrNull(distinct.size - 2)
}
