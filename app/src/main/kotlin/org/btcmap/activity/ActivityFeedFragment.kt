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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.ActivityFeedFragmentBinding
import org.btcmap.place.PlaceFragment
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.activityIntervalDays
import org.btcmap.settings.prefs
import androidx.core.view.isVisible

class ActivityFeedFragment : Fragment() {

    private data class Area(
        val id: String,
        val name: String,
        val type: String,
    )

    private var _binding: ActivityFeedFragmentBinding? = null
    private val binding get() = _binding!!

    private val areas by lazy {
        val ids = requireArguments().getStringArrayList("area_ids") ?: arrayListOf()
        val names = requireArguments().getStringArrayList("area_names") ?: arrayListOf()
        val types = requireArguments().getStringArrayList("area_types") ?: arrayListOf()
        ids.indices.map { i ->
            Area(
                id = ids[i],
                name = names.getOrNull(i) ?: ids[i],
                type = types.getOrNull(i) ?: "",
            )
        }
    }

    private val selectedIds = mutableSetOf<String>()
    private var loadJob: Job? = null

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

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter -> {
                    binding.filterPanel.visibility =
                        if (binding.filterPanel.isVisible) View.GONE else View.VISIBLE
                    true
                }
                else -> false
            }
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ActivityFeedAdapter { _ ->
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<PlaceFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)

        selectedIds.clear()
        selectedIds.addAll(areas.filter { it.type != "country" }.map { it.id })

        initAreasChips()
        initIntervalChips()

        if (areas.isEmpty()) {
            binding.areasScroll.visibility = View.GONE
        }

        loadActivity()
    }

    private fun initAreasChips() {
        binding.areasChipGroup.removeAllViews()
        for (area in areas) {
            val chip = Chip(requireContext())
            chip.text = area.name
            chip.isCheckable = true
            chip.isChecked = selectedIds.contains(area.id)
            chip.isCloseIconVisible = false
            chip.setOnClickListener { onChipToggled(area, chip.isChecked) }
            binding.areasChipGroup.addView(chip)
        }
    }

    private fun initIntervalChips() {
        binding.intervalChipGroup.removeAllViews()
        val currentDays = prefs.activityIntervalDays
        for (interval in ActivityInterval.entries) {
            val chip = Chip(requireContext())
            chip.text = interval.name(requireContext())
            chip.isCheckable = true
            chip.isChecked = interval.days == currentDays
            chip.isCloseIconVisible = false
            chip.setOnClickListener {
                if (chip.isChecked && prefs.activityIntervalDays != interval.days) {
                    prefs.activityIntervalDays = interval.days
                    uncheckOtherIntervalChips(chip)
                    loadActivity()
                }
            }
            binding.intervalChipGroup.addView(chip)
        }
    }

    private fun uncheckOtherIntervalChips(selectedChip: Chip) {
        for (i in 0 until binding.intervalChipGroup.childCount) {
            val other = binding.intervalChipGroup.getChildAt(i) as? Chip ?: continue
            if (other != selectedChip) {
                other.isChecked = false
            }
        }
    }

    private fun onChipToggled(area: Area, isChecked: Boolean) {
        if (isChecked) {
            selectedIds.add(area.id)
        } else {
            selectedIds.remove(area.id)
        }
        loadActivity()
    }

    private fun loadActivity() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val adapter = binding.list.adapter as ActivityFeedAdapter
            if (selectedIds.isEmpty()) {
                adapter.submitList(emptyList())
                return@launch
            }
            try {
                val activityItems = withContext(Dispatchers.IO) {
                    api().getActivity(selectedIds.toList(), days = prefs.activityIntervalDays)
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
        loadJob?.cancel()
        loadJob = null
        _binding = null
    }
}