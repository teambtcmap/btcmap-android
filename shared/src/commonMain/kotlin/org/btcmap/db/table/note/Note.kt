package org.btcmap.db.table.note

import kotlin.time.Instant

/** One of the signed-in user's personal notes, as cached locally. */
data class Note(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val text: String,
    val public: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
