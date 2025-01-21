package area

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import element.ElementFragment
import issue.IssuesFragment
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import map.MapFragment
import okhttp3.HttpUrl
import org.btcmap.R
import org.btcmap.databinding.FragmentAreaBinding
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import reports.ReportsFragment

class AreaFragment : Fragment() {

    private val args = lazy {
        AreaModel.Args(requireArguments().getLong("area_id"))
    }

    private val model: AreaModel by viewModel()

    private val resultModel: AreaResultModel by activityViewModel()

    private var _binding: FragmentAreaBinding? = null
    private val binding get() = _binding!!

    private val adapter = AreaAdapter(
        listener = object : AreaAdapter.Listener {
            override fun onMapClick() {
                viewLifecycleOwner.lifecycleScope.launch {
                    val area = model.selectArea(args.value.areaId) ?: return@launch
                    resultModel.area.update { area }
                    parentFragmentManager.commit {
                        replace<MapFragment>(R.id.nav_host_fragment)
                    }
                }
            }

            override fun onUrlClick(url: HttpUrl) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url.toString())
                startActivity(intent)
            }

            override fun onIssuesClick() {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<IssuesFragment>(
                        R.id.nav_host_fragment, null, bundleOf("area_id" to args.value.areaId)
                    )
                    addToBackStack(null)
                }
            }

            override fun onElementClick(item: AreaAdapter.Item.Element) {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<ElementFragment>(
                        R.id.nav_host_fragment, null, bundleOf("element_id" to item.id)
                    )
                    addToBackStack(null)
                }
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
        binding.topAppBar.apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_reports) {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<ReportsFragment>(
                            R.id.nav_host_fragment, null, bundleOf("area_id" to args.value.areaId)
                        )
                        addToBackStack(null)
                    }
                }

                true
            }
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        model.setArgs(args.value)

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

                            binding.topAppBar.title = state.area.tags.name()
                            binding.topAppBar.subtitle = resources.getQuantityString(
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
}