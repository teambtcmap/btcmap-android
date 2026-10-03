package org.btcmap.imagestats

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.databinding.ImageStatsFragmentBinding
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

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
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh -> {
                    refresh()
                    true
                }
                else -> false
            }
        }

        binding.statsList.iconTypeface = iconTypeface

        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Re-reads both caches and the load counters and shows the result. Called
     * once when the screen opens and again whenever the refresh action is
     * tapped, since the snapshot otherwise goes stale while the screen stays
     * open.
     */
    private fun refresh() {
        val appContext = requireContext().applicationContext
        val homeDirectory = requireContext().dataDir.path
        val statsList = binding.statsList
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
                statsList.sections = imageStatsSections(cache, ImageLoadStats.snapshot(), labels())
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    /** The Android labels and byte formatting for the shared image stats cards. */
    private fun labels(): ImageStatsLabels = ImageStatsLabels(
        memoryCache = getString(R.string.image_stats_memory_cache),
        diskCache = getString(R.string.image_stats_disk_cache),
        loads = getString(R.string.image_stats_loads),
        entries = getString(R.string.image_stats_entries),
        size = getString(R.string.image_stats_size),
        location = getString(R.string.image_stats_location),
        requests = getString(R.string.image_stats_requests),
        memoryHits = getString(R.string.image_stats_memory_hits),
        diskHits = getString(R.string.image_stats_disk_hits),
        networkLoads = getString(R.string.image_stats_network_loads),
        cacheHitRate = getString(R.string.image_stats_cache_hit_rate),
        errors = getString(R.string.image_stats_errors),
        cancels = getString(R.string.image_stats_cancels),
        averageLoad = getString(R.string.image_stats_average_load),
        percent = { getString(R.string.image_stats_percent, it) },
        millis = { getString(R.string.image_stats_millis, it) },
        sizeOf = { used, max ->
            getString(
                R.string.image_stats_size_of,
                Formatter.formatFileSize(requireContext(), used),
                Formatter.formatFileSize(requireContext(), max),
            )
        },
    )
}
