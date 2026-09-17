package org.btcmap.comment

import org.btcmap.db.table.comment.Comment
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Maps a stored comment to its list item.
 *
 * The stored [Comment.createdAt] keeps the offset returned by the API, so it is
 * converted to [zone] (the device zone by default) before formatting; otherwise
 * a comment posted late in the day could be shown with the previous day's date.
 *
 * The formatter is built per call so it follows the current [locale] (the
 * device locale by default) instead of the locale that was active when the
 * process started.
 */
internal fun Comment.toAdapterItem(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
): CommentsAdapterItem =
    CommentsAdapterItem(
        id = id,
        comment = comment,
        localizedDate = createdAt.withZoneSameInstant(zone)
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            ),
    )
