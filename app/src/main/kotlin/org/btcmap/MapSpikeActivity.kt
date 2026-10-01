package org.btcmap

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds
import org.btcmap.ui.map.OfflinePacks
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.btcmap.ui.MapComposeView
import org.btcmap.ui.PlaceSheetStrings
import java.text.NumberFormat

/**
 * Phase 3 spike: hosts the shared MapLibre Compose map ([MapComposeView]) so it
 * can be launched directly (`adb shell am start`). The map loads the merchants,
 * exchanges and events in its viewport from the local database itself.
 * Temporary scaffolding; remove once the shared map replaces [map.MapFragment].
 */
class MapSpikeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App
        val view = MapComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MapSpikeActivity)
            setViewTreeSavedStateRegistryOwner(this@MapSpikeActivity)
            setViewTreeViewModelStoreOwner(this@MapSpikeActivity)
            database = app.db
            initialLat = CENTER_LAT
            initialLon = CENTER_LON
            initialZoom = 13.0
            iconTypeface = org.btcmap.util.iconTypeface
            placeSheetStrings = PlaceSheetStrings(
                directions = getString(R.string.directions),
                share = getString(R.string.share),
                viewOnBtcmap = getString(R.string.view_on_btcmap),
                viewOnOsm = getString(R.string.view_on_osm),
                editOnOsm = getString(R.string.edit_on_osm),
                notVerified = getString(R.string.not_verified),
                verificationWarningTitle = getString(R.string.verification_warning_title),
                verificationWarningOutdated = getString(R.string.verification_warning_outdated),
                verificationWarningNotVerified = getString(R.string.verification_warning_not_verified),
                ok = getString(android.R.string.ok),
                companionWarning = { getString(R.string.companion_warning, it) },
                verify = getString(R.string.btn_verify),
                report = getString(R.string.btn_report),
                boost = getString(R.string.boost),
                comments = { count ->
                    if (count == 0L) {
                        getString(R.string.comments)
                    } else {
                        getString(R.string.comments_d, count.toInt())
                    }
                },
                commentsTitle = { count -> getString(R.string.comments_d, count.toInt()) },
                addComment = getString(R.string.add_comment),
            )
            onPlaceAction = { place, action ->
                Toast.makeText(this@MapSpikeActivity, "place ${place.id}: $action", Toast.LENGTH_SHORT).show()
            }
            onEventSelected = { event ->
                Toast.makeText(this@MapSpikeActivity, "event ${event.id}: ${event.name}", Toast.LENGTH_SHORT).show()
            }
            onAreaSelected = { areaId ->
                Toast.makeText(this@MapSpikeActivity, "area $areaId", Toast.LENGTH_SHORT).show()
            }
            formatDistance = ::formatDistance
        }
        setContentView(view)

        // Phase 4 spike: drive a small offline pack so the port can be watched.
        val offlinePacks = OfflinePacks(resources.displayMetrics.density)
        lifecycleScope.launch {
            offlinePacks.states.collect { states -> Log.i(TAG, "offline states: $states") }
        }
        lifecycleScope.launch {
            delay(4_000)
            if (offlinePacks.states.value[SPIKE_AREA_ID] is OfflineAreaState.Complete) return@launch
            offlinePacks.download(
                areaId = SPIKE_AREA_ID,
                areaName = "Poland",
                bounds = OfflineBounds(west = 21.00, south = 52.20, east = 21.10, north = 52.30),
                styleUrl = view.styleUrl,
                maxZoom = 12,
            )
        }
    }

    /** Mirrors `SearchController`'s distance formatting. */
    private fun formatDistance(meters: Double): String {
        val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
        return if (meters < 1_000) {
            getString(R.string.s_m, format.format(meters))
        } else {
            getString(R.string.s_km, format.format(meters / 1_000))
        }
    }

    private companion object {
        const val TAG = "MapSpike"
        const val SPIKE_AREA_ID = 530L

        // Warsaw, which has merchants, exchanges and an upcoming event nearby.
        const val CENTER_LAT = 52.2333742
        const val CENTER_LON = 21.0711489
    }
}
