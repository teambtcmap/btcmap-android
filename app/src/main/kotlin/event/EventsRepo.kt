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
            var count = 0L

            while (true) {
                val events = api.getEvents(queries.selectMaxUpdatedAt(), BATCH_SIZE)
                count += events.size
                queries.insertOrReplace(events.map { it.toEvent() })

                if (events.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedEvents = count,
            )
        }
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedEvents: Long,
    )

    companion object {
        private const val BATCH_SIZE = 2500L
    }
}