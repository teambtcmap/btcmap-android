package area

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import org.btcmap.R
import org.btcmap.databinding.FragmentAreaBinding
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AreaFragment : Fragment() {

    private val model: AreaModel by viewModel()

    private val resultModel: AreaResultModel by activityViewModel()

    private var _binding: FragmentAreaBinding? = null
    private val binding get() = _binding!!

    private val adapter = AreaAdapter(
        listener = object : AreaAdapter.Listener {
            override fun onMapClick() {
                viewLifecycleOwner.lifecycleScope.launch {
                    val area = model.selectArea(requireArgs().areaId) ?: return@launch
                    resultModel.area.update { area }
                    findNavController().navigate(R.id.action_areaFragment_to_mapFragment)
                }
            }

            override fun onUrlClick(url: HttpUrl) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url.toString())
                startActivity(intent)
            }

            override fun onIssuesClick() {
                findNavController().navigate(
                    R.id.action_areaFragment_to_issuesFragment,
                    bundleOf("area_id" to requireArgs().areaId),
                )
            }

            override fun onElementClick(item: AreaAdapter.Item.Element) {
                findNavController().navigate(
                    R.id.elementFragment,
                    bundleOf("element_id" to item.id),
                )
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAreaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setInsets()

        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_reports) {
                    findNavController().navigate(
                        R.id.reportsFragment,
                        bundleOf("area_id" to requireArgs().areaId),
                    )
                }

                true
            }
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        model.setArgs(requireArgs())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                model.state.collect { state ->
                    when (state) {
                        is AreaModel.State.Loading -> {
                            binding.progress.isVisible = true
                            binding.list.isVisible = false
                        }

                        is AreaModel.State.Loaded -> {
                            val elements =
                                state.items.filterIsInstance<AreaAdapter.Item.Element>().size

                            binding.toolbar.title = state.area.tags.name()
                            binding.toolbar.subtitle = resources.getQuantityString(
                                R.plurals.d_places,
                                elements,
                                elements,
                            )

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

    private fun setInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { appBar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun requireArgs(): AreaModel.Args {
        return AreaModel.Args(requireArguments().getLong("area_id"))
    }
}