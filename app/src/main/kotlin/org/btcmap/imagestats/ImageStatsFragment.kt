package org.btcmap.imagestats

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.databinding.ImageStatsFragmentBinding
import org.btcmap.stats.StatsAdapter
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError
import java.text.NumberFormat

class ImageStatsFragment : Fragment() {

    private var _binding: ImageStatsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ImageStatsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.statsList.layoutManager = LinearLayoutManager(requireContext())
        val adapter = StatsAdapter()
        binding.statsList.adapter = adapter

        val appContext = requireContext().applicationContext
        val homeDirectory = requireContext().dataDir.path
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Reading the caches can initialize them and touch the disk, so
                // keep it off the main thread.
                val cache = withContext(Dispatchers.IO) {
                    ImageCacheStatsReader(
                        imageLoader = SingletonImageLoader.get(appContext),
                        homeDirectory = homeDirectory,
                    ).read()
                }
                adapter.submitList(buildSections(cache, ImageLoadStats.snapshot()))
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

    private fun buildSections(
        cache: ImageCacheStats,
        counters: ImageLoadCounters,
    ): List<StatsSection> {
        val numberFormatter = NumberFormat.getIntegerInstance()
        val sections = mutableListOf<StatsSection>()

        cache.memory?.let { memory ->
            sections.add(
                StatsSection(
                    title = getString(R.string.image_stats_memory_cache),
                    icon = "memory",
                    entries = listOf(
                        StatsEntry(
                            getString(R.string.image_stats_entries),
                            numberFormatter.format(memory.entryCount),
                        ),
                        StatsEntry(
                            getString(R.string.image_stats_size),
                            sizeOf(memory.usedBytes, memory.maxBytes),
                        ),
                    ),
                )
            )
        }

        cache.disk?.let { disk ->
            sections.add(
                StatsSection(
                    title = getString(R.string.image_stats_disk_cache),
                    icon = "hard_drive",
                    entries = listOf(
                        StatsEntry(getString(R.string.image_stats_location), disk.directory),
                        StatsEntry(
                            getString(R.string.image_stats_size),
                            sizeOf(disk.usedBytes, disk.maxBytes),
                        ),
                    ),
                )
            )
        }

        sections.add(
            StatsSection(
                title = getString(R.string.image_stats_loads),
                icon = "query_stats",
                entries = buildList {
                    add(counter(R.string.image_stats_requests, counters.requests, numberFormatter))
                    add(
                        counter(
                            R.string.image_stats_memory_hits,
                            counters.memoryCacheHits,
                            numberFormatter,
                        )
                    )
                    add(
                        counter(
                            R.string.image_stats_disk_hits,
                            counters.diskCacheHits,
                            numberFormatter,
                        )
                    )
                    add(
                        counter(
                            R.string.image_stats_network_loads,
                            counters.networkLoads,
                            numberFormatter,
                        )
                    )
                    counters.cacheHitRatePercent?.let {
                        add(
                            StatsEntry(
                                getString(R.string.image_stats_cache_hit_rate),
                                getString(R.string.image_stats_percent, it),
                            )
                        )
                    }
                    add(counter(R.string.image_stats_errors, counters.errors, numberFormatter))
                    add(counter(R.string.image_stats_cancels, counters.cancels, numberFormatter))
                    add(
                        StatsEntry(
                            getString(R.string.image_stats_average_load),
                            getString(R.string.image_stats_millis, counters.averageLoadMillis),
                        )
                    )
                },
            )
        )

        return sections
    }

    private fun counter(resId: Int, value: Long, formatter: NumberFormat): StatsEntry {
        return StatsEntry(getString(resId), formatter.format(value))
    }

    private fun sizeOf(usedBytes: Long, maxBytes: Long): String {
        val used = Formatter.formatFileSize(requireContext(), usedBytes)
        val max = Formatter.formatFileSize(requireContext(), maxBytes)
        return getString(R.string.image_stats_size_of, used, max)
    }
}
