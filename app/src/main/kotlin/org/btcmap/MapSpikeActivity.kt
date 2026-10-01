package org.btcmap

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.btcmap.ui.MapComposeView
import org.btcmap.util.isUpcoming
import java.time.ZonedDateTime

/**
 * Phase 3 spike: hosts the shared MapLibre Compose map ([MapComposeView]) so it
 * can be launched directly (`adb shell am start`). It loads real merchants,
 * exchanges and events from the local database around a busy area to exercise
 * the ported layer pipelines. Temporary scaffolding; remove once the shared map
 * replaces [map.MapFragment].
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
            initialZoom = 13.0
            iconTypeface = org.btcmap.util.iconTypeface
        }
        setContentView(view)

        val app = application as App
        Thread {
            val minLat = CENTER_LAT - HALF_BOX
            val maxLat = CENTER_LAT + HALF_BOX
            val minLon = CENTER_LON - HALF_BOX
            val maxLon = CENTER_LON + HALF_BOX
            val now = ZonedDateTime.now()

            val merchants = app.db.place.selectMerchantsByBounds(minLat, maxLat, minLon, maxLon)
            val exchanges = app.db.place.selectExchangesByBounds(minLat, maxLat, minLon, maxLon)
            val events = app.db.event.selectByBounds(minLat, maxLat, minLon, maxLon)
                .filter { it.startsAt.isUpcoming(now) }

            Log.i(TAG, "loaded ${merchants.size} merchants, ${exchanges.size} exchanges, ${events.size} events")
            runOnUiThread {
                view.markers = merchants
                view.exchanges = exchanges
                view.events = events
            }
        }.start()
    }

    private companion object {
        const val TAG = "MapSpike"

        // Warsaw, which has merchants, exchanges and an upcoming event nearby.
        const val CENTER_LAT = 52.2333742
        const val CENTER_LON = 21.0711489
        const val HALF_BOX = 0.08
    }
}
