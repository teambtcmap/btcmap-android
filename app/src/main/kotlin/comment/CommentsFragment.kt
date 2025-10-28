package comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import db.db
import db.table.comment.CommentQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.databinding.FragmentElementCommentsBinding
import sync.CommentSync
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CommentsFragment : Fragment() {

    private data class Args(
        val placeId: Long,
    )

    private val args by lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: FragmentElementCommentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentElementCommentsBinding.inflate(inflater, container, false)
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
                    bundleOf("place_id" to args.placeId)
                )
                addToBackStack(null)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val comments = withContext(Dispatchers.IO) {
                    CommentQueries.selectByPlaceId(args.placeId, db)
                }

                val commentDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

                adapter.submitList(comments.map {
                    CommentsAdapterItem(
                        comment = it.comment,
                        localizedDate = it.createdAt.format(commentDateFormat),
                    )
                })

                if (CommentSync.run(db).rowsAffected > 0) {
                    val comments = withContext(Dispatchers.IO) {
                        CommentQueries.selectByPlaceId(args.placeId, db)
                    }

                    adapter.submitList(comments.map {
                        CommentsAdapterItem(
                            comment = it.comment,
                            localizedDate = it.createdAt.format(commentDateFormat),
                        )
                    })
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}