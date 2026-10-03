package org.btcmap.dbstats

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.sync.SyncState
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
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.syncController
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

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
                    // Kicks off the app-scoped full sync; the card above follows
                    // it and the item stays disabled until it finishes.
                    sync.start()
                    true
                }
                else -> false
            }
        }

        binding.statsList.iconTypeface = iconTypeface

        val database = db()
        val reader = DbStatsReader(database.conn)
        val context = requireContext()
        // The database cards are a live snapshot while the bundle cards come
        // from immutable assets, so the bundles are read once and the whole list
        // is rebuilt whenever the database is re-read. The sync card has to
        // follow the controller, so the base sections are combined for the
        // adapter.
        val baseSections = MutableStateFlow<List<StatsSection>>(emptyList())
        var bundles: Map<String, BundleStats> = emptyMap()

        suspend fun refresh() {
            val file = withContext(Dispatchers.IO) {
                DatabaseFile.read(database.path)
            }
            val version = withContext(Dispatchers.IO) {
                reader.readUserVersion()
            }
            val tables = withContext(Dispatchers.IO) {
                reader.readTables()
            }
            baseSections.value = dbStatsSections(
                file = file,
                version = version,
                tables = tables,
                bundles = bundles,
                labels = dbStatsLabels(),
                formatBytes = { Formatter.formatFileSize(context, it) },
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reads = withContext(Dispatchers.IO) {
                    readBundles(BUNDLES) { fileName -> context.assets.open(fileName) }
                }
                bundles = reads.stats
                // A snapshot that cannot be read only loses its own card: the
                // database stats above still render, and the problem is
                // reported rather than hidden behind a blank screen.
                reads.failures.forEach { showError(it) }
                refresh()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }

            // The database cards are a snapshot too, so re-read them when the
            // app-scoped sync finishes and the underlying rows have changed.
            var wasSyncing = false
            sync.state.collect { state ->
                val syncing = state != SyncState.Idle
                if (wasSyncing && !syncing) {
                    try {
                        refresh()
                    } catch (e: Throwable) {
                        e.rethrowIfCancellation()
                        showError(e)
                    }
                }
                wasSyncing = syncing
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(baseSections, sync.state) { base, state ->
                    buildList {
                        add(syncSection(state))
                        addAll(base)
                    }
                }.collect { binding.statsList.sections = it }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sync.state.collect { state ->
                    binding.topAppBar.menu.findItem(R.id.sync)?.isEnabled =
                        state == SyncState.Idle
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

    private fun syncSection(state: SyncState): StatsSection = StatsSection(
        key = "sync",
        title = getString(R.string.db_stats_sync),
        icon = "sync",
        entries = listOf(
            StatsEntry(
                getString(R.string.db_stats_sync_source),
                prefs.apiUrl.toString(),
            ),
            StatsEntry(
                getString(R.string.db_stats_sync_state),
                getString(state.labelRes),
            ),
        ),
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
