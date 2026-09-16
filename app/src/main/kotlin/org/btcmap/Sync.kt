package org.btcmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.getComments
import org.btcmap.api.getEvents
import org.btcmap.api.getPlaces
import org.btcmap.api.toPlace
import org.btcmap.db.Database
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.event.Event
import org.btcmap.util.rethrowIfCancellation
import java.time.Duration
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
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    return@withContext PlacesSyncReport(
                        duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                        rowsAffected = rowsAffected,
                    )
                }

                if (delta.isEmpty()) {
                    break
                }

                val maxUpdatedAt = delta.maxBy { ZonedDateTime.parse(it.updatedAt) }.updatedAt

                // The server filters with `updated_at > updated_since` and orders
                // by `updated_at`. A full batch whose rows all share one timestamp
                // may be truncating a larger group with that same timestamp, and
                // advancing the cursor to it would silently skip the rest of the
                // group. Keep the cursor and widen the request until the batch
                // either isn't full or spans more than one timestamp.
                if (delta.size.toLong() == batchSize && delta.all { it.updatedAt == maxUpdatedAt }) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = ZonedDateTime.parse(maxUpdatedAt)
                val reachedTip = delta.size < batchSize

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    db.place.insert(newOrChanged.map { it.toPlace() })

                    deleted.forEach { db.place.deleteById(it.id) }
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
    )

    suspend fun syncComments(): CommentSyncReport {
        val baseBatchSize = 1_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.comment.selectMaxUpdatedAt()
            var batchSize = baseBatchSize

            while (true) {
                val delta = try {
                    api.getComments(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    t.printStackTrace()
                    return@withContext CommentSyncReport(
                        duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                        rowsAffected = rowsAffected,
                    )
                }

                if (delta.isEmpty()) {
                    break
                }

                val maxUpdatedAt = delta.maxBy { ZonedDateTime.parse(it.updatedAt) }.updatedAt

                // See syncPlaces for why a full single-timestamp batch must not
                // advance the cursor.
                if (delta.size.toLong() == batchSize && delta.all { it.updatedAt == maxUpdatedAt }) {
                    batchSize *= 2
                    continue
                }

                maxKnownUpdatedAt = ZonedDateTime.parse(maxUpdatedAt)
                val reachedTip = delta.size < batchSize

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    db.comment.insert(newOrChanged.map {
                        Comment(
                            id = it.id,
                            placeId = it.elementId!!,
                            comment = it.comment!!,
                            createdAt = ZonedDateTime.parse(it.createdAt!!),
                            updatedAt = ZonedDateTime.parse(it.updatedAt),
                        )
                    })

                    deleted.forEach {
                        db.comment.deleteById(it.id)
                    }
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
            )
        }
    }

    data class EventSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun syncEvents(): EventSyncReport {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            val events = try {
                api.getEvents()
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                t.printStackTrace()
                return@withContext EventSyncReport(
                    duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                    rowsAffected = 0,
                )
            }

            db.transaction {
                db.event.deleteAll()
                db.event.insert(events.map {
                    Event(
                        id = it.id,
                        areaId = it.areaId,
                        lat = it.lat,
                        lon = it.lon,
                        name = it.name,
                        website = it.website,
                        startsAt = it.startsAt,
                        endsAt = it.endsAt,
                    )
                })
            }

            EventSyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = events.size.toLong(),
            )
        }
    }
}
