package org.btcmap.sync

import android.util.Log
import org.btcmap.Api
import org.btcmap.db.table.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object EventSync {

    suspend fun run(api: Api, db: Database): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            val events = try {
                api.getEvents()
            } catch (t: Throwable) {
                Log.e(null, null, t)
                return@withContext Report(
                    duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                    rowsAffected = 0,
                )
            }

            db.transaction {
                db.event.deleteAll()
                db.event.insert(events.map {
                    Event(
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

            Report(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = events.size.toLong(),
            )
        }
    }

    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
    )
}