package org.btcmap.imagestats

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil3.SingletonImageLoader
import org.btcmap.R
import org.btcmap.databinding.ImageStatsFragmentBinding
import org.btcmap.util.showError

/**
 * The image stats screen: a toolbar over the shared
 * [org.btcmap.ui.ImageStatsPage], which reads Coil's caches and the load
 * counters and renders the cards. This fragment only supplies the loader, the
 * home directory and the labels.
 */
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
                    binding.imageStatsContent.refreshKey++
                    true
                }
                else -> false
            }
        }

        val content = binding.imageStatsContent
        content.imageLoader = SingletonImageLoader.get(requireContext().applicationContext)
        content.homeDirectory = requireContext().dataDir.path
        content.labels = labels()
        content.iconTypeface = org.btcmap.util.iconTypeface
        content.onError = { showError(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
