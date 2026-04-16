package org.btcmap.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.ActivityFeedFragmentBinding
import org.btcmap.place.PlaceFragment

class ActivityFeedFragment : Fragment() {

    private data class Args(
        val areaIds: List<String>,
    )

    private var _binding: ActivityFeedFragmentBinding? = null
    private val binding get() = _binding!!

    private val args by lazy {
        Args(requireArguments().getStringArrayList("area_ids")!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityFeedFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ActivityFeedAdapter { item ->
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<PlaceFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val activityItems = withContext(Dispatchers.IO) {
                    api().getActivity(args.areaIds)
                }
                adapter.submitList(activityItems)
            } catch (e: Throwable) {
                e.printStackTrace()
                adapter.submitList(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}