package org.btcmap.comment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the comments screen's sync state, including the post-payment retry.
 *
 * The state lives here rather than in the fragment so it survives a
 * configuration change without hand-rolled instance-state plumbing: the
 * fragment drives the work from its view lifecycle, but a recreated view picks
 * the same pending retry and baseline back up.
 */
internal class CommentsViewModel : ViewModel() {

    /**
     * Set when the add screen reports that a comment was paid for. The next
     * resume then retries the sync instead of doing a single request, because
     * the server can report the invoice as paid just before it flips the
     * comment to visible.
     */
    private var postPaymentSync = false

    /**
     * Ids of the comments the user saw before opening the add screen. The retry
     * uses them to tell whether the paid comment is already in the list, so a
     * sync that stored it while the add screen was open does not trigger a
     * pointless retry. Null when the add screen was not opened from here, or
     * when the list had not rendered yet and the ids would be meaningless.
     */
    private var prePostCommentIds: Set<Long>? = null

    /** Ids shown by the last render. */
    private var renderedIds: Set<Long> = emptySet()

    /**
     * True once the view has rendered, so [renderedIds] is a trustworthy
     * baseline. Until then the ids are not a trustworthy baseline for the
     * post-payment retry.
     */
    private var renderedAtLeastOnce = false

    /**
     * Records the ids the current view is showing, so a later
     * [onAddCommentOpened] has a baseline to snapshot.
     */
    fun onCommentsRendered(ids: Set<Long>) {
        renderedIds = ids
        renderedAtLeastOnce = true
    }

    /**
     * Drops the ids that describe the destroyed view, so a tap before the
     * recreated view renders again cannot snapshot a stale baseline. The
     * pending post-payment flags are deliberately kept.
     */
    fun onViewDestroyed() {
        renderedIds = emptySet()
        renderedAtLeastOnce = false
    }

    /**
     * Snapshots what the user has seen before the add screen can store
     * anything, so the retry knows which comments are genuinely new. Before the
     * first render the ids are unknown, and an empty set would misread every
     * stored comment as new, so the baseline stays null and the retry runs its
     * full window instead.
     */
    fun onAddCommentOpened() {
        prePostCommentIds = if (renderedAtLeastOnce) renderedIds else null
    }

    /** Records that the add screen reported a paid comment. */
    fun onCommentPosted() {
        postPaymentSync = true
    }

    /**
     * Runs the sync that a resume needs: a single delta sync, or the bounded
     * retry after a payment. Returns whether the last sync completed, as
     * opposed to failing.
     *
     * The pending flags are cleared only once the retry returns normally. A
     * cancellation (leaving the screen, a configuration change) keeps them, so
     * the recreated view retries with the original baseline instead of giving
     * up after a single sync.
     */
    suspend fun syncOnResume(
        renderedIds: Set<Long>,
        sync: suspend () -> Boolean,
        render: suspend () -> Set<Long>,
    ): Boolean {
        if (!postPaymentSync) return sync()

        val baseline = prePostCommentIds ?: renderedIds
        val result = syncAfterPayment(renderedIds, baseline, sync, render)
        postPaymentSync = false
        prePostCommentIds = null
        return result
    }

    /**
     * Retries the delta sync after a payment until the paid comment shows up.
     *
     * The invoice can be reported as paid before the server has flipped the new
     * comment to visible, and because the list only syncs on resume, a single
     * request in that window would leave the comment hidden until the user
     * leaves and re-enters the screen.
     *
     * A hidden comment is dropped instead of stored, so the paid comment shows
     * up as an id that was not shown before the post flow started. Waiting for
     * a new id means an unrelated comment for another place cannot end the
     * retries. A different comment for the same place published first would
     * look like the expected signal, though: the paid comment's id is assigned
     * by the server, so the two cannot be told apart.
     *
     * Retries back off and stop after a bounded window so a comment the server
     * never publishes cannot poll forever.
     */
    private suspend fun syncAfterPayment(
        renderedIds: Set<Long>,
        baseline: Set<Long>,
        sync: suspend () -> Boolean,
        render: suspend () -> Set<Long>,
    ): Boolean {
        // A sync while the add screen was open may already have stored the paid
        // comment; then the list already has it and there is nothing to wait
        // for.
        if (renderedIds.any { it !in baseline }) {
            return true
        }

        // Remember the last attempt's outcome so the caller can tell a window
        // that ended because the comment was never published from one where the
        // server could not be reached.
        var lastSyncSucceeded = true

        withTimeoutOrNull(POST_PAYMENT_SYNC_TIMEOUT_MS) {
            var delayMs = POST_PAYMENT_SYNC_INITIAL_DELAY_MS

            while (true) {
                lastSyncSucceeded = sync()

                if (render().any { it !in baseline }) {
                    return@withTimeoutOrNull
                }

                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(POST_PAYMENT_SYNC_MAX_DELAY_MS)
            }
        }

        return lastSyncSucceeded
    }

    private companion object {
        const val POST_PAYMENT_SYNC_TIMEOUT_MS = 10_000L
        const val POST_PAYMENT_SYNC_INITIAL_DELAY_MS = 500L
        const val POST_PAYMENT_SYNC_MAX_DELAY_MS = 2_000L
    }
}
