package org.btcmap.map

import android.view.View
import android.widget.FrameLayout
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.btcmap.place.PlaceFragment

class BottomSheetController(
    view: FrameLayout,
    viewLifecycleOwner: LifecycleOwner,
    val placeFragment: PlaceFragment
) {
    val bottomSheetBehavior = BottomSheetBehavior.from(view)
    private val bottomSheetBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            bottomSheetBehavior.startBackProgress(backEvent)
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            bottomSheetBehavior.updateBackProgress(backEvent)
        }

        override fun handleOnBackPressed() {
            bottomSheetBehavior.handleBackInvoked()
        }

        override fun handleOnBackCancelled() {
            bottomSheetBehavior.cancelBackProgress()
        }
    }

    init {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.halfExpandedRatio = 0.33f
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomSheetBackCallback.isEnabled =
                        true

                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN -> bottomSheetBackCallback.isEnabled =
                        false

                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                placeFragment.onSlide(slideOffset)
            }
        })
        placeFragment.requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, bottomSheetBackCallback
        )
    }

    fun halfExpand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    fun hide() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
}