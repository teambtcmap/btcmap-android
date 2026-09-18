package org.btcmap.dbstats

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.databinding.DbStatsFragmentBinding
import org.btcmap.db
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError
import java.io.File
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

        binding.statsList.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DbStatsAdapter()
        binding.statsList.adapter = adapter

        val database = db()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val databases = withContext(Dispatchers.IO) {
                    readDatabaseFiles(database.path)
                }
                val tables = withContext(Dispatchers.IO) {
                    DbStatsReader(database.conn).readTables()
                }
                adapter.submitList(buildItems(databases, tables))
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildItems(
        databases: List<DatabaseFile>,
        tables: List<TableStats>,
    ): List<DbStatsItem> {
        val items = mutableListOf<DbStatsItem>()

        items.add(DbStatsItem.Header(getString(R.string.db_stats_databases)))
        if (databases.isEmpty()) {
            items.add(DbStatsItem.Entry(getString(R.string.db_stats_no_databases), ""))
        } else {
            databases.forEach { database ->
                val detail = buildString {
                    append(Formatter.formatFileSize(requireContext(), database.sizeBytes))
                    if (database.isCurrent) {
                        append(" · ")
                        append(getString(R.string.db_stats_current))
                    }
                }
                items.add(DbStatsItem.Entry(database.name, detail))
            }
        }

        items.add(DbStatsItem.Header(getString(R.string.db_stats_tables)))
        tables.forEach { table ->
            items.add(DbStatsItem.Entry(table.name, tableDetail(table)))
        }

        return items
    }

    private fun tableDetail(table: TableStats): String {
        val formatter = NumberFormat.getIntegerInstance()
        val lines = mutableListOf(
            getString(R.string.db_stats_rows, formatter.format(table.rowCount)),
        )
        table.visibleRowCount?.let {
            lines.add(getString(R.string.db_stats_visible_rows, formatter.format(it)))
        }
        table.deletedRowCount?.let {
            lines.add(getString(R.string.db_stats_deleted_rows, formatter.format(it)))
        }
        table.futureRowCount?.let {
            lines.add(getString(R.string.db_stats_future_rows, formatter.format(it)))
        }
        table.maxUpdatedAt?.let {
            lines.add(getString(R.string.db_stats_max_updated_at, it))
        }
        return lines.joinToString("\n")
    }

    private fun readDatabaseFiles(currentPath: String): List<DatabaseFile> {
        val current = File(currentPath)
        val dir = current.parentFile ?: return emptyList()
        return dir.listFiles()
            .orEmpty()
            .filter { it.isFile && SIDECAR_SUFFIXES.none { suffix -> it.name.endsWith(suffix) } }
            .sortedBy { it.name }
            .map { DatabaseFile(it.name, it.length(), it.name == current.name) }
    }

    private companion object {
        val SIDECAR_SUFFIXES = listOf("-journal", "-wal", "-shm")
    }
}
