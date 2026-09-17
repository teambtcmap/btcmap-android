package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.btcmap.R
import org.btcmap.databinding.CommentsFragmentBinding
import org.btcmap.db
import org.btcmap.sync

class CommentsFragment : Fragment() {

    private data class Args(
        val placeId: Long,
    )

    private val args by lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: CommentsFragmentBinding? = null
    private val binding get() = _binding!!

    /**
     * Set when the add screen reports that a comment was paid for. The next
     * resume then retries the sync for a few seconds instead of doing a single
     * request, because the server can report the invoice as paid just before it
     * flips the comment to visible.
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

    /** Ids shown by the last [renderComments] call. */
    private var renderedIds: Set<Long> = emptySet()

    /**
     * True once [renderComments] has populated [renderedIds]. Until then the
     * ids are not a trustworthy baseline for the post-payment retry.
     */
    private var renderedAtLeastOnce = false

    /**
     * False until the current sync finishes. The empty state is held back until
     * then so it cannot flash while the sync is running.
     */
    private var initialSyncDone = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CommentsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restored so a pending retry survives a process recreation before the
        // resume below consumes it.
        postPaymentSync =
            savedInstanceState?.getBoolean(STATE_POST_PAYMENT_SYNC) ?: postPaymentSync
        savedInstanceState?.getLongArray(STATE_PRE_POST_COMMENT_IDS)?.let {
            prePostCommentIds = it.toSet()
        }

        binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CommentsAdapter()
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val margin = (resources.displayMetrics.density * 24).toInt()
            // The insets are physical while marginEnd follows the layout
            // direction, so under RTL the button's end margin must clear the
            // physical left inset, not the right one.
            val endInset = if (v.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                insets.left
            } else {
                insets.right
            }

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = endInset + margin
                bottomMargin = insets.bottom + margin
            }

            WindowInsetsCompat.CONSUMED
        }

        binding.fab.setOnClickListener {
            // Snapshot what the user has seen before the add screen can store
            // anything, so the retry knows which comments are genuinely new.
            // Before the first render the ids are unknown, and an empty set
            // would misread every stored comment as new, so leave the baseline
            // null and let the retry run its full window instead.
            prePostCommentIds = if (renderedAtLeastOnce) renderedIds else null

            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AddCommentFragment>(
                    R.id.fragmentContainerView,
                    null,
                    Bundle().apply { putLong("place_id", args.placeId) }
                )
                addToBackStack(null)
            }
        }

        // The add screen sets this right before popping back once a comment was
        // paid for, so the next resume retries the sync below.
        parentFragmentManager.setFragmentResultListener(
            AddCommentFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, _ ->
            postPaymentSync = true
            // The result is kept by the FragmentManager until cleared, so a
            // later visit to this screen would otherwise retry again.
            parentFragmentManager.clearFragmentResult(AddCommentFragment.REQUEST_KEY)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Hold the empty state back until this resume's sync finishes,
                // not just the first one, so it cannot flash while the list is
                // still being fetched (for example right after a payment).
                initialSyncDone = false
                val rendered = renderComments(adapter)

                val synced = if (postPaymentSync) {
                    postPaymentSync = false
                    syncCommentsWithRetry(adapter, rendered)
                } else {
                    // Syncing on every resume also retries a sync that failed
                    // while the screen was in the background. It stays cheap
                    // because syncComments only fetches the delta since the
                    // stored cursor.
                    syncComments()
                }

                // Only claim the list is empty once a sync actually completed;
                // after a failure an empty list does not mean there are no
                // comments, and asserting it would be misleading.
                initialSyncDone = synced
                renderComments(adapter)
            }
        }
    }

    private suspend fun renderComments(adapter: CommentsAdapter): List<CommentsAdapterItem> {
        val items = withContext(Dispatchers.IO) {
            db().comment.selectByPlaceId(args.placeId).map { it.toAdapterItem() }
        }

        adapter.submitList(items)
        renderedIds = items.map { it.id }.toSet()
        renderedAtLeastOnce = true
        binding.empty.isVisible = initialSyncDone && items.isEmpty()
        return items
    }

    /** Returns whether the sync completed, as opposed to failing. */
    private suspend fun syncComments(): Boolean {
        return !sync().syncComments().failed
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
     * that specific signal (rather than for any stored row) means an unrelated
     * comment syncing in the meantime cannot end the retries.
     *
     * Retries back off and stop after a bounded window so a comment the server
     * never publishes cannot poll forever.
     */
    private suspend fun syncCommentsWithRetry(
        adapter: CommentsAdapter,
        rendered: List<CommentsAdapterItem>,
    ): Boolean {
        // Fall back to the currently shown ids when the add screen was not
        // opened from the list, for example after a process recreation that
        // lost the snapshot taken when the add button was tapped. Nothing is
        // then known to be new, and the retry below simply runs its window.
        val baseline = prePostCommentIds ?: rendered.map { it.id }.toSet()
        prePostCommentIds = null

        // A sync while the add screen was open may already have stored the paid
        // comment; then the list already has it and there is nothing to wait
        // for.
        if (rendered.any { it.id !in baseline }) {
            return true
        }

        // Remember the last attempt's outcome so the caller can tell a window
        // that ended because the comment was never published from one where the
        // server could not be reached.
        var lastSyncSucceeded = true

        withTimeoutOrNull(POST_PAYMENT_SYNC_TIMEOUT_MS) {
            var delayMs = POST_PAYMENT_SYNC_INITIAL_DELAY_MS

            while (true) {
                lastSyncSucceeded = !sync().syncComments().failed

                if (renderComments(adapter).any { it.id !in baseline }) {
                    return@withTimeoutOrNull
                }

                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(POST_PAYMENT_SYNC_MAX_DELAY_MS)
            }
        }

        return lastSyncSucceeded
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_POST_PAYMENT_SYNC, postPaymentSync)
        prePostCommentIds?.let {
            outState.putLongArray(STATE_PRE_POST_COMMENT_IDS, it.toLongArray())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // These ids describe the view that was showing. Clearing them means a
        // tap before the recreated view renders again cannot snapshot a stale
        // baseline. The pending post-payment flags are deliberately kept.
        renderedIds = emptySet()
        renderedAtLeastOnce = false
        _binding = null
    }

    private companion object {
        const val POST_PAYMENT_SYNC_TIMEOUT_MS = 10_000L
        const val POST_PAYMENT_SYNC_INITIAL_DELAY_MS = 500L
        const val POST_PAYMENT_SYNC_MAX_DELAY_MS = 2_000L
        const val STATE_POST_PAYMENT_SYNC = "post_payment_sync"
        const val STATE_PRE_POST_COMMENT_IDS = "pre_post_comment_ids"
    }
}
