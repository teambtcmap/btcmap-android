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
        val reader = DbStatsReader(database.conn)
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
                adapter.submitList(buildItems(file, version, tables))
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
        file: DatabaseFile?,
        version: Int,
        tables: List<TableStats>,
    ): List<DbStatsItem> {
        val items = mutableListOf<DbStatsItem>()

        items.add(DbStatsItem.Header(getString(R.string.db_stats_database)))
        file?.let {
            items.add(DbStatsItem.Entry(getString(R.string.db_stats_file_name), it.name))
        }
        items.add(DbStatsItem.Entry(getString(R.string.db_stats_version), version.toString()))
        file?.let {
            items.add(
                DbStatsItem.Entry(
                    getString(R.string.db_stats_size),
                    Formatter.formatFileSize(requireContext(), it.sizeBytes),
                )
            )
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
}
