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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.SyncEvent
import org.btcmap.databinding.CommentsFragmentBinding
import org.btcmap.db
import org.btcmap.syncController

class CommentsFragment : Fragment() {

    private data class Args(
        val placeId: Long,
    )

    private val args by lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: CommentsFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by lazy {
        ViewModelProvider(this)[CommentsViewModel::class.java]
    }

    /**
     * Items handed to the adapter by the last [renderComments] call. Resumes and
     * the post-payment retry re-read the same rows repeatedly, so the adapter is
     * only updated when the list actually changed.
     */
    private var lastSubmittedItems: List<CommentsAdapterItem>? = null

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
            viewModel.onAddCommentOpened()

            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AddCommentFragment>(
                    R.id.fragmentContainerView,
                    null,
                    Bundle().apply {
                        putLong("place_id", args.placeId)
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
                    if (event == SyncEvent.CommentsChanged) renderComments(adapter)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Hold the empty state back until this resume's sync finishes,
                // not just the first one, so it cannot flash while the list is
                // still being fetched (for example right after a payment).
                syncFinished = false
                val rendered = renderComments(adapter)

                // Syncing on every resume also retries a sync that failed while
                // the screen was in the background. It stays cheap because
                // syncComments only fetches the delta since the stored cursor.
                val synced = viewModel.syncOnResume(
                    renderedNow = rendered,
                    sync = { syncComments() },
                    render = { renderComments(adapter) },
                )

                lastSyncFailed = !synced
                syncFinished = true
                renderComments(adapter)
            }
        }
    }

    private suspend fun renderComments(adapter: CommentsAdapter): List<CommentsAdapterItem> {
        return renderMutex.withLock {
            val items = withContext(Dispatchers.IO) {
                val formatter = commentDateFormatter()
                db().comment.selectByPlaceId(args.placeId).map { it.toAdapterItem(formatter) }
            }

            if (items != lastSubmittedItems) {
                adapter.submitList(items)
                lastSubmittedItems = items
            }
            viewModel.onCommentsRendered(items.map { it.id }.toSet())

            // Only claim the list is empty once a sync attempt finished; after a
            // failure it still says so, but with a message that admits the
            // fetch failed rather than asserting there are no comments. The
            // text is only touched when shown, so a background re-render does
            // not allocate a string every time.
            val emptyMessage = commentsEmptyStateMessageRes(
                syncFinished = syncFinished,
                syncFailed = lastSyncFailed,
                hasComments = items.isNotEmpty(),
            )
            binding.empty.isVisible = emptyMessage != null
            emptyMessage?.let { binding.empty.setText(it) }

            items
        }
    }

    /** Returns whether the sync completed, as opposed to failing. */
    private suspend fun syncComments(): Boolean {
        return !syncController().syncComments().failed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // These describe the view that was showing. Clearing them means a tap
        // before the recreated view renders again cannot snapshot a stale
        // baseline. The pending post-payment flags live in the view model and
        // are deliberately kept.
        lastSubmittedItems = null
        viewModel.onViewDestroyed()
        _binding = null
    }
}
