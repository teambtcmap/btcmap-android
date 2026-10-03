@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.btcmap.bundle

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.Source
import okio.buffer
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toZonedDateTimeOrNull

/**
 * Seeds the event table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries each event's real `updated_at`, so the first sync only
 * fetches the few that changed since the snapshot was generated, and events are
 * searchable offline. It contains only events that had not started when it was
 * generated, matching what the screens display; the delta keeps the rest fresh.
 *
 * The snapshot is decoded record by record from an [okio.Source], so it is
 * never buffered whole in memory.
 */
object BundledEvents {
    const val FILE_NAME = "bundled-events.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val eventsImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds [db] from the snapshot produced by [openSource], unless it already
     * holds events. A null [openSource] result means the optional asset is
     * absent, which is not an error.
     */
    suspend fun import(
        db: Database,
        openSource: () -> Source?,
    ): ImportResult {
        val startedAt = TimeSource.Monotonic.markNow()

        // Seeding is intentionally a one-shot, fresh-install operation: any row
        // already stored means the snapshot was imported or live data was
        // synced. The count includes tombstones, so a table that holds only
        // deleted events is not re-seeded into resurrecting them. Re-importing
        // a newer asset later is deliberately avoided so a stale bundle can
        // never overwrite rows that sync has since refreshed.
        var eventsImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            val eventsInDb = withContext(Dispatchers.IO) {
                db.event.selectCount(includeDeleted = true)
            }
            if (eventsInDb > 0) {
                return ImportResult(eventsImported = 0, duration = startedAt.elapsedNow())
            }

            // The whole parse runs inside one transaction so a malformed asset
            // rolls back to an empty table and is retried on the next launch,
            // instead of leaving a partial seed that the count check above would
            // then treat as complete.
            withContext(Dispatchers.IO) {
                val source = openSource()
                if (source == null) {
                    return@withContext
                }

                source.buffer().use { buffered ->
                    db.transaction {
                        var batch = mutableListOf<Event>()
                        for (record in btcmapJson.decodeBufferedSourceToSequence<BundledEventJson>(
                            buffered,
                            DecodeSequenceMode.ARRAY_WRAPPED,
                        )) {
                            batch.add(record.toEvent())
                            if (batch.size >= BATCH_SIZE) {
                                db.event.insert(batch)
                                eventsImported += batch.size
                                batch = mutableListOf()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            db.event.insert(batch)
                            eventsImported += batch.size
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
            return ImportResult(eventsImported = 0, duration = startedAt.elapsedNow())
        }

        return ImportResult(eventsImported = eventsImported, duration = startedAt.elapsedNow())
    }
}

/** One event record as it appears in the bundled snapshot. */
@Serializable
internal class BundledEventJson(
    val id: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val name: String? = null,
    val website: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

internal fun BundledEventJson.toEvent(): Event {
    // Required fields must be present: defaulting them would silently seed a
    // bogus event if the snapshot format ever changes, instead of failing
    // loudly and rolling the import back. The id is resolved first so the
    // messages for the remaining fields can name the event.
    val eventId = requireNotNull(id) { "bundled event is missing 'id'" }
    val eventLat = requireNotNull(lat) { "bundled event $eventId is missing 'lat'" }
    val eventLon = requireNotNull(lon) { "bundled event $eventId is missing 'lon'" }
    val eventName = requireNotNull(name) { "bundled event $eventId is missing 'name'" }
    val eventStartsAt = requireNotNull(startsAt?.toZonedDateTimeOrNull()) {
        "bundled event $eventId is missing a parseable 'starts_at'"
    }
    // `updated_at` drives the delta sync cursor, so a malformed one must fail
    // the seed rather than silently reset the cursor to an arbitrary value.
    val eventUpdatedAt = requireNotNull(updatedAt?.toZonedDateTimeOrNull()) {
        "bundled event $eventId is missing a parseable 'updated_at'"
    }
    require(eventName.isNotEmpty()) { "bundled event $eventId has an empty 'name'" }
    return Event(
        id = eventId,
        lat = eventLat,
        lon = eventLon,
        name = eventName,
        website = website?.toHttpUrlOrNull(),
        startsAt = eventStartsAt,
        endsAt = endsAt?.toZonedDateTimeOrNull(),
        updatedAt = eventUpdatedAt,
        deletedAt = null,
    )
}
