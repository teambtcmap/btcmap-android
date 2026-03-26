package org.btcmap.bundle

import android.content.Context
import org.btcmap.db.table.Place
import org.btcmap.json.toJsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import java.io.InputStream
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

object BundledPlaces {
    const val FILE_NAME = "bundled-places.json"

    data class ImportResult(
        val placesImported: Long,
        val duration: Duration,
    )

    suspend fun import(ctx: Context, db: Database): ImportResult {
        val startedAt = OffsetDateTime.now()
        val placesInDb = withContext(Dispatchers.IO) { db.place.selectCount() }
        if (placesInDb > 0) {
            return ImportResult(
                placesImported = 0,
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            )
        }
        val assets =
            withContext(Dispatchers.IO) { ctx.resources.assets.list("") ?: emptyArray<String>() }
        if (!assets.contains(FILE_NAME)) {
            return ImportResult(
                placesImported = 0,
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            )
        }
        val bundledPlaces = withContext(Dispatchers.IO) {
            ctx.assets.open(FILE_NAME).use { it.toBundledPlaces() }
        }
        withContext(Dispatchers.IO) {
            db.place.insert(bundledPlaces.map { it.toPlace() })
        }
        return ImportResult(
            placesImported = bundledPlaces.size.toLong(),
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
        )
    }

    private fun InputStream.toBundledPlaces(): List<BundledPlace> {
        return toJsonArray().map { it.toBundledPlace() }
    }

    private fun JsonObject.toBundledPlace(): BundledPlace {
        return BundledPlace(
            id = get("id").asLong,
            lat = get("lat").asDouble,
            lon = get("lon").asDouble,
            icon = get("icon").asString,
            name = get("name").asString,
            comments = if (!has("comments") || get("comments").isJsonNull) null else get("comments").asLong,
            boostedUntil = if (!has("boosted_until") || get("boosted_until").isJsonNull) null else ZonedDateTime.parse(
                get("boosted_until").asString
            ),
        )
    }

    private fun BundledPlace.toPlace(): Place {
        return Place(
            id = this.id,
            lat = this.lat,
            lon = this.lon,
            icon = this.icon,
            name = this.name,
            localizedName = null,
            updatedAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
            requiredAppUrl = null,
            boostedUntil = this.boostedUntil,
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
            comments = this.comments,
            telegram = null,
        )
    }
}