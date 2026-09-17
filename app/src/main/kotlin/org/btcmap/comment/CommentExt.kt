package org.btcmap.comment

import org.btcmap.db.table.comment.Comment
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val commentDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * Maps a stored comment to its list item.
 *
 * The stored [Comment.createdAt] keeps the offset returned by the API, so it is
 * converted to [zone] (the device zone by default) before formatting; otherwise
 * a comment posted late in the day could be shown with the previous day's date.
 */
internal fun Comment.toAdapterItem(zone: ZoneId = ZoneId.systemDefault()): CommentsAdapterItem =
    CommentsAdapterItem(
        id = id,
        comment = comment,
        localizedDate = createdAt.withZoneSameInstant(zone).format(commentDateFormat),
    )
