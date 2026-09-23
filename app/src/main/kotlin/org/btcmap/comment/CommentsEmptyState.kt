package org.btcmap.comment

import androidx.annotation.StringRes
import org.btcmap.R

/**
 * The empty-state message the comments screen should show, or null when it must
 * stay hidden.
 *
 * The empty state only appears once a sync attempt finished, so it cannot flash
 * while the list is still being fetched, and it distinguishes a list that is
 * genuinely empty from one that could not be loaded.
 */
@StringRes
internal fun commentsEmptyStateMessageRes(
    syncFinished: Boolean,
    syncFailed: Boolean,
    hasComments: Boolean,
): Int? = when {
    !syncFinished || hasComments -> null
    syncFailed -> R.string.failed_to_load
    else -> R.string.no_comments_yet
}
