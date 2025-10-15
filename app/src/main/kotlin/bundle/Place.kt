package bundle

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import db.table.place.Place
import db.table.place.PlaceQueries
import json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class BundledPlace(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
    val comments: Long?,
    val boostedUntil: ZonedDateTime?,
)

object BundledPlaces {
    const val FILE_NAME = "bundled-places.json"

    suspend fun import(ctx: Context, db: SQLiteDatabase): Boolean {
        var hadDbWrites = false

        withContext(Dispatchers.IO) {
            if (PlaceQueries.selectCount(db) > 0) {
                return@withContext
            }
            if (!ctx.resources.assets.list("")!!.contains(FILE_NAME)) {
                return@withContext
            }
            val bundledPlaces = ctx.assets.open(FILE_NAME).use { it.toBundledPlaces() }
            db.transaction { PlaceQueries.insert(bundledPlaces.map { it.toPlace() }, this) }
            hadDbWrites = true
        }

        return hadDbWrites
    }

    private fun InputStream.toBundledPlaces(): List<BundledPlace> {
        return toJsonArray().map { it.toBundledPlace() }
    }

    private fun JSONObject.toBundledPlace(): BundledPlace {
        return BundledPlace(
            id = getLong("id"),
            lat = getDouble("lat"),
            lon = getDouble("lon"),
            icon = getString("icon"),
            name = getString("name"),
            comments = if (has("comments")) getLong("comments") else null,
            boostedUntil = if (has("boosted_until")) ZonedDateTime.parse(getString("boosted_until")) else null,
        )
    }

    private fun BundledPlace.toPlace(): Place {
        return Place(
            id = this.id,
            lat = this.lat,
            lon = this.lon,
            icon = this.icon,
            name = this.name,
            updatedAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
            requiredAppUrl = null,
            boostedUntil = this.boostedUntil,
            verifiedAt = null,
            address = null,
            openingHours = null,
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