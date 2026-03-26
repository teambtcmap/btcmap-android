package org.btcmap.bundle

import android.content.Context
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.Place
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

object BundledPlaces {
    private const val FILE_NAME = "bundled-places.json"

    private const val BATCH_SIZE = 10_000

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
        var placesImported = 0L
        withContext(Dispatchers.IO) {
            db.transaction {
                ctx.assets.open(FILE_NAME).use { stream ->
                    stream.bufferedReader().use { reader ->
                        val jsonReader = JsonReader(reader)
                        jsonReader.beginArray()
                        val batch = mutableListOf<Place>()
                        while (jsonReader.hasNext()) {
                            batch.add(jsonReader.readPlace())
                            if (batch.size >= BATCH_SIZE) {
                                db.place.insert(batch.toList())
                                placesImported += batch.size
                                batch.clear()
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
        return ImportResult(
            placesImported = placesImported,
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
        )
    }

    private fun JsonReader.readPlace(): Place {
        var id = 0L
        var lat = 0.0
        var lon = 0.0
        var icon = ""
        var name = ""
        var comments: Long? = null
        var boostedUntil: ZonedDateTime? = null
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "id" -> id = nextLong()
                "lat" -> lat = nextDouble()
                "lon" -> lon = nextDouble()
                "icon" -> icon = nextString()
                "name" -> name = nextString()
                "comments" -> {
                    if (peek() == com.google.gson.stream.JsonToken.NULL) {
                        skipValue()
                        comments = null
                    } else {
                        comments = nextLong()
                    }
                }
                "boosted_until" -> {
                    if (peek() == com.google.gson.stream.JsonToken.NULL) {
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
        return Place(
            id = id,
            lat = lat,
            lon = lon,
            icon = icon,
            name = name,
            localizedName = null,
            updatedAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
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
        )
    }
}