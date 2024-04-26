package event

import api.Api
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EventsRepo(
    private val api: Api,
    private val queries: EventQueries,
) {

    suspend fun selectAll(limit: Long) = queries.selectAll(limit)

    suspend fun selectByUserIdAsListItems(userId: Long) = queries.selectByUserId(userId)

    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            val newEvents = mutableListOf<Event>()
            val updatedEvents = mutableListOf<Event>()
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
                        updatedEvents += it
                    }
                }

                queries.insertOrReplace(events)

                if (events.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                newEvents = newEvents,
                updatedEvents = updatedEvents,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newEvents: List<Event>,
        val updatedEvents: List<Event>,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}