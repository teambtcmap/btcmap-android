package org.btcmap.comment

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.btcmap.db.table.comment.Comment
import org.btcmap.platform.currentLanguage

/** Formats a comment's created instant for display. */
fun interface CommentDateFormatter {
    fun format(instant: Instant): String
}

/**
 * Builds the formatter used for a place's comments.
 *
 * The formatter is built per render rather than kept as a constant so it
 * follows the current [timeZone] and [language] (the device ones by default)
 * instead of the values that were active when the process started.
 */
expect fun commentDateFormatter(
    timeZone: String = TimeZone.currentSystemDefault().id,
    language: String = currentLanguage(),
): CommentDateFormatter

/**
 * Maps a stored comment to its list item using an already built [formatter].
 *
 * The stored [Comment.createdAt] is an instant, so the formatter converts it to
 * the device zone before formatting; otherwise a comment posted late in the day
 * could be shown with the previous day's date.
 */
fun Comment.toAdapterItem(formatter: CommentDateFormatter): CommentsAdapterItem =
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
fun Comment.toAdapterItem(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    language: String = currentLanguage(),
): CommentsAdapterItem = toAdapterItem(commentDateFormatter(timeZone.id, language))
