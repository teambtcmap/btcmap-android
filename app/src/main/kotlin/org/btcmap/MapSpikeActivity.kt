package org.btcmap

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.btcmap.ui.MapComposeView

/**
 * Phase 3 spike: hosts the shared MapLibre Compose map ([MapComposeView]) so it
 * can be launched directly (`adb shell am start`). It loads real merchants from
 * the local database around a dense area to exercise the ported layer pipeline.
 * Temporary scaffolding; remove once the shared map replaces [map.MapFragment].
 */
class MapSpikeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = MapComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MapSpikeActivity)
            setViewTreeSavedStateRegistryOwner(this@MapSpikeActivity)
            setViewTreeViewModelStoreOwner(this@MapSpikeActivity)
            initialLat = CENTER_LAT
            initialLon = CENTER_LON
            initialZoom = 12.0
        }
        setContentView(view)

        val app = application as App
        Thread {
            val markers = app.db.place.selectMerchantsByBounds(
                minLat = CENTER_LAT - HALF_BOX,
                maxLat = CENTER_LAT + HALF_BOX,
                minLon = CENTER_LON - HALF_BOX,
                maxLon = CENTER_LON + HALF_BOX,
            )
            Log.i(TAG, "loaded ${markers.size} merchants")
            runOnUiThread { view.markers = markers }
        }.start()
    }

    private companion object {
        const val TAG = "MapSpike"

        // Johannesburg, the densest cell in the bundled snapshot.
        const val CENTER_LAT = -26.2041
        const val CENTER_LON = 28.0473
        const val HALF_BOX = 0.08
    }
}
