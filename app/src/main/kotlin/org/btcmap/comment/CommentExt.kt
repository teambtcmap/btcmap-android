package org.btcmap.comment

import org.btcmap.db.table.comment.Comment
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Builds the date formatter used for a place's comments.
 *
 * The formatter is built per render rather than kept as a constant so it follows
 * the current [zone] and [locale] (the device ones by default) instead of the
 * values that were active when the process started.
 */
internal fun commentDateFormatter(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(zone)

/**
 * Maps a stored comment to its list item using an already built [formatter].
 *
 * The stored [Comment.createdAt] keeps the offset returned by the API, so the
 * formatter converts it to the device zone before formatting; otherwise a
 * comment posted late in the day could be shown with the previous day's date.
 */
internal fun Comment.toAdapterItem(formatter: DateTimeFormatter): CommentsAdapterItem =
    CommentsAdapterItem(
        id = id,
        comment = comment,
        localizedDate = formatter.format(createdAt),
    )

/**
 * Maps a stored comment to its list item.
 *
 * Prefer [toAdapterItem] with a shared formatter when mapping several comments;
 * this overload exists for callers that handle a single comment.
 */
internal fun Comment.toAdapterItem(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
): CommentsAdapterItem = toAdapterItem(commentDateFormatter(zone, locale))
