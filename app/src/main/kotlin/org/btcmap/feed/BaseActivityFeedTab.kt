package org.btcmap.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
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
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.api.getPlaceOsmId
import org.btcmap.databinding.ActivityFeedFilterDialogBinding
import org.btcmap.databinding.ActivityFeedTabBinding
import org.btcmap.db
import org.btcmap.place.PlaceFragment
import org.btcmap.place.toOsmUrl
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.activityIntervalDays
import org.btcmap.settings.prefs
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation

/** The area and place ids an activity feed tab queries. */
data class ActivityScope(
    val areaIds: List<String>,
    val placeIds: List<String> = emptyList(),
)

/**
 * Common scaffolding for an Activity Feed tab: list of items, filter chips
 * surfaced via [showFilterDialog], and a placeholder for empty/loading states.
 * Subclasses override [loadScope] to yield the ids to query for the current
 * selection (or null to short-circuit with empty).
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
        val adapter = ActivityFeedAdapter { item ->
            if (item.type == ActivityFeedItem.TYPE_PLACE_DELETED) {
                openDeletedPlace(item.placeId)
            } else {
                requireActivity().supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainerView, PlaceFragment.create(item.placeId))
                    addToBackStack(null)
                }
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
        val restoredSelection = savedInstanceState?.getStringArrayList(STATE_SELECTED_IDS)
        if (restoredSelection != null) {
            selectedIds.addAll(restoredSelection)
        } else if (showAreaChips) {
            // Communities are the useful default scope; a country is selected
            // only when the map centre is inside no community, otherwise the
            // tab would be empty in rural areas.
            val communities = initialAreas.filter { it.type != "country" }
            selectedIds.addAll(communities.ifEmpty { initialAreas }.map { it.id })
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
     * Returns the ids that should be queried given the current selection.
     * Returning an empty scope means "nothing selected" and the tab shows its
     * empty state. Return null to skip the network call entirely (e.g. user
     * not logged in for the Saved tab).
     */
    protected open fun loadScope(): ActivityScope? {
        return ActivityScope(areaIds = selectedIds.toList())
    }

    protected fun loadActivity() {
        loadJob?.cancel()
        val adapter = binding.list.adapter as ActivityFeedAdapter

        val scope = loadScope()
        if (scope == null || (scope.areaIds.isEmpty() && scope.placeIds.isEmpty())) {
            showEmptyState(adapter)
            return
        }

        binding.list.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    api().getActivity(
                        scope.areaIds,
                        scope.placeIds,
                        prefs.activityIntervalDays,
                    )
                }
                binding.loading.visibility = View.GONE
                if (items.isEmpty()) {
                    showEmptyState(adapter)
                } else {
                    binding.list.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                    binding.emptyView.setOnClickListener(null)
                    adapter.submitList(items)
                }
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showErrorState(adapter)
            }
        }
    }

    /**
     * A deleted place no longer has a screen to open, so the row opens its
     * OpenStreetMap page instead. The OSM id is taken from the local tombstone
     * when present, and read from the server otherwise: a delete can name a
     * place whose tombstone never reached the device.
     */
    private fun openDeletedPlace(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val osmId = withContext(Dispatchers.IO) {
                db().place.selectByIdIncludingDeleted(placeId)?.osmId
                    ?: fetchOsmId(placeId)
            } ?: return@launch
            val url = osmId.toOsmUrl() ?: return@launch
            openInBrowser(url.toUri())
        }
    }

    private suspend fun fetchOsmId(placeId: Long): String? {
        return try {
            api().getPlaceOsmId(placeId)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            null
        }
    }

    private fun showEmptyState(adapter: ActivityFeedAdapter) {
        adapter.submitList(emptyList())
        binding.loading.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = emptyMessage()
        binding.emptyView.setOnClickListener(null)
        binding.list.visibility = View.GONE
    }

    private fun showErrorState(adapter: ActivityFeedAdapter) {
        adapter.submitList(emptyList())
        binding.loading.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyView.text = getString(
            R.string.failed_to_load_tap_to_retry,
            getString(R.string.failed_to_load),
            getString(R.string.tap_to_retry),
        )
        binding.emptyView.setOnClickListener { loadActivity() }
        binding.list.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_SELECTED_IDS, ArrayList(selectedIds))
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

        private const val STATE_SELECTED_IDS = "selected_ids"
    }
}
