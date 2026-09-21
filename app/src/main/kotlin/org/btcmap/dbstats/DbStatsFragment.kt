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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.SyncState
import org.btcmap.databinding.DbStatsFragmentBinding
import org.btcmap.db
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.btcmap.stats.StatsAdapter
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.syncController
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError
import java.text.NumberFormat

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

        binding.statsList.layoutManager = LinearLayoutManager(requireContext())
        val adapter = StatsAdapter()
        binding.statsList.adapter = adapter

        val database = db()
        val reader = DbStatsReader(database.conn)
        // The database stats are read once, but the sync card has to follow the
        // controller, so the two are combined for the adapter.
        val baseSections = MutableStateFlow<List<StatsSection>>(emptyList())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    DatabaseFile.read(database.path)
                }
                val version = withContext(Dispatchers.IO) {
                    reader.readUserVersion()
                }
                val tables = withContext(Dispatchers.IO) {
                    reader.readTables()
                }
                baseSections.value = buildSections(file, version, tables)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(baseSections, sync.state) { base, state ->
                    buildList {
                        add(syncSection(state))
                        addAll(base)
                    }
                }.collect { adapter.submitList(it) }
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

    private fun buildSections(
        file: DatabaseFile?,
        version: Int,
        tables: List<TableStats>,
    ): List<StatsSection> {
        val sections = mutableListOf<StatsSection>()

        sections.add(
            StatsSection(
                title = getString(R.string.db_stats_database),
                icon = "database",
                entries = buildList {
                    file?.let {
                        add(StatsEntry(getString(R.string.db_stats_file_name), it.name))
                    }
                    add(StatsEntry(getString(R.string.db_stats_version), version.toString()))
                    file?.let {
                        add(
                            StatsEntry(
                                getString(R.string.db_stats_size),
                                Formatter.formatFileSize(requireContext(), it.sizeBytes),
                            )
                        )
                    }
                },
            )
        )

        tables.forEach { table ->
            sections.add(
                StatsSection(
                    title = table.name,
                    icon = "table",
                    entries = tableEntries(table),
                )
            )
        }

        return sections
    }

    private fun syncSection(state: SyncState): StatsSection = StatsSection(
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

    private fun tableEntries(table: TableStats): List<StatsEntry> {
        val formatter = NumberFormat.getIntegerInstance()
        return buildList {
            add(StatsEntry(getString(R.string.db_stats_rows), formatter.format(table.rowCount)))
            table.visibleRowCount?.let {
                add(StatsEntry(getString(R.string.db_stats_visible_rows), formatter.format(it)))
            }
            table.deletedRowCount?.let {
                add(StatsEntry(getString(R.string.db_stats_deleted_rows), formatter.format(it)))
            }
            table.futureRowCount?.let {
                add(StatsEntry(getString(R.string.db_stats_future_rows), formatter.format(it)))
            }
            table.maxUpdatedAt?.let {
                add(StatsEntry(getString(R.string.db_stats_max_updated_at), it))
            }
        }
    }
}
