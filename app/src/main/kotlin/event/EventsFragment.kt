package event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import element.ElementFragment
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentEventsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class EventsFragment : Fragment() {

    private val model: EventsModel by viewModel()

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val adapter = EventsAdapter(object : EventsAdapter.Listener {
        override fun onItemClick(item: EventsAdapter.Item) {
            viewLifecycleOwner.lifecycleScope.launch {
                val element = model.selectElementById(item.elementId)

                if (element != null) {
                    withResumed {
                        parentFragmentManager.commit {
                            setReorderingAllowed(true)
                            replace<ElementFragment>(R.id.nav_host_fragment, null, bundleOf("element_id" to element.id))
                            addToBackStack(null)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Element not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onShowMoreClick() {
            model.onShowMoreItemsClick()
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.state.collect {
                    when (it) {
                        is EventsModel.State.ShowingItems -> {
                            val itemCount = adapter.itemCount

                            adapter.submitList(it.items) {
                                if (itemCount != 0) {
                                    adapter.notifyItemChanged(itemCount - 1)
                                }
                            }
                        }

                        else -> {}
                    }
                }

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}