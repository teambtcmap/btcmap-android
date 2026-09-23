package org.btcmap.area

import android.content.DialogInterface
import android.text.format.Formatter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.db.table.area.Area
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds
import org.btcmap.offline.OfflineRegionEstimates
import org.btcmap.offlineMaps
import org.btcmap.settings.mapStyle
import org.btcmap.settings.name
import org.btcmap.settings.offlineStyleUrl
import org.btcmap.settings.prefs
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

/**
 * Drives the offline-map section of the area screen: the toolbar download
 * action, the zoom/size dialog and the progress panel.
 *
 * Kept out of [AreaFragment] so the screen only wires the area content. The
 * controller owns the [Area] the toolbar action needs, set by [bind], and
 * collects the download state for as long as the view is resumed.
 */
internal class AreaOfflineMapController(
    private val fragment: Fragment,
    private val binding: AreaFragmentBinding,
) {

    private var area: Area? = null

    fun bind(area: Area) {
        this.area = area
        val bounds = area.offlineBounds()
        binding.toolbar.menu.findItem(R.id.download).isVisible = bounds != null
        if (bounds == null) return

        binding.offlineMapDownload.setOnClickListener { showDialog(area, bounds) }
        binding.offlineMapDelete.setOnClickListener { confirmDelete(area) }

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                fragment.offlineMaps().states.collect { states ->
                    renderState(states[area.id])
                }
            }
        }
    }

    fun onDownloadClicked() {
        val area = area ?: return
        val bounds = area.offlineBounds() ?: return
        showDialog(area, bounds)
    }

    private fun renderState(state: OfflineAreaState?) {
        val context = fragment.context ?: return
        val resolved = state ?: OfflineAreaState.None

        // The toolbar button is the only offline affordance until a download is
        // started or finished; the panel is reserved for progress and result.
        binding.offlineMap.isVisible = resolved !is OfflineAreaState.None
        binding.toolbar.menu.findItem(R.id.download).isEnabled =
            resolved !is OfflineAreaState.Downloading
        if (resolved is OfflineAreaState.None) return

        val downloading = resolved is OfflineAreaState.Downloading
        binding.offlineMapProgress.isVisible = downloading
        binding.offlineMapDownload.isVisible = !downloading
        binding.offlineMapDelete.isVisible = resolved is OfflineAreaState.Complete
        binding.offlineMapDownload.text = fragment.getString(
            if (resolved is OfflineAreaState.Complete) {
                R.string.offline_map_download_again
            } else {
                R.string.offline_map_download
            },
        )

        val status = when (resolved) {
            OfflineAreaState.None -> ""

            is OfflineAreaState.Downloading -> {
                val size = Formatter.formatFileSize(context, resolved.completedBytes)
                val progress = resolved.progress
                if (progress == null) {
                    binding.offlineMapProgress.isIndeterminate = true
                    fragment.getString(R.string.offline_map_status_downloading, size)
                } else {
                    binding.offlineMapProgress.isIndeterminate = false
                    val percent = (progress * 100).toInt().coerceIn(0, 100)
                    binding.offlineMapProgress.progress = percent
                    fragment.getString(
                        R.string.offline_map_status_downloading_progress,
                        percent,
                        size,
                    )
                }
            }

            is OfflineAreaState.Complete -> {
                val size = Formatter.formatFileSize(context, resolved.bytes)
                val downloaded = fragment.getString(
                    R.string.offline_map_status_downloaded,
                    size,
                    OfflineRegionEstimates.MIN_ZOOM,
                    resolved.maxZoom,
                )
                if (resolved.styleUrl == prefs.mapStyle.offlineStyleUrl(context)) {
                    downloaded
                } else {
                    downloaded + "\n" + fragment.getString(R.string.offline_map_style_mismatch)
                }
            }

            is OfflineAreaState.Failed ->
                fragment.getString(R.string.offline_map_status_failed, resolved.message)
        }

        binding.offlineMapStatus.isVisible = status.isNotEmpty()
        binding.offlineMapStatus.text = status
    }

    private fun showDialog(area: Area, bounds: OfflineBounds) {
        val context = fragment.requireContext()
        val view = fragment.layoutInflater.inflate(R.layout.dialog_offline_map, null)
        val description = view.findViewById<TextView>(R.id.description)
        val style = view.findViewById<TextView>(R.id.style)
        val zoom = view.findViewById<TextView>(R.id.zoom)
        val slider = view.findViewById<Slider>(R.id.zoom_slider)
        val estimate = view.findViewById<TextView>(R.id.estimate)

        description.text = fragment.getString(R.string.offline_map_description, area.name)
        style.text = fragment.getString(
            R.string.offline_map_style,
            prefs.mapStyle.name(context),
        )

        val minZoom = OfflineRegionEstimates.MIN_SELECTABLE_MAX_ZOOM
        val maxZoom = OfflineRegionEstimates.maxSelectableZoom(bounds)
        val withinLimit = OfflineRegionEstimates.isWithinLimit(bounds)

        fun update(selectedMaxZoom: Int) {
            zoom.text = fragment.getString(R.string.offline_map_max_zoom, selectedMaxZoom)
            val bytes = OfflineRegionEstimates.estimatedBytes(
                bounds,
                OfflineRegionEstimates.MIN_ZOOM,
                selectedMaxZoom,
            )
            val size = Formatter.formatFileSize(context, bytes)
            estimate.text = if (withinLimit) {
                fragment.getString(R.string.offline_map_estimated_size, size)
            } else {
                fragment.getString(R.string.offline_map_too_large, size)
            }
        }

        val fixedZoom = maxZoom <= minZoom
        if (fixedZoom) {
            slider.isVisible = false
            update(minZoom)
        } else {
            slider.valueFrom = minZoom.toFloat()
            slider.valueTo = maxZoom.toFloat()
            slider.stepSize = 1f
            slider.value = OfflineRegionEstimates.defaultMaxZoom(bounds)
                .coerceIn(minZoom, maxZoom)
                .toFloat()
            slider.addOnChangeListener { _, value, _ -> update(value.toInt()) }
            update(slider.value.toInt())
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.offline_map)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.offline_map_download) { _, _ ->
                val selectedMaxZoom = if (fixedZoom) minZoom else slider.value.toInt()
                startDownload(area, bounds, selectedMaxZoom)
            }
            .create()
        dialog.show()

        if (!withinLimit) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        }
    }

    private fun startDownload(area: Area, bounds: OfflineBounds, maxZoom: Int) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                fragment.offlineMaps().download(
                    areaId = area.id,
                    areaName = area.name,
                    bounds = bounds,
                    styleUrl = prefs.mapStyle.offlineStyleUrl(fragment.requireContext()),
                    maxZoom = maxZoom,
                )
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                fragment.showError(e)
            }
        }
    }

    private fun confirmDelete(area: Area) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.offline_map_delete_title)
            .setMessage(R.string.offline_map_delete_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        fragment.offlineMaps().delete(area.id)
                    } catch (e: Throwable) {
                        e.rethrowIfCancellation()
                        fragment.showError(e)
                    }
                }
            }
            .show()
    }
}

/**
 * The area's bounding box as [OfflineBounds], or null when any side is missing:
 * an area without a bbox has no offline region to download.
 */
private fun Area.offlineBounds(): OfflineBounds? {
    val west = bboxWest ?: return null
    val south = bboxSouth ?: return null
    val east = bboxEast ?: return null
    val north = bboxNorth ?: return null
    return OfflineBounds(west = west, south = south, east = east, north = north)
}
