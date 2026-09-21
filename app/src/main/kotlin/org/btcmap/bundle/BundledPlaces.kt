package org.btcmap.bundle

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.api.toVerifiedAt
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.util.rethrowIfCancellation
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Seeds the places table from the bundled snapshot produced by the bundler.
 *
 * The snapshot carries the full field set the app syncs, including each place's
 * real `updated_at`, so a seeded row is a complete record rather than a
 * placeholder. The first sync therefore only fetches the changes made since the
 * snapshot was generated: `syncPlaces` pages from the newest stored
 * `updated_at`, which the seed already provides, and asks the server for the
 * delta. The map stays fully usable offline, or while the server is
 * unreachable and the delta cannot be fetched, because every field a place
 * screen reads is already present.
 *
 * Seeded rows are stored with `bundled = false` for the same reason: the
 * "more details will appear after full sync" state and the actions it disables
 * exist only for the partial rows the old minimal snapshot used to seed, and
 * those no longer exist. An unchanged place is never fetched again, so a flag
 * that stayed set until a live sync would otherwise leave every place
 * permanently read-only.
 */
object BundledPlaces {
    internal const val FILE_NAME = "bundled-places.json"

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
        // synced. Re-importing a newer asset later is deliberately avoided so a
        // stale bundle can never overwrite rows that sync has since refreshed.
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
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync.
            e.rethrowIfCancellation()
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
    var localizedName: JsonObject? = null
    var updatedAt: ZonedDateTime? = null
    var verifiedAt: ZonedDateTime? = null
    var address: String? = null
    var openingHours: String? = null
    var localizedOpeningHours: JsonObject? = null
    var phone: String? = null
    var website: HttpUrl? = null
    var email: String? = null
    var twitter: HttpUrl? = null
    var facebook: HttpUrl? = null
    var instagram: HttpUrl? = null
    var line: HttpUrl? = null
    var requiredAppUrl: HttpUrl? = null
    var boostedUntil: ZonedDateTime? = null
    var comments: Long? = null
    var telegram: HttpUrl? = null
    var osmId: String? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            "id" -> id = nextLong()
            "lat" -> lat = nextDouble()
            "lon" -> lon = nextDouble()
            "icon" -> icon = nextString()
            "name" -> name = nextStringOrNull()
            "localized_name" -> localizedName = nextJsonObjectOrNull()
            "updated_at" -> updatedAt = nextStringOrNull()?.toZonedDateTimeOrNull()
            "verified_at" -> verifiedAt = nextStringOrNull()?.toVerifiedAtOrNull()
            "address" -> address = nextStringOrNull()
            "opening_hours" -> openingHours = nextStringOrNull()
            "localized_opening_hours" -> localizedOpeningHours = nextJsonObjectOrNull()
            "phone" -> phone = nextStringOrNull()
            "website" -> website = nextStringOrNull()?.toHttpUrlOrNull()
            "email" -> email = nextStringOrNull()
            "twitter" -> twitter = nextStringOrNull()?.toHttpUrlOrNull()
            "facebook" -> facebook = nextStringOrNull()?.toHttpUrlOrNull()
            "instagram" -> instagram = nextStringOrNull()?.toHttpUrlOrNull()
            "line" -> line = nextStringOrNull()?.toHttpUrlOrNull()
            "required_app_url" -> requiredAppUrl = nextStringOrNull()?.toHttpUrlOrNull()
            "boosted_until" -> boostedUntil = nextStringOrNull()?.toZonedDateTimeOrNull()
            "comments" -> comments = nextLongOrNull()
            "telegram" -> telegram = nextStringOrNull()?.toHttpUrlOrNull()
            "osm_id" -> osmId = nextStringOrNull()
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
    val placeLat = requireNotNull(lat) { "bundled place $placeId is missing 'lat'" }
    val placeLon = requireNotNull(lon) { "bundled place $placeId is missing 'lon'" }
    val placeIcon = requireNotNull(icon) { "bundled place $placeId is missing 'icon'" }
    // `updated_at` drives the delta sync cursor, so a malformed one must fail
    // the seed rather than silently reset the cursor to an arbitrary value.
    val placeUpdatedAt = requireNotNull(updatedAt) {
        "bundled place $placeId is missing a parseable 'updated_at'"
    }
    // Coordinates are range-checked here as well as by the bundler: the asset is
    // committed to the repository and could be edited directly, and a bogus
    // coordinate would otherwise seed a marker that can never be reached. NaN is
    // rejected too, because it fails the range check.
    require(placeLat in -90.0..90.0) { "bundled place $placeId has 'lat' outside [-90, 90]" }
    require(placeLon in -180.0..180.0) { "bundled place $placeId has 'lon' outside [-180, 180]" }
    require(placeIcon.isNotEmpty()) { "bundled place $placeId has an empty 'icon'" }
    return Place(
        id = placeId,
        bundled = false,
        updatedAt = placeUpdatedAt,
        lat = placeLat,
        lon = placeLon,
        icon = placeIcon,
        name = name,
        localizedName = localizedName,
        verifiedAt = verifiedAt,
        address = address,
        openingHours = openingHours,
        localizedOpeningHours = localizedOpeningHours,
        phone = phone,
        website = website,
        email = email,
        twitter = twitter,
        facebook = facebook,
        instagram = instagram,
        line = line,
        requiredAppUrl = requiredAppUrl,
        boostedUntil = boostedUntil,
        comments = comments,
        telegram = telegram,
        osmId = osmId,
    )
}

private fun String.toVerifiedAtOrNull(): ZonedDateTime? =
    try {
        toVerifiedAt()
    } catch (_: DateTimeParseException) {
        null
    }
