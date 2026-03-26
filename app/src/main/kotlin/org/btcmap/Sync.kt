package org.btcmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.db.Database
import org.btcmap.db.table.CommentProjectionFull
import org.btcmap.db.table.EventProjectionFull
import org.btcmap.db.table.PlaceProjectionFull
import org.btcmap.time.toZonedDateTime
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(val api: Api, val db: Database) {
    data class PlacesSyncReport(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun syncPlaces(): PlacesSyncReport {
        val batchSize = 10_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.place.selectMaxUpdatedAt()
            while (true) {
                val delta = try {
                    api.getPlaces(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    return@withContext PlacesSyncReport(
                        duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                        rowsAffected = rowsAffected,
                    )
                }

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    db.place.insert(newOrChanged.map {
                        val verifiedAt = if (it.verifiedAt == null) {
                            null
                        } else {
                            it.verifiedAt + "T00:00:00Z"
                        }

                        PlaceProjectionFull(
                            id = it.id,
                            bundled = it.bundled,
                            updatedAt = it.updatedAt.toZonedDateTime(),
                            lat = it.lat,
                            lon = it.lon,
                            icon = it.icon,
                            name = it.name,
                            localizedName = it.localizedName,
                            verifiedAt = verifiedAt?.toZonedDateTime(),
                            address = it.address,
                            openingHours = it.openingHours,
                            localizedOpeningHours = it.localizedOpeningHours,
                            phone = it.phone,
                            website = it.website?.toHttpUrlOrNull(),
                            email = it.email,
                            twitter = it.twitter?.toHttpUrlOrNull(),
                            facebook = it.facebook?.toHttpUrlOrNull(),
                            instagram = it.instagram?.toHttpUrlOrNull(),
                            line = it.line?.toHttpUrlOrNull(),
                            requiredAppUrl = it.requiredAppUrl?.toHttpUrlOrNull(),
                            boostedUntil = it.boostedUntil?.toZonedDateTime(),
                            comments = it.comments,
                            telegram = it.telegram,
                        )
                    })

                    deleted.forEach { db.place.deleteById(it.id) }
                }

                rowsAffected += delta.size

                if (delta.size < batchSize) {
                    break
                }
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
        val batchSize = 1_000L

        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.comment.selectMaxUpdatedAt()

            while (true) {
                val delta = try {
                    api.getComments(maxKnownUpdatedAt, batchSize)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    return@withContext CommentSyncReport(
                        duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                        rowsAffected = rowsAffected,
                    )
                }

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    db.comment.insert(newOrChanged.map {
                        CommentProjectionFull(
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

                if (delta.size < batchSize) {
                    break
                }
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
                t.printStackTrace()
                return@withContext EventSyncReport(
                    duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                    rowsAffected = 0,
                )
            }

            db.transaction {
                db.event.deleteAll()
                db.event.insert(events.map {
                    EventProjectionFull(
                        id = it.id,
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