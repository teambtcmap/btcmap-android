package org.btcmap.bundle

import android.content.Context
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.util.rethrowIfCancellation
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Seeds the event table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries each event's real `updated_at`, so the first sync only
 * fetches the few that changed since the snapshot was generated, and events are
 * searchable offline. Events are time-sensitive, but a past event is simply
 * filtered out by `starts_at` at display time, and the delta keeps the rest
 * fresh.
 */
object BundledEvents {
    internal const val FILE_NAME = "bundled-events.json"

    internal const val BATCH_SIZE = 1_000

    data class ImportResult(
        val eventsImported: Long,
        val duration: Duration,
    )

    suspend fun import(ctx: Context, db: Database): ImportResult =
        importFrom(db) { ctx.assets.open(FILE_NAME) }

    /**
     * Seeds [db] from the snapshot produced by [openStream], unless it already
     * holds events.
     *
     * Kept separate from [import] so the seeding logic — the one-shot guard, the
     * transactional import and the failure handling — is testable without an
     * Android [Context] or a real asset.
     */
    internal suspend fun importFrom(
        db: Database,
        openStream: () -> InputStream,
    ): ImportResult {
        val startedAt = System.nanoTime()

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
                return ImportResult(eventsImported = 0, duration = elapsedSince(startedAt))
            }

            // The whole parse runs inside one transaction so a malformed asset
            // rolls back to an empty table and is retried on the next launch,
            // instead of leaving a partial seed that the count check above would
            // then treat as complete.
            withContext(Dispatchers.IO) {
                openStream().use { stream ->
                    stream.bufferedReader().use { reader ->
                        val jsonReader = JsonReader(reader)
                        db.transaction {
                            jsonReader.beginArray()
                            var batch = mutableListOf<Event>()
                            while (jsonReader.hasNext()) {
                                batch.add(jsonReader.readBundledEvent())
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
                            jsonReader.endArray()
                        }
                    }
                }
            }
        } catch (_: FileNotFoundException) {
            // The snapshot asset is optional; a missing file is not an error.
            return ImportResult(eventsImported = 0, duration = elapsedSince(startedAt))
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
            return ImportResult(eventsImported = 0, duration = elapsedSince(startedAt))
        }

        return ImportResult(eventsImported = eventsImported, duration = elapsedSince(startedAt))
    }

    private fun elapsedSince(startedAtNanos: Long): Duration =
        Duration.ofNanos(System.nanoTime() - startedAtNanos)
}

internal fun JsonReader.readBundledEvent(): Event {
    var id: Long? = null
    var areaId: Long? = null
    var lat: Double? = null
    var lon: Double? = null
    var name: String? = null
    var website: HttpUrl? = null
    var startsAt: ZonedDateTime? = null
    var endsAt: ZonedDateTime? = null
    var updatedAt: ZonedDateTime? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "id" -> id = nextLong()
            "area_id" -> areaId = nextLongOrNull()
            "lat" -> lat = nextDouble()
            "lon" -> lon = nextDouble()
            "name" -> name = nextStringOrNull()
            "website" -> website = nextStringOrNull()?.toHttpUrlOrNull()
            "starts_at" -> startsAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            "ends_at" -> endsAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            "updated_at" -> updatedAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            else -> skipValue()
        }
    }
    endObject()
    // Required fields must be present: defaulting them would silently seed a
    // bogus event if the snapshot format ever changes, instead of failing
    // loudly and rolling the import back. The id is resolved first so the
    // messages for the remaining fields can name the event.
    val eventId = requireNotNull(id) { "bundled event is missing 'id'" }
    val eventLat = requireNotNull(lat) { "bundled event $eventId is missing 'lat'" }
    val eventLon = requireNotNull(lon) { "bundled event $eventId is missing 'lon'" }
    val eventName = requireNotNull(name) { "bundled event $eventId is missing 'name'" }
    val eventStartsAt = requireNotNull(startsAt) {
        "bundled event $eventId is missing a parseable 'starts_at'"
    }
    // `updated_at` drives the delta sync cursor, so a malformed one must fail
    // the seed rather than silently reset the cursor to an arbitrary value.
    val eventUpdatedAt = requireNotNull(updatedAt) {
        "bundled event $eventId is missing a parseable 'updated_at'"
    }
    require(eventName.isNotEmpty()) { "bundled event $eventId has an empty 'name'" }
    return Event(
        id = eventId,
        areaId = areaId,
        lat = eventLat,
        lon = eventLon,
        name = eventName,
        website = website,
        startsAt = eventStartsAt,
        endsAt = endsAt,
        updatedAt = eventUpdatedAt,
        deletedAt = null,
    )
}
