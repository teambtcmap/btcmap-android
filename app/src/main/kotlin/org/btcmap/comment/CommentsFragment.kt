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

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right + (resources.displayMetrics.density * 24).toInt()
                bottomMargin = insets.bottom + (resources.displayMetrics.density * 24).toInt()
                leftMargin = insets.left
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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                renderComments(adapter)

                // Syncing on every resume also retries a sync that failed while
                // the screen was in the background. It stays cheap because
                // syncComments only fetches the delta since the stored cursor.
                if (sync().syncComments().rowsAffected > 0) {
                    renderComments(adapter)
                }
            }
        }
    }

    private suspend fun renderComments(adapter: CommentsAdapter) {
        val items = withContext(Dispatchers.IO) {
            db().comment.selectByPlaceId(args.placeId).map { it.toAdapterItem() }
        }

        adapter.submitList(items)
        binding.empty.isVisible = items.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
