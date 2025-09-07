package element_comment

import android.content.Context
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
import element.CommentsAdapter
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentElementCommentsBinding

class ElementCommentsFragment : Fragment() {

    private data class Args(
        val elementId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("element_id"))
    }

    private var _binding: FragmentElementCommentsBinding? = null
    private val binding get() = _binding!!

    private val adapter = CommentsAdapter()

    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { v, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                    v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = insets.top
                        rightMargin = insets.right + v.context.dpToPx(24)
                        bottomMargin = insets.bottom + v.context.dpToPx(24)
                        leftMargin = insets.left
                    }

                    WindowInsetsCompat.CONSUMED
                }
                binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
                binding.list.layoutManager = LinearLayoutManager(requireContext())
                binding.list.adapter = adapter
                binding.list.setHasFixedSize(true)
                binding.fab.setOnClickListener {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<AddElementCommentFragment>(
                            R.id.fragmentContainerView,
                            null,
                            bundleOf("element_id" to args.value.elementId)
                        )
                        addToBackStack(null)
                    }
                }
                showComments()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showComments() {
        val comments = CommentQueries.selectByPlaceId(args.value.elementId, db)
        adapter.submitList(comments)
    }
}