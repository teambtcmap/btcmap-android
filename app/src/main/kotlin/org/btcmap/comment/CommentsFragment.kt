package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.databinding.CommentsFragmentBinding
import org.btcmap.db
import org.btcmap.sync.SyncEvent
import org.btcmap.syncController
import org.btcmap.util.iconTypeface

class CommentsFragment : Fragment() {

    private data class Args(
        val placeId: Long,
        val placeName: String?,
    )

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            placeName = requireArguments().getString("place_name"),
        )
    }

    private var _binding: CommentsFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by lazy {
        ViewModelProvider(this)[CommentsViewModel::class.java]
    }

    /**
     * True once the current resume's sync attempt has finished, so the empty
     * state is held back while the list is still being fetched.
     */
    private var syncFinished = false

    /**
     * Whether the last finished sync failed. The empty state then says the
     * comments could not be loaded instead of claiming there are none.
     */
    private var lastSyncFailed = false

    /**
     * Serializes [renderComments]: the change observer and the resume sync can
     * both render, and without this the two reads could interleave and submit
     * the list out of order.
     */
    private val renderMutex = Mutex()

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

        binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        binding.commentsList.iconTypeface = iconTypeface
        binding.commentsList.addDescription = getString(R.string.add)
        binding.commentsList.onAddComment = {
            // Snapshot what the user has seen before the add screen can store
            // anything, so the retry knows which comments are genuinely new.
            viewModel.onAddCommentOpened()

            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AddCommentFragment>(
                    R.id.fragmentContainerView,
                    null,
                    Bundle().apply {
                        putLong("place_id", args.placeId)
                        putString("place_name", args.placeName)
                        putBoolean(AddCommentFragment.ARG_NOTIFY_ON_POSTED, true)
                    }
                )
                addToBackStack(null)
            }
        }

        // The add screen sets this right before popping back once a comment was
        // paid for, so the next resume retries the sync below. The add screen is
        // told to set it only when it was opened from here, so a comment posted
        // straight from the place screen cannot leave a stale result behind for
        // a later visit. The FragmentManager clears the result once this view
        // has received it.
        parentFragmentManager.setFragmentResultListener(
            AddCommentFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            viewModel.onCommentPosted(result.getString(AddCommentFragment.ARG_POSTED_COMMENT))
        }

        // A background sync (the app-scoped full sync, or another screen's) can
        // publish the paid comment after the retry window; re-render when it
        // does instead of waiting for the next resume.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncController().events.collect { event ->
                    if (event == SyncEvent.CommentsChanged) renderComments()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Hold the empty state back until this resume's sync finishes,
                // not just the first one, so it cannot flash while the list is
                // still being fetched (for example right after a payment).
                syncFinished = false
                val rendered = renderComments()

                // Syncing on every resume also retries a sync that failed while
                // the screen was in the background. It stays cheap because
                // syncComments only fetches the delta since the stored cursor.
                val synced = viewModel.syncOnResume(
                    renderedNow = rendered,
                    sync = { syncComments() },
                    render = { renderComments() },
                )

                lastSyncFailed = !synced
                syncFinished = true
                renderComments()
            }
        }
    }

    private suspend fun renderComments(): List<CommentsAdapterItem> {
        return renderMutex.withLock {
            val items = withContext(Dispatchers.IO) {
                val formatter = commentDateFormatter()
                db().comment.selectByPlaceId(args.placeId).map { it.toAdapterItem(formatter) }
            }

            // Only claim the list is empty once a sync attempt finished; after a
            // failure it still says so, but with a message that admits the fetch
            // failed rather than asserting there are no comments.
            val emptyMessage = commentsEmptyStateMessageRes(
                syncFinished = syncFinished,
                syncFailed = lastSyncFailed,
                hasComments = items.isNotEmpty(),
            )

            _binding?.commentsList?.let {
                it.items = items
                it.emptyMessage = emptyMessage?.let(::getString)
            }
            viewModel.onCommentsRendered(items.map { it.id }.toSet())

            items
        }
    }

    /** Returns whether the sync completed, as opposed to failing. */
    private suspend fun syncComments(): Boolean {
        return !syncController().syncComments().failed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The pending post-payment flags live in the view model and are
        // deliberately kept across a view recreation.
        viewModel.onViewDestroyed()
        _binding = null
    }
}
