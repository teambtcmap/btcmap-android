package sync

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.database.sqlite.transaction
import api.EventApi
import db.table.event.Event
import db.table.event.EventQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object EventSync {

    suspend fun run(db: SQLiteDatabase): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            val events = try {
                EventApi.getEvents()
            } catch (t: Throwable) {
                Log.e(null, null, t)
                return@withContext Report(
                    duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                    rowsAffected = 0,
                )
            }

            db.transaction {
                EventQueries.deleteAll(this)
                EventQueries.insert(events.map {
                    Event(
                        id = it.id,
                        lat = it.lat,
                        lon = it.lon,
                        name = it.name,
                        website = it.website,
                        startsAt = it.startsAt,
                        endsAt = it.endsAt,
                    )
                }, this)
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