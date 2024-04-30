package event

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EventsRepo(
    private val api: Api,
    private val queries: EventQueries,
    private val context: Context,
) {

    suspend fun selectAll(limit: Long) = queries.selectAll(limit)

    suspend fun selectByUserIdAsListItems(userId: Long) = queries.selectByUserId(userId)

    suspend fun selectCount() = queries.selectCount()

    suspend fun hasBundledEvents(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("events.json")
        }
    }

    suspend fun fetchBundledEvents() {
        withContext(Dispatchers.IO) {
            context.assets.open("events.json").use { bundledEvents ->
                val events = bundledEvents.toEventsJson().map { it.toEvent() }
                queries.insertOrReplace(events)
            }
        }
    }

    suspend fun sync(): SyncReport {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        val newEvents = mutableListOf<Event>()
        var updatedEvents = 0L
        val maxUpdatedAtBeforeSync = queries.selectMaxUpdatedAt()

        while (true) {
            val events =
                api.getEvents(queries.selectMaxUpdatedAt(), BATCH_SIZE).map { it.toEvent() }

            events.forEach {
                if (maxUpdatedAtBeforeSync == null
                    || it.createdAt.isAfter(maxUpdatedAtBeforeSync)
                ) {
                    newEvents += it
                } else {
                    updatedEvents += 1
                }
            }

            queries.insertOrReplace(events)

            if (events.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            newEvents = newEvents,
            updatedEvents = updatedEvents,
        )
    }

    data class SyncReport(
        val duration: Duration,
        val newEvents: List<Event>,
        val updatedEvents: Long,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}