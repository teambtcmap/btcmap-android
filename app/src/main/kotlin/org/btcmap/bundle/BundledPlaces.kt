package org.btcmap.bundle

import android.content.Context
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.util.rethrowIfCancellation
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Sentinel timestamp for seeded rows.
 *
 * The bundled snapshot intentionally carries only a minimal field set so the
 * map shows places immediately while the full data is downloaded. Every row
 * returned by the API carries a later `updated_at`, so the first delta sync
 * pulls the whole snapshot again and replaces each seeded row with the full
 * live record, progressively enriching it. The low constant is a deliberate
 * guarantee of that gradual override and eventual consistency, not an
 * accident: a seeded row can never shadow or outrank live data.
 */
private val SEEDED_UPDATED_AT = ZonedDateTime.parse("2000-01-01T00:00:00Z")

object BundledPlaces {
    private const val FILE_NAME = "bundled-places.json"

    internal const val BATCH_SIZE = 10_000

    data class ImportResult(
        val placesImported: Long,
        val duration: Duration,
    )

    suspend fun import(ctx: Context, db: Database): ImportResult =
        importFrom(db) { ctx.assets.open(FILE_NAME) }

    /**
     * Seeds [db] from the snapshot produced by [openStream], unless it already
     * holds places.
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

        // Seeding is intentionally a one-shot, fresh-install operation: any
        // place already stored means the snapshot was imported or live data was
        // synced. It only needs to happen once, because the minimal snapshot is
        // meant to keep the user busy until sync replaces it: see
        // SEEDED_UPDATED_AT for why every seeded row is enriched and eventually
        // overridden by live data. Re-importing a newer asset later is
        // deliberately avoided so a stale bundle can never overwrite rows that
        // sync has since refreshed.
        var placesImported = 0L
        try {
            // The count read is inside the try as well: a database failure must
            // not escape and take down the calling screen, for the same reason a
            // missing or malformed asset must not.
            //
            // The guard is a cheap optimisation to avoid re-parsing the snapshot
            // on every resume, not a concurrency primitive: the count check and
            // the import below are not one atomic unit, so two callers racing
            // here could both observe an empty table and import twice. Only
            // MapFragment calls this, on the main lifecycle, so that is not
            // reachable today; even if it were, INSERT OR REPLACE makes a double
            // import idempotent rather than corrupting data.
            val placesInDb = withContext(Dispatchers.IO) { db.place.selectCount() }
            if (placesInDb > 0) {
                return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
            }

            // The whole parse runs inside one transaction so a malformed asset
            // rolls back to an empty table and is retried on the next launch,
            // instead of leaving a partial seed that the count check above would
            // then treat as complete. Holding the transaction for the entire
            // parse briefly blocks other writers; that is acceptable because
            // this is a fresh-install path and the snapshot is inserted once.
            withContext(Dispatchers.IO) {
                openStream().use { stream ->
                    stream.bufferedReader().use { reader ->
                        val jsonReader = JsonReader(reader)
                        db.transaction {
                            jsonReader.beginArray()
                            var batch = mutableListOf<Place>()
                            while (jsonReader.hasNext()) {
                                batch.add(jsonReader.readBundledPlace())
                                if (batch.size >= BATCH_SIZE) {
                                    db.place.insert(batch)
                                    placesImported += batch.size
                                    batch = mutableListOf()
                                }
                            }
                            if (batch.isNotEmpty()) {
                                db.place.insert(batch)
                                placesImported += batch.size
                            }
                            jsonReader.endArray()
                        }
                    }
                }
            }
        } catch (_: FileNotFoundException) {
            // The snapshot asset is optional; a missing file is not an error.
            return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
        }

        val duration = elapsedSince(startedAt)
        return ImportResult(placesImported = placesImported, duration = duration)
    }

    private fun elapsedSince(startedAtNanos: Long): Duration =
        Duration.ofNanos(System.nanoTime() - startedAtNanos)
}

internal fun JsonReader.readBundledPlace(): Place {
    var id: Long? = null
    var lat: Double? = null
    var lon: Double? = null
    var icon: String? = null
    var name: String? = null
    var comments: Long? = null
    var boostedUntil: ZonedDateTime? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "id" -> id = nextLong()
            "lat" -> lat = nextDouble()
            "lon" -> lon = nextDouble()
            "icon" -> icon = nextString()
            "name" -> name = if (peek() == JsonToken.NULL) {
                skipValue()
                null
            } else {
                nextString()
            }
            "comments" -> {
                if (peek() == JsonToken.NULL) {
                    skipValue()
                    comments = null
                } else {
                    comments = nextLong()
                }
            }
            "boosted_until" -> {
                if (peek() == JsonToken.NULL) {
                    skipValue()
                    boostedUntil = null
                } else {
                    boostedUntil = ZonedDateTime.parse(nextString())
                }
            }
            else -> skipValue()
        }
    }
    endObject()
    // Required fields must be present: defaulting them would silently seed a
    // bogus place (for example id 0 at Null Island) if the snapshot format ever
    // changes, instead of failing loudly and rolling the import back. The id is
    // resolved first so the messages for the remaining fields can name the place
    // and the missing-id message never interpolates a null id.
    val placeId = requireNotNull(id) { "bundled place is missing 'id'" }
    return Place(
        id = placeId,
        lat = requireNotNull(lat) { "bundled place $placeId is missing 'lat'" },
        lon = requireNotNull(lon) { "bundled place $placeId is missing 'lon'" },
        icon = requireNotNull(icon) { "bundled place $placeId is missing 'icon'" },
        name = name,
        localizedName = null,
        updatedAt = SEEDED_UPDATED_AT,
        requiredAppUrl = null,
        boostedUntil = boostedUntil,
        verifiedAt = null,
        address = null,
        openingHours = null,
        localizedOpeningHours = null,
        website = null,
        phone = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
        bundled = true,
        comments = comments,
        telegram = null,
        osmId = null,
    )
}
