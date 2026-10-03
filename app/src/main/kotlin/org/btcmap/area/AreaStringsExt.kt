package org.btcmap.area

import android.content.Context
import android.text.format.Formatter
import org.btcmap.R
import org.btcmap.ui.AreaStrings

/**
 * The Android strings for the shared [org.btcmap.ui.AreaScreen], so the area
 * fragment resolves them in one place.
 */
fun Context.areaStrings(): AreaStrings = AreaStrings(
    readMore = getString(R.string.read_more),
    collapse = getString(R.string.collapse),
    boostedMerchants = getString(R.string.boosted_merchants),
    events = getString(R.string.events),
    howToHelp = getString(R.string.how_to_help),
    offlineMap = getString(R.string.offline_map),
    offlineDownload = getString(R.string.offline_map_download),
    offlineDownloadAgain = getString(R.string.offline_map_download_again),
    offlineDelete = getString(R.string.delete),
    cancel = getString(android.R.string.cancel),
    boosted = getString(R.string.boosted),
    boostedUntil = { date -> getString(R.string.boosted_until_s, date) },
    issues = { shown, total ->
        if (shown < total) {
            getString(R.string.issues_d_of_d, shown.toInt(), total.toInt())
        } else {
            getString(R.string.issues_d, total.toInt())
        }
    },
    issueDescription = { code ->
        val description = describeIssue(code)
        if (description.formatArg != null) {
            getString(description.resId, description.formatArg)
        } else {
            getString(description.resId)
        }
    },
    offlineStatusDownloading = { size -> getString(R.string.offline_map_status_downloading, size) },
    offlineStatusProgress = { percent, size ->
        getString(R.string.offline_map_status_downloading_progress, percent, size)
    },
    offlineStatusDownloaded = { size, minZoom, maxZoom ->
        getString(R.string.offline_map_status_downloaded, size, minZoom, maxZoom)
    },
    offlineStyleMismatch = getString(R.string.offline_map_style_mismatch),
    offlineStatusFailed = { message -> getString(R.string.offline_map_status_failed, message) },
    offlineDialogDescription = { areaName ->
        getString(R.string.offline_map_description, areaName)
    },
    offlineDialogStyle = { styleName -> getString(R.string.offline_map_style, styleName) },
    offlineDialogMaxZoom = { zoom -> getString(R.string.offline_map_max_zoom, zoom) },
    offlineDialogEstimatedSize = { size -> getString(R.string.offline_map_estimated_size, size) },
    offlineDialogTooLarge = { size -> getString(R.string.offline_map_too_large, size) },
    offlineDialogEstimateNote = getString(R.string.offline_map_estimate_note),
    formatBytes = { bytes -> Formatter.formatFileSize(this, bytes) },
)
