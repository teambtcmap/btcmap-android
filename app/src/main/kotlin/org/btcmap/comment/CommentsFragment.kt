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

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = insets.right + margin
                bottomMargin = insets.bottom + margin
            }

            WindowInsetsCompat.CONSUMED
        }

        binding.fab.setOnClickListener {
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
                renderComments(adapter)

                if (postPaymentSync) {
                    postPaymentSync = false
                    syncCommentsWithRetry(adapter)
                } else {
                    // Syncing on every resume also retries a sync that failed
                    // while the screen was in the background. It stays cheap
                    // because syncComments only fetches the delta since the
                    // stored cursor.
                    syncComments(adapter)
                }
            }
        }
    }

    private suspend fun renderComments(adapter: CommentsAdapter): List<CommentsAdapterItem> {
        val items = withContext(Dispatchers.IO) {
            db().comment.selectByPlaceId(args.placeId).map { it.toAdapterItem() }
        }

        adapter.submitList(items)
        binding.empty.isVisible = items.isEmpty()
        return items
    }

    private suspend fun syncComments(adapter: CommentsAdapter) {
        if (sync().syncComments().rowsAffected > 0) {
            renderComments(adapter)
        }
    }

    /**
     * Retries the delta sync shortly after a payment.
     *
     * The invoice can be reported as paid before the server has flipped the new
     * comment to visible, and because the list only syncs on resume, a single
     * request in that window would leave the comment hidden until the user
     * leaves and re-enters the screen.
     */
    private suspend fun syncCommentsWithRetry(adapter: CommentsAdapter) {
        // A hidden comment is dropped instead of stored, so the paid comment
        // shows up as a comment id that was not in the list before. Waiting for
        // that specific signal (rather than for any stored row) means an
        // unrelated comment syncing in the meantime cannot end the retries.
        val knownIds = withContext(Dispatchers.IO) {
            db().comment.selectByPlaceId(args.placeId).map { it.id }.toSet()
        }

        repeat(POST_PAYMENT_SYNC_ATTEMPTS) { attempt ->
            sync().syncComments()

            if (renderComments(adapter).any { it.id !in knownIds }) {
                return
            }

            if (attempt < POST_PAYMENT_SYNC_ATTEMPTS - 1) {
                delay(POST_PAYMENT_SYNC_DELAY_MS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val POST_PAYMENT_SYNC_ATTEMPTS = 6
        const val POST_PAYMENT_SYNC_DELAY_MS = 500L
    }
}
