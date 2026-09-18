package org.btcmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
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

class Sync(val api: Api, val db: Database) {
    data class PlacesSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun syncPlaces(): PlacesSyncReport {
        val baseBatchSize = 10_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.place.selectMaxUpdatedAt()
            var batchSize = baseBatchSize
            while (true) {
                val delta = try {
                    api.getPlaces(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    // A failed delta must not crash the caller; leave the cursor
                    // where it is so the next sync retries the same page.
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                if (delta.isEmpty()) {
                    break
                }

                val cursor = nextUpdatedAtCursor(delta.map { it.updatedAt }, batchSize)
                if (cursor == null) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = cursor
                val reachedTip = delta.size < batchSize

                try {
                    // Guard the whole apply step, not just the request: a
                    // malformed row or a database failure here must not escape
                    // and take down the lifecycle coroutine that called sync.
                    db.transaction {
                        // Tombstones are kept as well: a deleted place still
                        // carries its last-known data, which stays available
                        // offline, and its updated_at keeps the cursor moving.
                        db.place.insert(delta.map { it.toPlace() })
                    }
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                rowsAffected += delta.size

                if (reachedTip) {
                    break
                }
                batchSize = baseBatchSize
            }
            PlacesSyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
            )
        }
    }

    data class CommentSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
        /**
         * True when the sync could not read the delta or apply it, for example
         * on a network or database failure. [rowsAffected] alone cannot tell
         * that apart from a successful sync that changed nothing.
         */
        val failed: Boolean,
    )

    suspend fun syncComments(): CommentSyncReport {
        val baseBatchSize = 1_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var failed = false
            var maxKnownUpdatedAt = db.comment.selectMaxUpdatedAt()
            var batchSize = baseBatchSize

            while (true) {
                val delta = try {
                    api.getComments(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    // A failed delta must not crash the caller; leave the cursor
                    // where it is so the next sync retries the same page.
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    failed = true
                    break
                }

                if (delta.isEmpty()) {
                    break
                }

                val cursor = nextUpdatedAtCursor(delta.map { it.updatedAt }, batchSize)
                if (cursor == null) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = cursor
                val reachedTip = delta.size < batchSize

                try {
                    // Guard the whole apply step, not just the request: a
                    // malformed timestamp or a database failure here must not
                    // escape and take down the lifecycle coroutine that called
                    // sync.
                    db.transaction {
                        // Deleted comments are kept as tombstones too, so a
                        // hidden comment that is published later replaces the
                        // tombstone in place.
                        db.comment.insert(delta.map {
                            Comment(
                                id = it.id,
                                placeId = it.placeId,
                                comment = it.comment,
                                createdAt = ZonedDateTime.parse(it.createdAt),
                                updatedAt = ZonedDateTime.parse(it.updatedAt),
                                deletedAt = it.deletedAt?.toZonedDateTime(),
                            )
                        })
                    }
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    failed = true
                    break
                }

                rowsAffected += delta.size

                if (reachedTip) {
                    break
                }
                batchSize = baseBatchSize
            }

            CommentSyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
                failed = failed,
            )
        }
    }

    data class EventSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun syncEvents(): EventSyncReport {
        val baseBatchSize = 1_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt =
                db.event.selectMaxUpdatedAt() ?: ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
            var batchSize = baseBatchSize

            while (true) {
                val delta = try {
                    api.getEvents(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    // A failed delta must not crash the caller; leave the cursor
                    // where it is so the next sync retries the same page.
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                if (delta.isEmpty()) {
                    break
                }

                val cursor = nextUpdatedAtCursor(delta.map { it.updatedAt }, batchSize)
                if (cursor == null) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = cursor
                val reachedTip = delta.size < batchSize

                try {
                    // Guard the whole apply step, not just the request: a
                    // malformed row or a database failure here must not escape
                    // and take down the lifecycle coroutine that called sync.
                    db.transaction {
                        // Event tombstones are kept like the other tables'.
                        db.event.insert(delta.map {
                            Event(
                                id = it.id,
                                areaId = it.areaId,
                                lat = it.lat,
                                lon = it.lon,
                                name = it.name,
                                website = it.website,
                                startsAt = it.startsAt,
                                endsAt = it.endsAt,
                                updatedAt = ZonedDateTime.parse(it.updatedAt),
                                deletedAt = it.deletedAt?.toZonedDateTime(),
                            )
                        })
                    }
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                rowsAffected += delta.size

                if (reachedTip) {
                    break
                }
                batchSize = baseBatchSize
            }

            EventSyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
            )
        }
    }

    data class AreaSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun syncAreas(): AreaSyncReport {
        val baseBatchSize = 1_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt =
                db.area.selectMaxUpdatedAt() ?: ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
            var batchSize = baseBatchSize

            while (true) {
                val delta = try {
                    api.getAreas(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    // A failed delta must not crash the caller; leave the cursor
                    // where it is so the next sync retries the same page.
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                if (delta.isEmpty()) {
                    break
                }

                val cursor = nextUpdatedAtCursor(delta.map { it.updatedAt }, batchSize)
                if (cursor == null) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = cursor
                val reachedTip = delta.size < batchSize

                try {
                    // Guard the whole apply step, not just the request: a
                    // malformed row or a database failure here must not escape
                    // and take down the lifecycle coroutine that called sync.
                    db.transaction {
                        // Area tombstones are kept like the other tables'. Raw
                        // tags are never synced; bbox and the full geo_json
                        // polygon are the geometry the server exposes.
                        db.area.insert(delta.map {
                            Area(
                                id = it.id,
                                name = it.name,
                                type = it.type,
                                urlAlias = it.urlAlias,
                                icon = it.icon,
                                iconWide = it.iconWide,
                                websiteUrl = it.websiteUrl,
                                description = it.description,
                                bboxWest = it.bboxWest,
                                bboxSouth = it.bboxSouth,
                                bboxEast = it.bboxEast,
                                bboxNorth = it.bboxNorth,
                                geoJson = it.geoJson,
                                updatedAt = ZonedDateTime.parse(it.updatedAt),
                                deletedAt = it.deletedAt?.toZonedDateTime(),
                            )
                        })
                    }
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    break
                }

                rowsAffected += delta.size

                if (reachedTip) {
                    break
                }
                batchSize = baseBatchSize
            }

            AreaSyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
            )
        }
    }
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
