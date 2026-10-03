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
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.api.getPlaceOsmId
import org.btcmap.databinding.ActivityFeedTabBinding
import org.btcmap.db
import org.btcmap.place.PlaceFragment
import org.btcmap.place.toOsmUrl
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.activityIntervalDays
import org.btcmap.settings.prefs
import org.btcmap.ui.ActivityFeedComposeView
import org.btcmap.ui.ChipFilterComposeView
import org.btcmap.ui.ChipOption
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation

/** The area and place ids an activity feed tab queries. */
data class ActivityScope(
    val areaIds: List<String>,
    val placeIds: List<String> = emptyList(),
)

/**
 * Common scaffolding for an Activity Feed tab: the filter chips surfaced via
 * [showFilterDialog], the area selection, and the load handed to the shared
 * [org.btcmap.ui.ActivityFeedPage]. Subclasses override [loadScope] to yield
 * the ids to query for the current selection (or null to short-circuit).
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

        // Read before apply(): inside it, the name would resolve to the view's
        // own property instead of the icon font built from the assets.
        val typeface = iconTypeface
        val errorMessage = getString(
            R.string.failed_to_load_tap_to_retry,
            getString(R.string.failed_to_load),
            getString(R.string.tap_to_retry),
        )
        binding.feedList.apply {
            iconTypeface = typeface
            load = { loadItems() }
            toRow = { item -> item.toRow(requireContext()) }
            emptyMessage = { this@BaseActivityFeedTab.emptyMessage() }
            this.errorMessage = errorMessage
            onItemClick = { item -> openItem(item) }
        }
    }

    abstract fun emptyMessage(): String

    /** Opens the filter dialog with area chips (optional) and interval chips. */
    fun showFilterDialog() {
        val view = ChipFilterComposeView(requireContext()).apply {
            setViewTreeLifecycleOwner(this@BaseActivityFeedTab)
            setViewTreeSavedStateRegistryOwner(this@BaseActivityFeedTab)
            setViewTreeViewModelStoreOwner(this@BaseActivityFeedTab)
            areasLabel = getString(R.string.activity_filter_areas)
            intervalLabel = getString(R.string.activity_interval)
        }
        updateChipView(view)

        view.onAreaToggle = { key ->
            if (selectedIds.contains(key)) selectedIds.remove(key) else selectedIds.add(key)
            updateChipView(view)
            reload()
        }
        view.onIntervalSelect = { key ->
            key.toIntOrNull()?.let { days ->
                if (prefs.activityIntervalDays != days) {
                    prefs.activityIntervalDays = days
                    updateChipView(view)
                    reload()
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun updateChipView(view: ChipFilterComposeView) {
        view.areaOptions = if (showAreaChips) {
            initialAreas.map { area -> ChipOption(area.id, area.name, selectedIds.contains(area.id)) }
        } else {
            emptyList()
        }
        view.intervalOptions = ActivityInterval.entries.map { interval ->
            ChipOption(
                key = interval.days.toString(),
                label = interval.name(requireContext()),
                selected = interval.days == prefs.activityIntervalDays,
            )
        }
    }

    /**
     * Returns the ids that should be queried given the current selection.
     * Returning an empty scope means "nothing selected" and the tab shows its
     * empty state. Return null to skip the network call entirely (e.g. user
     * not logged in for the Saved tab).
     */
    protected open suspend fun loadScope(): ActivityScope? {
        return ActivityScope(areaIds = selectedIds.toList())
    }

    /**
     * Runs the query behind the shared page. [loadScope] reads the local cache
     * for the Saved tab, so it runs inside the coroutine: the shared SQLite
     * connection's lock can stall a main-thread read behind a background sync
     * write.
     */
    private suspend fun loadItems(): List<ActivityFeedItem> {
        val scope = loadScope()
        if (scope == null || (scope.areaIds.isEmpty() && scope.placeIds.isEmpty())) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            api().getActivity(scope.areaIds, scope.placeIds, prefs.activityIntervalDays)
        }
    }

    /** Re-runs the load, e.g. after the filter changed or on resume. */
    protected fun reload() {
        val list = _binding?.feedList ?: return
        list.reloadKey += 1
    }

    private fun openItem(item: ActivityFeedItem) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_SELECTED_IDS, ArrayList(selectedIds))
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
