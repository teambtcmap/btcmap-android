package org.btcmap.sync

import android.util.Log
import org.btcmap.api.PlaceApi
import org.btcmap.db.table.place.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.db.Database
import org.btcmap.time.toZonedDateTime
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object PlaceSync {
    private const val BATCH_SIZE = 10_000L

    suspend fun run(db: Database): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.place.selectMaxUpdatedAt()
            while (true) {
                val delta = try {
                    PlaceApi.getPlaces(maxKnownUpdatedAt, BATCH_SIZE)
                } catch (t: Throwable) {
                    Log.e(null, null, t)
                    return@withContext Report(
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

                        Place(
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

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }
            Report(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
            )
        }
    }

    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
    )
}