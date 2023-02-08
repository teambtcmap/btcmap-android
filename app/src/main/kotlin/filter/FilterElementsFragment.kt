package filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.databinding.FragmentFilterElementsBinding
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FilterElementsFragment : Fragment() {

    private val model: FilterElementsModel by viewModel()

    private val resultModel: FilterResultModel by activityViewModel()

    private var _binding: FragmentFilterElementsBinding? = null
    private val binding get() = _binding!!

    private val adapter = ElementCategoriesAdapter(object : ElementCategoriesAdapter.Listener {
        override fun onItemCheckedChange(item: ElementCategoriesAdapter.Item, checked: Boolean) {
            model.setVisible(item.id, checked)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFilterElementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setInsets()

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            whenResumed {
                model.state.collect { state ->
                    when (state) {
                        is FilterElementsModel.State.Loading -> {
                            binding.progress.isVisible = true
                            binding.list.isVisible = false
                        }

                        is FilterElementsModel.State.Loaded -> {
                            binding.progress.isVisible = false
                            binding.list.isVisible = true
                            adapter.submitList(state.items)
                            resultModel.filteredCategories.update { state.filteredCategories }
                        }
                    }
                }
            }
        }

        model.onViewCreated(resultModel.filteredCategories.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}