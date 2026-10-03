@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.btcmap.dbstats

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import org.btcmap.json.btcmapJson
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toInstantOrNull

/** Stats about a bundled snapshot asset. */
data class BundleStats(
    val location: String,
    val sizeBytes: Long,
    val visibleCount: Long,
    val deletedCount: Long,
    val maxUpdatedAt: String?,
)

/** The bundle stats that were read and the snapshots that failed to read. */
data class BundleReads(
    val stats: Map<String, BundleStats>,
    val failures: List<Throwable>,
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
     * Reads the snapshot opened by [openSource], or returns null when the asset
     * does not exist (each snapshot is optional). [location] is reported as-is,
     * so the caller controls how the asset path is displayed.
     */
    fun read(location: String, openSource: () -> Source?): BundleStats? {
        val source = openSource() ?: return null

        // The snapshot is streamed rather than buffered whole: it can be many
        // megabytes, and only the counts and the byte size are needed. The
        // counting source makes the exact size available once the reader has
        // consumed the asset to the end.
        val counting = CountingSource(source)
        val buffered = counting.buffer()
        var visible = 0L
        var deleted = 0L
        var maxUpdatedAt: String? = null
        var maxUpdatedAtInstant: Instant? = null
        try {
            for (record in btcmapJson.decodeBufferedSourceToSequence<BundleRecordJson>(
                buffered,
                DecodeSequenceMode.ARRAY_WRAPPED,
            )) {
                if (record.deletedAt == null) visible++ else deleted++

                // Timestamps are stored as ISO-8601 text and are not
                // fixed-width (a zero fraction is dropped), so the newest is
                // found by parsing and comparing chronologically, not as text.
                val updatedAt = record.updatedAt?.toInstantOrNull() ?: continue
                if (maxUpdatedAtInstant == null || updatedAt > maxUpdatedAtInstant) {
                    maxUpdatedAtInstant = updatedAt
                    maxUpdatedAt = record.updatedAt
                }
            }
        } finally {
            // Drain what the parser did not need (the final newline, say) so the
            // counted size covers the whole asset, then close the stream.
            while (!buffered.exhausted()) {
                buffered.readByteArray()
            }
            buffered.close()
        }

        return BundleStats(
            location = location,
            sizeBytes = counting.count,
            visibleCount = visible,
            deletedCount = deleted,
            maxUpdatedAt = maxUpdatedAt,
        )
    }
}

/** The fields a snapshot record reports for the stats. */
@Serializable
private class BundleRecordJson(
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * Reads every snapshot in [bundles], a table name to asset file name map,
 * through [openSource].
 *
 * Every snapshot is optional and independent: one missing or malformed asset
 * must not stop the others or hide the database stats, so a failure is
 * collected in [BundleReads.failures] instead of thrown. A snapshot that does
 * not exist is simply absent from [BundleReads.stats] and is not a failure.
 * Cancellation is rethrown so the caller's coroutine still unwinds.
 */
fun readBundles(
    bundles: Map<String, String>,
    openSource: (String) -> Source?,
): BundleReads {
    val stats = mutableMapOf<String, BundleStats>()
    val failures = mutableListOf<Throwable>()

    bundles.forEach { (table, fileName) ->
        try {
            BundleReader.read(
                location = "assets/$fileName",
                openSource = { openSource(fileName) },
            )?.let { stats[table] = it }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            failures.add(e)
        }
    }

    return BundleReads(stats = stats, failures = failures)
}

/**
 * Counts the bytes read through it, so a streamed asset's size is known without
 * buffering the whole thing in memory.
 */
private class CountingSource(
    delegate: Source,
) : ForwardingSource(delegate) {

    var count: Long = 0L
        private set

    override fun read(sink: Buffer, byteCount: Long): Long {
        val read = super.read(sink, byteCount)
        if (read > 0) count += read
        return read
    }
}
