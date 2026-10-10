package org.btcmap.api

import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.comment.commentOf
import org.btcmap.util.toInstant

/**
 * The comment row both the delta sync and the bundled seed store, so the two
 * paths map the API's fields the same way.
 */
internal fun GetCommentsItem.toComment(): Comment = commentOf(
    id = id,
    placeId = placeId,
    comment = comment,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
