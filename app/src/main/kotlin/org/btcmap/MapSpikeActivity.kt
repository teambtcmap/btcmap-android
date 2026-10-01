package org.btcmap

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.btcmap.ui.MapComposeView
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
            onPlaceSelected = { place ->
                Toast.makeText(this@MapSpikeActivity, "place ${place.id}: ${place.name}", Toast.LENGTH_SHORT).show()
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
        // Warsaw, which has merchants, exchanges and an upcoming event nearby.
        const val CENTER_LAT = 52.2333742
        const val CENTER_LON = 21.0711489
    }
}
