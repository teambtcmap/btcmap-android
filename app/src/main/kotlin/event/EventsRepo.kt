package event

import api.Api

class EventsRepo(
    private val api: Api,
    private val queries: EventQueries,
) {

    suspend fun selectAll(limit: Long) = queries.selectAll(limit)

    suspend fun selectByUserIdAsListItems(userId: Long) = queries.selectByUserId(userId)

    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()
            val createdOrUpdatedEvents = mutableListOf<Event>()
            val newEvents = mutableListOf<Event>()
            val maxUpdatedAtBeforeSync = queries.selectMaxUpdatedAt()

            while (true) {
                val events =
                    api.getEvents(queries.selectMaxUpdatedAt(), BATCH_SIZE).map { it.toEvent() }
                createdOrUpdatedEvents += events
                newEvents += events.filter {
                    maxUpdatedAtBeforeSync == null
                            || it.createdAt.isAfter(maxUpdatedAtBeforeSync)
                }
                queries.insertOrReplace(events)

                if (events.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedEvents = createdOrUpdatedEvents,
                newEvents = newEvents,
            )
        }
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedEvents: List<Event>,
        val newEvents: List<Event>,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}