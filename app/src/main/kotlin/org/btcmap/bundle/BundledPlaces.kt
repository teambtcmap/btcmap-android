package org.btcmap.bundle

import android.content.Context
import androidx.sqlite.execSQL
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.api.toVerifiedAt
import org.btcmap.db.Database
import org.btcmap.db.table.place.CREATE_INDEXES
import org.btcmap.db.table.place.INDEX_NAMES
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
 */
object BundledPlaces {
    internal const val FILE_NAME = "bundled-places.json"

    /**
     * Rows inserted per commit. Small enough that the map can query and draw the
     * places committed so far while the rest of the snapshot is still parsing.
     */
    internal const val BATCH_SIZE = 5_000

    /**
     * The snapshot is read in a few large chunks rather than the 8 KiB default,
     * which cuts the number of UTF-8 decodes over the ~13 MB file.
     */
    private const val READ_BUFFER_SIZE = 1 shl 16

    /**
     * Where the seed records its progress in the preference table. A marker is
     * needed because the import commits in batches: a table with some rows is no
     * longer proof that the snapshot was imported in full.
     */
    internal const val SEED_STATE_KEY = "place_seed_state"

    internal const val SEED_IN_PROGRESS = "in_progress"
    internal const val SEED_COMPLETE = "complete"

    data class ImportResult(
        val placesImported: Long,
        val duration: Duration,
    )

    /**
     * Seeds the places table from the bundled snapshot, reporting the running
     * number of imported places through [onBatch] after each committed batch.
     */
    suspend fun import(
        ctx: Context,
        db: Database,
        onBatch: (Long) -> Unit = {},
    ): ImportResult = importFrom(db, onBatch) { ctx.assets.open(FILE_NAME) }

    /**
     * Seeds [db] from the snapshot produced by [openStream], unless it has
     * already been seeded.
     *
     * The import commits in batches so the caller can surface the rows that have
     * landed instead of waiting for the whole snapshot, and it records progress
     * in the preference table so an import interrupted by a crash is retried
     * rather than mistaken for a finished one.
     *
     * Kept separate from [import] so the seeding logic is testable without an
     * Android [Context] or a real asset.
     */
    internal suspend fun importFrom(
        db: Database,
        onBatch: (Long) -> Unit = {},
        openStream: () -> InputStream,
    ): ImportResult {
        val startedAt = System.nanoTime()

        // Seeding is intentionally a one-shot, fresh-install operation: once it
        // has run, a stale bundle must never overwrite rows that sync has since
        // refreshed. Re-importing a newer asset later is therefore avoided.
        var placesImported = 0L

        // Whether this call got as far as opening the snapshot. A failure before
        // that must not discard rows an earlier run left behind; only a seed
        // this call started (and so marked in progress) is safe to remove.
        var seedingStarted = false
        try {
            if (withContext(Dispatchers.IO) { alreadySeeded(db) }) {
                return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
            }

            withContext(Dispatchers.IO) {
                seedingStarted = true
                db.preference.upsert(SEED_STATE_KEY, SEED_IN_PROGRESS)

                openStream().use { stream ->
                    stream.reader(Charsets.UTF_8).buffered(READ_BUFFER_SIZE).use { reader ->
                        val jsonReader = JsonReader(reader)
                        // The indexes are rebuilt once on the finished table:
                        // maintaining the updated_at expression index and the
                        // bounds index for every seeded row costs more than one
                        // pass over the table. Until they exist the map's
                        // viewport read scans the partially filled table, which
                        // is cheap at this size and lets it draw each batch.
                        dropPlaceIndexes(db)
                        try {
                            jsonReader.beginArray()
                            var batch = mutableListOf<Place>()
                            while (jsonReader.hasNext()) {
                                batch.add(jsonReader.readBundledPlace())
                                if (batch.size >= BATCH_SIZE) {
                                    placesImported += insertBatch(db, batch)
                                    onBatch(placesImported)
                                    batch = mutableListOf()
                                }
                            }
                            if (batch.isNotEmpty()) {
                                placesImported += insertBatch(db, batch)
                                onBatch(placesImported)
                            }
                            jsonReader.endArray()
                        } finally {
                            createPlaceIndexes(db)
                        }
                    }
                }

                if (placesImported > 0) {
                    db.preference.upsert(SEED_STATE_KEY, SEED_COMPLETE)
                } else {
                    // An empty snapshot seeds nothing; leave no marker so a
                    // later snapshot can still seed.
                    db.preference.delete(SEED_STATE_KEY)
                }
            }
        } catch (_: FileNotFoundException) {
            // The snapshot asset is optional; a missing file is not an error.
            if (seedingStarted) discardPartialSeed(db)
            return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
        } catch (e: Exception) {
            // Only recoverable failures are swallowed: an Error must keep
            // propagating instead of being reported as a successful empty seed
            // that lets the caller continue into the sync. The batches already
            // committed are discarded, so the next launch retries from an empty
            // table instead of treating a partial seed as done.
            e.rethrowIfCancellation()
            if (seedingStarted) discardPartialSeed(db)
            return ImportResult(placesImported = 0, duration = elapsedSince(startedAt))
        }

        val duration = elapsedSince(startedAt)
        return ImportResult(placesImported = placesImported, duration = duration)
    }

    /**
     * Whether the snapshot has already run.
     *
     * A database that predates the progress marker still counts as seeded when
     * it holds any place — tombstone included — so upgrading does not re-import
     * over rows that sync has since refreshed. Counting tombstones also stops a
     * table holding only deleted places from resurrecting them.
     */
    private fun alreadySeeded(db: Database): Boolean {
        when (db.preference.select(SEED_STATE_KEY)) {
            SEED_COMPLETE -> return true
            SEED_IN_PROGRESS -> {
                // A previous import was interrupted before it finished; its rows
                // are a prefix of the snapshot, not a complete seed.
                db.place.deleteAll()
                return false
            }
        }

        if (db.place.selectCount(includeDeleted = true) > 0) {
            db.preference.upsert(SEED_STATE_KEY, SEED_COMPLETE)
            return true
        }
        return false
    }

    private fun insertBatch(db: Database, batch: List<Place>): Long {
        db.transaction { db.place.insert(batch) }
        return batch.size.toLong()
    }

    private fun dropPlaceIndexes(db: Database) {
        db.transaction { INDEX_NAMES.forEach { db.conn.execSQL("DROP INDEX IF EXISTS $it;") } }
    }

    private fun createPlaceIndexes(db: Database) {
        db.transaction { CREATE_INDEXES.forEach { db.conn.execSQL(it) } }
    }

    /** Best-effort removal of a seed that did not finish, so it is retried. */
    private fun discardPartialSeed(db: Database) {
        try {
            db.place.deleteAll()
        } catch (t: Exception) {
            // The seed is retried on the next launch regardless; a cancellation
            // still propagates.
            t.rethrowIfCancellation()
        }
        try {
            db.preference.delete(SEED_STATE_KEY)
        } catch (t: Exception) {
            // The marker only makes the retry cheaper.
            t.rethrowIfCancellation()
        }
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
        updatedAt = placeUpdatedAt,
        lat = placeLat,
        lon = placeLon,
        icon = placeIcon,
        name = name,
        localizedName = localizedName,
        verifiedAt = verifiedAt,
        address = address,
        openingHours = openingHours,
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
