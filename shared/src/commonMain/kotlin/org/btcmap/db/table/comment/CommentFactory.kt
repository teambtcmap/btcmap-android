package org.btcmap.db.table.comment

import kotlin.time.Instant

/**
 * The one place a [Comment] is built from an API delta or a bundled snapshot.
 * Dates are passed already parsed: the two paths parse them with different
 * strictness and validate required fields themselves.
 */
internal fun commentOf(
    id: Long,
    placeId: Long,
    comment: String,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant? = null,
): Comment = Comment(
    id = id,
    placeId = placeId,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
