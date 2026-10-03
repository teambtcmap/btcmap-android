package org.btcmap.dbstats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.bundle.BundledAreas
import org.btcmap.bundle.BundledComments
import org.btcmap.bundle.BundledEvents
import org.btcmap.bundle.BundledPlaces
import org.btcmap.databinding.DbStatsFragmentBinding
import org.btcmap.db
import org.btcmap.db.table.area.TABLE as AREA_TABLE
import org.btcmap.db.table.comment.TABLE as COMMENT_TABLE
import org.btcmap.db.table.event.TABLE as EVENT_TABLE
import org.btcmap.db.table.place.TABLE as PLACE_TABLE
import org.btcmap.settings.prefs
import org.btcmap.sync.SyncState
import org.btcmap.syncController
import org.btcmap.ui.DbStatsPageLabels
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

/**
 * The database stats screen: a Views toolbar with the sync action over the
 * shared [org.btcmap.ui.DbStatsPage], which reads the database, builds the cards
 * with the shared [dbStatsSections] and renders [org.btcmap.ui.StatsScreen].
 * This fragment supplies the labels and the bundled snapshot stats, and keeps
 * the sync action's enabled state in step with the controller.
 */
class DbStatsFragment : Fragment() {

    private var _binding: DbStatsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DbStatsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val sync = syncController()
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sync -> {
                    // Kicks off the app-scoped full sync; the card below follows
                    // it and the item stays disabled until it finishes.
                    sync.start()
                    true
                }
                else -> false
            }
        }

        // Read before apply(): inside it, the name would resolve to the view's
        // own property instead of the icon font built from the assets.
        val typeface = iconTypeface
        binding.statsContent.apply {
            database = db()
            settings = prefs
            labels = dbStatsPageLabels()
            iconTypeface = typeface
            onSync = { sync.start() }
            showSyncButton = false
        }

        // The bundle cards come from immutable assets, so they are read once and
        // handed to the shared page. A snapshot that cannot be read only loses
        // its own card: the database stats still render, and the problem is
        // reported rather than hidden behind a blank screen.
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reads = withContext(Dispatchers.IO) {
                    readBundles(BUNDLES) { fileName -> context.assets.open(fileName) }
                }
                _binding?.statsContent?.bundles = reads.stats
                reads.failures.forEach { showError(it) }
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }

        // The sync card follows the controller and the toolbar action is only
        // enabled while it is idle.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sync.state.collect { state ->
                    _binding?.statsContent?.syncState = state
                    binding.topAppBar.menu.findItem(R.id.sync)?.isEnabled = state == SyncState.Idle
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** The Android labels for the shared database stats cards. */
    private fun dbStatsLabels(): DbStatsLabels = DbStatsLabels(
        database = getString(R.string.db_stats_database),
        file = getString(R.string.db_stats_file_name),
        version = getString(R.string.db_stats_version),
        size = getString(R.string.db_stats_size),
        table = { getString(R.string.db_stats_table, it) },
        bundle = { getString(R.string.db_stats_bundle, it) },
        location = getString(R.string.db_stats_location),
        visibleRows = getString(R.string.db_stats_visible_rows),
        deletedRows = getString(R.string.db_stats_deleted_rows),
        futureRows = getString(R.string.db_stats_future_rows),
        rows = getString(R.string.db_stats_rows),
        newestUpdate = getString(R.string.db_stats_max_updated_at),
    )

    /** The Android labels for the shared sync card. */
    private fun dbStatsPageLabels(): DbStatsPageLabels = DbStatsPageLabels(
        dbStats = dbStatsLabels(),
        sync = getString(R.string.db_stats_sync),
        source = getString(R.string.db_stats_sync_source),
        state = getString(R.string.db_stats_sync_state),
        syncNow = getString(R.string.sync),
        syncStateLabel = { getString(it.labelRes) },
    )

    private val SyncState.labelRes: Int
        get() = when (this) {
            SyncState.Idle -> R.string.db_stats_sync_state_idle
            SyncState.UnbundlingPlaces -> R.string.db_stats_sync_state_unbundling_places
            SyncState.SyncingPlaces -> R.string.db_stats_sync_state_syncing_places
            SyncState.UnbundlingEvents -> R.string.db_stats_sync_state_unbundling_events
            SyncState.SyncingEvents -> R.string.db_stats_sync_state_syncing_events
            SyncState.UnbundlingComments -> R.string.db_stats_sync_state_unbundling_comments
            SyncState.SyncingComments -> R.string.db_stats_sync_state_syncing_comments
            SyncState.UnbundlingAreas -> R.string.db_stats_sync_state_unbundling_areas
            SyncState.SyncingAreas -> R.string.db_stats_sync_state_syncing_areas
        }

    private companion object {
        /**
         * The optional bundled snapshot that seeds each table, keyed by table
         * name. Tables without a snapshot (like `pref`) simply have no card.
         */
        val BUNDLES = mapOf(
            PLACE_TABLE to BundledPlaces.FILE_NAME,
            COMMENT_TABLE to BundledComments.FILE_NAME,
            AREA_TABLE to BundledAreas.FILE_NAME,
            EVENT_TABLE to BundledEvents.FILE_NAME,
        )
    }
}
