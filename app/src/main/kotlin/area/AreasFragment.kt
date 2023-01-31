package area

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentAreasBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class AreasFragment : Fragment() {

    private val model: AreasModel by viewModel()

    private var _binding: FragmentAreasBinding? = null
    private val binding get() = _binding!!

    private val adapter = AreasAdapter {
        findNavController().navigate(
            resId = R.id.areaFragment,
            args = bundleOf("area_id" to it.id),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAreasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.setArgs(requireArgs())

        setInsets()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.state.collect { state ->
                    when (state) {
                        is AreasModel.State.Loading -> {
                            binding.progress.isVisible = true
                            binding.list.isVisible = false
                        }
                        is AreasModel.State.Loaded -> {
                            binding.progress.isVisible = false
                            binding.list.isVisible = true
                            adapter.submitList(state.items)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requireArgs(): AreasModel.Args {
        return AreasModel.Args(
            lat = requireArguments().getFloat("lat").toDouble(),
            lon = requireArguments().getFloat("lon").toDouble(),
        )
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