package org.btcmap.map

import android.content.res.Configuration
import android.util.Log
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.btcmap.settings.MapStyle
import org.btcmap.settings.mapStyle
import org.btcmap.settings.prefs

class MapStatusBarController(
    private val conf: Configuration,
    private val insetsController: WindowInsetsControllerCompat,
    private val bottomSheetBehavior: BottomSheetBehavior<*>,
) {
    val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            syncStatusBar(bottomSheetBehavior.state)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            syncStatusBar(bottomSheetBehavior.state)
        }
    }

    fun onViewCreated() {
        syncStatusBar(BottomSheetBehavior.STATE_COLLAPSED)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
    }

    fun onDestroyView() {
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
        syncStatusBar(BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun syncStatusBar(bottomSheetState: Int) {
        Log.d("map_status_bar", "syncing")
        val nightMode =
            conf.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            insetsController.isAppearanceLightStatusBars = !nightMode
        } else {
            when (prefs.mapStyle) {
                MapStyle.Auto -> {
                    insetsController.isAppearanceLightStatusBars = !nightMode
                }

                MapStyle.Dark,
                MapStyle.CartoDarkMatter -> insetsController.isAppearanceLightStatusBars = false

                else -> insetsController.isAppearanceLightStatusBars = true
            }
        }
    }
}