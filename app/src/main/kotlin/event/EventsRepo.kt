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

    suspend fun selectAll(limit: Long): List<EventListItem> {
        return withContext(Dispatchers.IO) {
            queries.selectAll(limit)
        }
    }

    suspend fun selectByUserIdAsListItems(userId: Long): List<EventListItem> {
        return withContext(Dispatchers.IO) {
            queries.selectByUserId(userId)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledEvents(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("events.json")
        }
    }

    suspend fun fetchBundledEvents() {
        withContext(Dispatchers.IO) {
            context.assets.open("events.json").use { bundledEvents ->
                queries.insertOrReplace(
                    bundledEvents
                        .toEventsJson()
                        .filter { it.deletedAt == null }
                        .map { it.toEvent() }
                )
            }
        }
    }

    suspend fun sync(): SyncReport {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            val newItems = mutableListOf<Event>()
            var updatedItems = 0L
            var deletedItems = 0L
            var maxKnownUpdatedAt = queries.selectMaxUpdatedAt()

            while (true) {
                val delta = api.getEvents(maxKnownUpdatedAt, BATCH_SIZE)

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                delta.forEach {
                    val cached = queries.selectById(it.id)

                    if (it.deletedAt == null) {
                        if (cached == null) {
                            newItems += it.toEvent()
                        } else {
                            updatedItems++
                        }

                        queries.insertOrReplace(it.toEvent())
                    } else {
                        if (cached == null) {
                            // Already evicted from cache, nothing to do here
                        } else {
                            queries.deleteById(it.id)
                            deletedItems++
                        }
                    }
                }

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                newEvents = newItems,
                updatedEvents = updatedItems,
                deletedEvents = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newEvents: List<Event>,
        val updatedEvents: Long,
        val deletedEvents: Long,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}