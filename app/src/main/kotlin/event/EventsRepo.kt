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