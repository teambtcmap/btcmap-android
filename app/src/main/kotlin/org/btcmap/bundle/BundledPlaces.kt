package org.btcmap.bundle

import android.content.Context
import org.btcmap.db.table.Place
import org.btcmap.json.toJsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import java.io.InputStream
import java.time.ZonedDateTime

object BundledPlaces {
    const val FILE_NAME = "bundled-places.json"

    suspend fun import(ctx: Context, db: Database): Boolean {
        var hadDbWrites = false

        withContext(Dispatchers.IO) {
            if (db.place.selectCount() > 0) {
                return@withContext
            }
            if (!ctx.resources.assets.list("")!!.contains(FILE_NAME)) {
                return@withContext
            }
            val bundledPlaces = ctx.assets.open(FILE_NAME).use { it.toBundledPlaces() }
            db.place.insert(bundledPlaces.map { it.toPlace() })
            hadDbWrites = true
        }

        return hadDbWrites
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
            boostedUntil = if (!has("boosted_until") || get("boosted_until").isJsonNull) null else ZonedDateTime.parse(get("boosted_until").asString),
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