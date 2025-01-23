package element_comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import element.CommentsAdapter
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentElementCommentsBinding
import org.koin.android.ext.android.inject

class ElementCommentsFragment : Fragment() {

    private data class Args(
        val elementId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("element_id"))
    }

    private var _binding: FragmentElementCommentsBinding? = null
    private val binding get() = _binding!!

    private val elementCommentRepo: ElementCommentRepo by inject()

    private val adapter = CommentsAdapter()

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
                binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
                binding.topAppBar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_add -> {
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                replace<AddElementCommentFragment>(
                                    R.id.nav_host_fragment,
                                    null,
                                    bundleOf("element_id" to args.value.elementId)
                                )
                                addToBackStack(null)
                            }
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
                binding.list.layoutManager = LinearLayoutManager(requireContext())
                binding.list.adapter = adapter
                binding.list.setHasFixedSize(true)
                binding.fab.setOnClickListener {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<AddElementCommentFragment>(
                            R.id.nav_host_fragment,
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

    private suspend fun showComments() {
        val comments = elementCommentRepo.selectByElementId(args.value.elementId)
        adapter.submitList(comments)
    }
}