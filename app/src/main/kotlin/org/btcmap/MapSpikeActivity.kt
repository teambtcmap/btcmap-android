package org.btcmap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.btcmap.ui.MapComposeView

/**
 * Phase 3 spike: hosts the shared MapLibre Compose map ([MapComposeView]) so it
 * can be launched directly (`adb shell am start`). Temporary scaffolding for the
 * map migration; remove once the shared map screen replaces [map.MapFragment].
 */
class MapSpikeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = MapComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MapSpikeActivity)
            setViewTreeSavedStateRegistryOwner(this@MapSpikeActivity)
            setViewTreeViewModelStoreOwner(this@MapSpikeActivity)
        }
        setContentView(view)
    }
}
