package area

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentAreasBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class AreasFragment : Fragment() {

    private val model: AreasModel by viewModel()

    private var _binding: FragmentAreasBinding? = null
    private val binding get() = _binding!!

    private val args = lazy {
        AreasModel.Args(
            lat = requireArguments().getFloat("lat").toDouble(),
            lon = requireArguments().getFloat("lon").toDouble(),
        )
    }

    private val adapter = AreasAdapter {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AreaFragment>(
                R.id.nav_host_fragment, null, bundleOf("area_id" to it.id)
            )
            addToBackStack(null)
        }
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
        Log.d("AreasFragment", "args: ${args.value}")
        model.setArgs(args.value)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
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
}