package org.btcmap.db.table.note

import kotlin.time.Instant

/**
 * One of the signed-in user's personal notes, as cached locally.
 *
 * A soft-deleted note is kept as a tombstone ([deletedAt] set) so the delta
 * sync's `max(updated_at)` cursor advances past the deletion; reads exclude it.
 */
data class Note(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val text: String,
    /** The pin discriminator, a Material Symbols name; the API defaults it to `notes`. */
    val icon: String,
    val public: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** When the note was soft-deleted, or null while it is live. */
    val deletedAt: Instant? = null,
)
