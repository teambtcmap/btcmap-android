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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.ActivityFeedFilterDialogBinding
import org.btcmap.databinding.ActivityFeedTabBinding
import org.btcmap.place.PlaceFragment
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.activityIntervalDays
import org.btcmap.settings.prefs

/**
 * Common scaffolding for an Activity Feed tab: list of items, filter chips
 * surfaced via [showFilterDialog], and a placeholder for empty/loading states.
 * Subclasses override [loadAreaIds] to yield the set of area ids to query for
 * the current selection (or null to short-circuit with empty).
 */
abstract class BaseActivityFeedTab : Fragment() {

    data class Area(
        val id: String,
        val name: String,
        val type: String,
    )

    private var _binding: ActivityFeedTabBinding? = null
    private val binding get() = _binding!!

    private val selectedIds = mutableSetOf<String>()
    private var loadJob: Job? = null
    private var showAreaChips: Boolean = false
    private var initialAreas: List<Area> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityFeedTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ActivityFeedAdapter { _ ->
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<PlaceFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)

        showAreaChips = arguments?.getBoolean(ARG_SHOW_AREA_CHIPS, false) ?: false
        val ids = arguments?.getStringArrayList(ARG_INITIAL_AREA_IDS) ?: arrayListOf()
        val names = arguments?.getStringArrayList(ARG_INITIAL_AREA_NAMES) ?: arrayListOf()
        val types = arguments?.getStringArrayList(ARG_INITIAL_AREA_TYPES) ?: arrayListOf()
        initialAreas = ids.indices.map { i ->
            Area(
                id = ids[i],
                name = names.getOrNull(i) ?: ids[i],
                type = types.getOrNull(i) ?: "",
            )
        }

        selectedIds.clear()
        if (showAreaChips) {
            selectedIds.addAll(initialAreas.filter { it.type != "country" }.map { it.id })
        }

        loadActivity()
    }

    abstract fun emptyMessage(): String

    /** Opens the filter dialog with area chips (optional) and interval chips. */
    fun showFilterDialog() {
        val b = _binding ?: return
        val dialogBinding = ActivityFeedFilterDialogBinding.inflate(layoutInflater)

        if (showAreaChips && initialAreas.isNotEmpty()) {
            for (area in initialAreas) {
                val chip = Chip(requireContext())
                chip.text = area.name
                chip.isCheckable = true
                chip.isChecked = selectedIds.contains(area.id)
                chip.isCloseIconVisible = false
                chip.setOnClickListener {
                    if (chip.isChecked) selectedIds.add(area.id)
                    else selectedIds.remove(area.id)
                    loadActivity()
                }
                dialogBinding.areasChipGroup.addView(chip)
            }
        } else {
            dialogBinding.areasLabel.visibility = View.GONE
            dialogBinding.areasChipGroup.visibility = View.GONE
        }

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
                    loadActivity()
                }
            }
            dialogBinding.intervalChipGroup.addView(chip)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Returns the area ids that should be queried given the current chip
     * selection. Returning an empty list means "no areas selected" and the
     * tab shows its empty state. Return null to skip the network call
     * entirely (e.g. user not logged in for the Following tab).
     */
    protected open fun loadAreaIds(): List<String>? {
        return selectedIds.toList()
    }

    protected fun loadActivity() {
        loadJob?.cancel()
        val adapter = binding.list.adapter as ActivityFeedAdapter

        val ids = loadAreaIds()
        if (ids == null) {
            adapter.submitList(emptyList())
            binding.loading.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = emptyMessage()
            binding.list.visibility = View.GONE
            return
        }

        if (ids.isEmpty()) {
            adapter.submitList(emptyList())
            binding.loading.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = emptyMessage()
            binding.list.visibility = View.GONE
            return
        }

        binding.list.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    api().getActivity(ids, days = prefs.activityIntervalDays)
                }
                binding.loading.visibility = View.GONE
                if (items.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.emptyView.text = emptyMessage()
                    binding.list.visibility = View.GONE
                } else {
                    binding.list.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                }
                adapter.submitList(items)
            } catch (e: Throwable) {
                e.printStackTrace()
                binding.loading.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = emptyMessage()
                binding.list.visibility = View.GONE
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

    companion object {
        const val ARG_SHOW_AREA_CHIPS = "show_area_chips"
        const val ARG_INITIAL_AREA_IDS = "area_ids"
        const val ARG_INITIAL_AREA_NAMES = "area_names"
        const val ARG_INITIAL_AREA_TYPES = "area_types"
    }
}
