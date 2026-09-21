package org.btcmap.dbstats

import com.google.gson.stream.JsonReader
import org.btcmap.bundle.nextStringOrNull
import org.btcmap.bundle.toZonedDateTimeOrNull
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.ZonedDateTime

/** Stats about a bundled snapshot asset. */
data class BundleStats(
    val location: String,
    val sizeBytes: Long,
    val visibleCount: Long,
    val deletedCount: Long,
    val maxUpdatedAt: String?,
)

/**
 * Reads a bundled snapshot asset to report its size, record counts and newest
 * update time.
 *
 * Every snapshot is a JSON array of records. The bundlers do not request
 * `deleted_at`, so the snapshots currently hold no tombstones and every record
 * counts as visible, but the field is still checked so a hand-edited asset is
 * reported truthfully rather than assumed.
 */
object BundleReader {

    /**
     * Reads the snapshot opened by [openStream], or returns null when the asset
     * does not exist (each snapshot is optional). [location] is reported as-is,
     * so the caller controls how the asset path is displayed.
     */
    fun read(location: String, openStream: () -> InputStream): BundleStats? {
        val bytes = try {
            openStream().use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            return null
        }

        var visible = 0L
        var deleted = 0L
        var maxUpdatedAt: String? = null
        var maxUpdatedAtInstant: ZonedDateTime? = null
        JsonReader(bytes.inputStream().bufferedReader()).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                val record = reader.readRecord()
                if (record.deletedAt == null) visible++ else deleted++

                // Timestamps are stored as ISO-8601 text and are not
                // fixed-width (a zero fraction is dropped), so the newest is
                // found by parsing and comparing chronologically, not as text.
                val updatedAt = record.updatedAt?.toZonedDateTimeOrNull() ?: continue
                if (maxUpdatedAtInstant == null || updatedAt.isAfter(maxUpdatedAtInstant)) {
                    maxUpdatedAtInstant = updatedAt
                    maxUpdatedAt = record.updatedAt
                }
            }
            reader.endArray()
        }

        return BundleStats(
            location = location,
            sizeBytes = bytes.size.toLong(),
            visibleCount = visible,
            deletedCount = deleted,
            maxUpdatedAt = maxUpdatedAt,
        )
    }

    /** The fields a snapshot record reports for the stats. */
    private class Record(
        val deletedAt: String?,
        val updatedAt: String?,
    )

    /**
     * Skips the current value, returning the fields the stats read from it.
     */
    private fun JsonReader.readRecord(): Record {
        var deletedAt: String? = null
        var updatedAt: String? = null
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                DELETED_AT -> deletedAt = nextStringOrNull()
                UPDATED_AT -> updatedAt = nextStringOrNull()
                else -> skipValue()
            }
        }
        endObject()
        return Record(deletedAt = deletedAt, updatedAt = updatedAt)
    }

    private const val DELETED_AT = "deleted_at"
    private const val UPDATED_AT = "updated_at"
}
