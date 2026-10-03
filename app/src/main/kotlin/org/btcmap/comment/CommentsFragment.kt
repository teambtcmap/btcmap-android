package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.btcmap.R
import org.btcmap.databinding.CommentsFragmentBinding
import org.btcmap.db
import org.btcmap.syncController
import org.btcmap.ui.CommentsLabels

/**
 * A place's comments: a toolbar over the shared
 * [org.btcmap.ui.CommentsPage], which syncs, reads and renders the list. This
 * fragment supplies the sync, the labels and the add-comment navigation, and
 * re-runs the page once a posted comment comes back.
 */
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

        binding.commentsList.apply {
            database = db()
            placeId = args.placeId
            labels = CommentsLabels(
                add = getString(R.string.add),
                failedToLoad = getString(R.string.failed_to_load),
                noComments = getString(R.string.no_comments_yet),
            )
            sync = { !syncController().syncComments().failed }
            iconTypeface = org.btcmap.util.iconTypeface
            onAddComment = { openAddComment() }
        }

        // The add screen sets this right before popping back once a comment was
        // paid for, so the list re-syncs and re-reads.
        parentFragmentManager.setFragmentResultListener(
            AddCommentFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, _ ->
            binding.commentsList.reloadKey++
        }
    }

    private fun openAddComment() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
